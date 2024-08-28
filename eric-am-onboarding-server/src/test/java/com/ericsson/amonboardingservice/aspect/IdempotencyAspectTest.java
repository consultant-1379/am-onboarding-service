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
package com.ericsson.amonboardingservice.aspect;

import static com.ericsson.amonboardingservice.TestUtils.getInMemoryAppender;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static com.ericsson.amonboardingservice.utils.Constants.IDEMPOTENCY_KEY_HEADER;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.logging.InMemoryAppender;
import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.presentation.services.idempotency.IdempotencyService;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

@SpringBootTest
@AutoConfigureMockMvc
public class IdempotencyAspectTest extends AbstractDbSetupTest {

    @SpyBean
    private IdempotencyAspect idempotencyAspect;

    @MockBean
    private IdempotencyService idempotencyService;

    final private ObjectMapper objectMapper = new ObjectMapper();

    {
        objectMapper.registerModule(new JsonNullableModule());
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testIdempotencyAspectIdempotencyKeyFail() throws Throwable {
        final InMemoryAppender inMemoryAppender = getInMemoryAppender();

        String packageId = "duummy";
        final RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get(String.format("/api/vnfpkgm/v1/vnf_packages/%s/helmfile", packageId))
                .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(requestBuilder).andReturn();

        assertThat(inMemoryAppender.contains(String.format("/api/vnfpkgm/v1/vnf_packages/%s/helmfile", packageId), Level.WARN)).isTrue();
        verify(idempotencyService, times(0)).saveIdempotentResponse(any(), any());
        verify(idempotencyAspect, times(1)).around(any());
    }

    @Test
    public void testIdempotencyAspectCheckInternalRuntimeExceptionFail() throws Throwable {
        final InMemoryAppender inMemoryAppender = getInMemoryAppender();
        String idempotencyKey = "dummyKey";

        doThrow(new InternalRuntimeException("")).when(idempotencyService).saveIdempotentResponse(eq(idempotencyKey), any());

        String packageId = "dummy";
        final RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get(String.format("/api/vnfpkgm/v1/vnf_packages/%s/helmfile", packageId))
                .accept(MediaType.APPLICATION_JSON)
                .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey);

        mockMvc.perform(requestBuilder).andReturn();

        assertThat(inMemoryAppender.contains(String.format("The request processing details was not updated for idempotencyKey = %s",
                                                           idempotencyKey),
                                             Level.ERROR)).isTrue();
        verify(idempotencyService, times(1)).saveIdempotentResponse(eq(idempotencyKey), any());
        verify(idempotencyAspect, times(1)).around(any());
    }

    @Test
    public void testIdempotencyAspectResponseEntitySuccessful() throws Throwable {
        String idempotencyKey = "dummyKey";

        String packageId = "dummy";
        final RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get(String.format("/api/vnfpkgm/v1/vnf_packages/%s/helmfile", packageId))
                .accept(MediaType.APPLICATION_JSON)
                .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey);

        mockMvc.perform(requestBuilder).andReturn();

        verify(idempotencyService, times(1)).saveIdempotentResponse(eq(idempotencyKey), any());
        verify(idempotencyAspect, times(1)).around(any());
    }

    @Test
    public void testIdempotencyAspectErrorMessageSuccessful() throws Throwable {
        String idempotencyKey = "dummyKey";

        String packageId = "f3def1ce-4cf4-477c-123";

        final RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get(String.format("/api/vnfpkgm/v1/vnf_packages/%s/helmfile", packageId))
                .accept(MediaType.APPLICATION_JSON)
                .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey);

        mockMvc.perform(requestBuilder).andReturn();

        verify(idempotencyService, times(1)).saveIdempotentResponse(eq(idempotencyKey), any());
        verify(idempotencyAspect, times(1)).around(any());
    }

    @Test
    public void testIdempotencyAspectIdempotencyKeyIsNotPresentFail() throws Throwable {
        final InMemoryAppender inMemoryAppender = getInMemoryAppender();
        final ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        doReturn(new Object()).when(pjp).proceed();
        idempotencyAspect.around(pjp);

        assertThat(inMemoryAppender.contains("Idempotency key is not present for call to ",
                                             Level.WARN)).isTrue();
        verify(idempotencyService, times(0)).saveIdempotentResponse(any(), any());
    }

}
