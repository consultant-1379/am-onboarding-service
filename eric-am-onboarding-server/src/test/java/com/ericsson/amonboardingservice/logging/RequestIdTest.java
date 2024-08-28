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
package com.ericsson.amonboardingservice.logging;

import com.ericsson.amonboardingservice.infrastructure.configuration.RequestIdProperties;
import com.ericsson.amonboardingservice.presentation.services.helmservice.HelmService;
import com.ericsson.amonboardingservice.utils.JsonUtils;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "amonboardingservice.test.use-wire-mock=true")

@ActiveProfiles("test")
@Disabled("Need to be investigated after changes with helm chart registry")
public class RequestIdTest {
    private static final String TRACE_ID_HEADER = "X-B3-TraceId";
    static final String EXPECTED_BODY = JsonUtils.formatJsonString("{\"testBody\": \"Request was processed by wiremock helm registry\"}");

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Autowired
    private WireMockServer mockServer;

    @Autowired
    private RequestIdProperties properties;

    @Autowired
    private HelmService helmService;

    @AfterEach
    public void reset() {
        mockServer.resetToDefaultMappings();
    }

    @BeforeEach
    public void init() {
        this.restTemplate.getRestTemplate().getInterceptors().clear();
    }
    @Test
    public void addRequestIdWhenRequestWasSendFromUpStreamService() {
        //given
        String expected = "0f0479f0b560f9a0bec8984615b9e46e";
        HttpHeaders headers = new HttpHeaders();
        headers.set(TRACE_ID_HEADER, expected);
        headers.set("X-B3-SpanId", "9130b1c8064a2be8");
        HttpEntity<String> entity = new HttpEntity<>(headers);


        mockServer.stubFor(get(urlPathEqualTo("/api/onboarded/charts"))
                .withHeader(properties.getHeaderName(), equalTo(expected))
                .withHeader(TRACE_ID_HEADER, equalTo(expected))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(200)
                        .withBody(EXPECTED_BODY)
                )
        );
        //when
        ResponseEntity<String> response = this.restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/charts",
                HttpMethod.GET,
                entity,
                String.class);
        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(EXPECTED_BODY);
    }
    @Disabled("Disabled till solution of mockserver problem")
    @Test
    public void addRequestIdWhenRequestWasSendFromOnboardingService() {
        //given
        mockServer.stubFor(get(urlPathEqualTo("/api/onboarded/charts"))
                .withHeader(properties.getHeaderName(), matching("^[a-fA-F0-9]+$"))
                .withHeader(TRACE_ID_HEADER, matching("^[a-fA-F0-9]+$"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(200)
                        .withBody(EXPECTED_BODY)
                )
        );
        //when
        String response = helmService.listCharts();
        //then
        assertThat(response).isEqualTo(EXPECTED_BODY);
    }
}
