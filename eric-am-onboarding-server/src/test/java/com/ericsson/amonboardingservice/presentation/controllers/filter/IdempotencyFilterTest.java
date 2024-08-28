/*
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 */
package com.ericsson.amonboardingservice.presentation.controllers.filter;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.model.CreateVnfPkgInfoRequest;
import com.ericsson.amonboardingservice.presentation.controllers.VnfPackageControllerImpl;
import com.ericsson.amonboardingservice.presentation.models.ProcessingState;
import com.ericsson.amonboardingservice.presentation.models.entity.RequestProcessingDetails;
import com.ericsson.amonboardingservice.presentation.repositories.RequestProcessingDetailsRepository;
import com.ericsson.amonboardingservice.presentation.services.idempotency.IdempotencyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.ericsson.amonboardingservice.utils.Constants.IDEMPOTENCY_KEY_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@Slf4j
@AutoConfigureMockMvc
@SpringBootTest
public class IdempotencyFilterTest extends AbstractDbSetupTest {

    private static final String VNF_PACKAGE_URI = "/api/vnfpkgm/v1/vnf_packages";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RequestProcessingDetailsRepository requestProcessingDetailsRepository;

    @MockBean
    private VnfPackageControllerImpl vnfPackageControllerImpl;

    @SpyBean
    private IdempotencyService idempotencyService;

    @AfterEach
    public void cleanup() {
        requestProcessingDetailsRepository.deleteAll();
    }

    @Test
    public void testIdempotencyFilterForNewRequest() throws Exception {
        String body = objectMapper.writeValueAsString(new CreateVnfPkgInfoRequest());
        String idempotencyKey = UUID.randomUUID().toString();
        String hash = calculateRequestHash(VNF_PACKAGE_URI, "POST", body);

        makePostRequest(VNF_PACKAGE_URI, body, idempotencyKey);

        Optional<RequestProcessingDetails> detailsOptional = requestProcessingDetailsRepository.findById(idempotencyKey);

        verify(vnfPackageControllerImpl, times(1))
                .vnfPackagesPost(any(), any(), any(), any());
        assertThat(detailsOptional.isPresent()).isTrue();
        RequestProcessingDetails details = detailsOptional.get();

        assertThat(details.getRequestHash()).isEqualTo(hash);
        assertThat(details.getId()).isEqualTo(idempotencyKey);
        assertThat(details.getProcessingState()).isEqualTo(ProcessingState.STARTED);
        assertThat(details.getRetryAfter()).isEqualTo(5);
        assertThat(details.getCreationTime()).isNotNull();
    }

    @Test
    public void testIdempotencyFilterForExistedRequest() throws Exception {
        String body = objectMapper.writeValueAsString(new CreateVnfPkgInfoRequest());
        String idempotencyKey = UUID.randomUUID().toString();

        makePostRequest(VNF_PACKAGE_URI, body, idempotencyKey);

        verify(vnfPackageControllerImpl, times(1))
                .vnfPackagesPost(any(), any(), any(), any());

        MvcResult result = makePostRequest(VNF_PACKAGE_URI, body, idempotencyKey);
        HttpServletResponse response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_EARLY.value());
    }

    @Test
    public void testFinishedIdempotentRequest() throws Exception {
        String body = objectMapper.writeValueAsString(new CreateVnfPkgInfoRequest());
        String idempotencyKey = UUID.randomUUID().toString();
        String hash = calculateRequestHash(VNF_PACKAGE_URI, "POST", body);

        RequestProcessingDetails requestProcessingDetails = createDummyRequest(hash, ProcessingState.FINISHED,
                LocalDateTime.now(ZoneOffset.UTC), idempotencyKey);

        MvcResult result = makePostRequest(VNF_PACKAGE_URI, body, idempotencyKey);
        HttpServletResponse response = result.getResponse();

        verify(vnfPackageControllerImpl, never())
                .vnfPackagesPost(any(), any(), any(), any());
        assertThat(response.getStatus()).isEqualTo(requestProcessingDetails.getResponseCode());
        assertThat(response.getHeader("header")).isEqualTo("dummy");
    }

    @Test
    public void testExpiredIdempotentRequest() throws Exception {
        String body = objectMapper.writeValueAsString(new CreateVnfPkgInfoRequest());
        String idempotencyKey = UUID.randomUUID().toString();
        String hash = calculateRequestHash(VNF_PACKAGE_URI, "POST", body);

        createDummyRequest(hash, ProcessingState.STARTED, LocalDateTime.now(ZoneOffset.UTC).minusSeconds(11), idempotencyKey);

        makePostRequest(VNF_PACKAGE_URI, body, idempotencyKey);
        Optional<RequestProcessingDetails> detailsOptional = requestProcessingDetailsRepository.findById(idempotencyKey);

        verify(vnfPackageControllerImpl, times(1))
                .vnfPackagesPost(any(), any(), any(), any());
        verify(idempotencyService, times(1))
                .updateProcessingRequestCreationTime(any());
        assertThat(detailsOptional.isPresent()).isTrue();
        RequestProcessingDetails details = detailsOptional.get();

        assertThat(details.getId()).isEqualTo(idempotencyKey);
        assertThat(details.getRequestHash()).isEqualTo(hash);
        assertThat(details.getProcessingState()).isEqualTo(ProcessingState.STARTED);
        assertThat(details.getRetryAfter()).isEqualTo(5);

        long periodBetween = ChronoUnit.SECONDS.between(LocalDateTime.now(ZoneOffset.UTC), details.getCreationTime());
        assertThat(periodBetween < 5).isTrue();
    }

    @Test
    public void testIdempotentRequestWithWrongHash() throws Exception {
        String body = objectMapper.writeValueAsString(new CreateVnfPkgInfoRequest());
        String idempotencyKey = UUID.randomUUID().toString();

        createDummyRequest("wrongHashValue", ProcessingState.STARTED, LocalDateTime.now(ZoneOffset.UTC), idempotencyKey);

        MvcResult result = makePostRequest(VNF_PACKAGE_URI, body, idempotencyKey);
        HttpServletResponse response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        verify(vnfPackageControllerImpl, times(0)).vnfPackagesPost(any(), any(), any(), any());
    }

    @Test
    public void testIdempotentRequestWithoutIdempotencyKey() throws Exception {
        final var requestBuilder = post(VNF_PACKAGE_URI)
                .content(objectMapper.writeValueAsString(new CreateVnfPkgInfoRequest()))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(requestBuilder)
                .andReturn();

        verify(vnfPackageControllerImpl, times(1)).vnfPackagesPost(any(), any(), any(), any());
    }

    private RequestProcessingDetails createDummyRequest(String hash, ProcessingState state, LocalDateTime creationTime, String uuid) throws Exception {
        RequestProcessingDetails requestProcessingDetails = new RequestProcessingDetails();
        requestProcessingDetails.setId(uuid);
        requestProcessingDetails.setRequestHash(hash);
        requestProcessingDetails.setProcessingState(state);
        requestProcessingDetails.setCreationTime(creationTime);
        requestProcessingDetails.setRetryAfter(5);
        requestProcessingDetails.setResponseBody(objectMapper.writeValueAsString("{\"name\": \"test\"}"));
        requestProcessingDetails.setResponseHeaders(objectMapper.writeValueAsString(Map.of("header", List.of("dummy"))));
        requestProcessingDetails.setResponseCode(201);

        requestProcessingDetailsRepository.save(requestProcessingDetails);

        return requestProcessingDetails;
    }

    private MvcResult makePostRequest(final String requestUrl, final String body, String idempotencyKey) throws Exception {

        final var requestBuilder = post(requestUrl)
                .content(body)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        if (idempotencyKey != null) {
            requestBuilder.header(IDEMPOTENCY_KEY_HEADER, idempotencyKey);
        }

        return mockMvc.perform(requestBuilder)
                .andReturn();
    }

    private static String calculateRequestHash(String url, String method, String body) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(url.getBytes());
        messageDigest.update(method.getBytes());
        messageDigest.update(body.getBytes());
        return DatatypeConverter.printHexBinary(messageDigest.digest()).toLowerCase();
    }
}
