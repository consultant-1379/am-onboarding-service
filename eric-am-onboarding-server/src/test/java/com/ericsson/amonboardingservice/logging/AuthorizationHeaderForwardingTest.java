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

import com.ericsson.amonboardingservice.presentation.services.helmservice.HelmService;
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
import org.springframework.test.util.ReflectionTestUtils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "amonboardingservice.test.use-wire-mock=true")
@ActiveProfiles("test")
@Disabled("Removing for now so that the authorization header for the call to the docker registry is not replaced.")
public class AuthorizationHeaderForwardingTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Autowired
    private WireMockServer mockServer;

    @Autowired
    private HelmService helmService;

    @BeforeEach
    public void init() {
        restTemplate.getRestTemplate().getInterceptors().clear();
        ReflectionTestUtils.setField(helmService, "port", Integer.toString(mockServer.port()));
    }

    @AfterEach
    public void reset() {
        mockServer.resetToDefaultMappings();
    }

    @Test
    public void forwardAuthorizationHeaderWhenItExistInIncomingRequest() {
        //given
        String token = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJncWFMd1h0TG5UbldULXpJTW1KSm43OE15MWRESm8xWmwwSFplcThiUHdZIn0.eyJqdGkiOiIwZjNmY2QzNC02YzhkLTQyM2EtYjBiMi1lMmFjMDZiODZkOGMiLCJleHAiOjE1NTg2OTc4NzIsIm5iZiI6MCwiaWF0IjoxNTU4Njk3NTcyLCJpc3MiOiJodHRwOi8vaWFtLmdlci50b2RkMDQxLnJuZC5naWMuZXJpY3Nzb24uc2UvYXV0aC9yZWFsbXMvQURQLUFwcGxpY2F0aW9uLU1hbmFnZXIiLCJhdWQiOiJlcmljc3Nvbi1hcHAiLCJzdWIiOiI2ZTNhNDViYy0zYmVkLTQwMGQtOWNlZi0yMmMyNDIyYjRhZWEiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJlcmljc3Nvbi1hcHAiLCJhdXRoX3RpbWUiOjE1NTg2OTc1NzIsInNlc3Npb25fc3RhdGUiOiJhYjkzMTVlZC01NDgyLTRlZjEtYWYzNi1hN2U3NzkzOWRhZWIiLCJhY3IiOiIxIiwiYWxsb3dlZC1vcmlnaW5zIjpbXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6Im9wZW5pZCBwcm9maWxlIG9mZmxpbmVfYWNjZXNzIGVtYWlsIGFkZHJlc3MgcGhvbmUiLCJhZGRyZXNzIjp7fSwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJuYW1lIjoiZnVuY1VzZXIgZnVuY1VzZXIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJmdW5jdXNlciIsImdpdmVuX25hbWUiOiJmdW5jVXNlciIsImZhbWlseV9uYW1lIjoiZnVuY1VzZXIiLCJlbWFpbCI6ImZ1bmN1c2VyQGVyaWNzc29uLmNvbSJ9.JKyofwJUccHUIIWeNeSWyDsjJOOltMqn3ia1amb6RdJQCD6cZr4O1GPI6fgjDGfyC_rfayk9MReF5ZUmmwfVFLTXRKfWGUWMccmwRsFAaJy1Sl_29L_38kBeDGtOBByHHXylLN3F5wub0qjmr7VSQRnRcabtRrSf-9FfvHfYhDduCBJUPnkvSec8djbpfY3nSP-W5Wj9QT8BuTk-MtDwJ_D-JnFFBvguDNab_7ClJ96_TevyblCowfDFpEUaqKl-OWJO2_-3FiCrolqxFisFXhvGzFGWCwp7wExUGO7otTBPmko57IbMtpAqH3iJcBx_D8QZnNe1ndW8HXTCS-ik7g";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        mockServer.stubFor(get(urlPathEqualTo("/api/onboarded/charts"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(token))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(200)
                        .withBody(RequestIdTest.EXPECTED_BODY)
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
        assertThat(response.getBody()).isEqualTo(RequestIdTest.EXPECTED_BODY);
    }
}
