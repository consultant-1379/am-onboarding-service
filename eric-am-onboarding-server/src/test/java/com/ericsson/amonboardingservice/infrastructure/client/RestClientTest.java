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
package com.ericsson.amonboardingservice.infrastructure.client;

import static java.nio.charset.Charset.defaultCharset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.HEAD;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.PERMANENT_REDIRECT;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

import java.io.File;
import java.nio.file.Paths;
import java.util.Optional;

import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.ApplicationServer;

@SpringBootTest(classes = ApplicationServer.class)
@ActiveProfiles("test")
public class RestClientTest extends AbstractDbSetupTest {

    private static final String NON_EXISTENT_RESOURCE_URL = "/non-existent-resource";
    private static final String EXISTENT_RESOURCE_URL = "/existent-resource";
    private static final String MALFUNCTIONING_HOST_URL = "/malfunctioning-host";
    private static final String NON_EXISTENT_RESOURCE_BODY = "{ message: \"Resource not found\"}";
    private static final String SERVICE_NOT_AVAILABLE_BODY = "{ message: \"Service is not available\"}";

    @Autowired
    private RestClient restClient;

    @MockBean(name = "restTemplate")
    private RestTemplate restTemplate;

    @Test
    public void shouldReturnResponseEntityOnSuccess() {
        when(restTemplate.exchange(contains(EXISTENT_RESOURCE_URL), eq(GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("", OK));

        ResponseEntity<String> response =
                restClient.get(EXISTENT_RESOURCE_URL, "user", "password");

        assertEquals(OK, response.getStatusCode());
    }

    @Test
    public void shouldExecutePostWithRetries() {
        when(restTemplate.exchange(contains(EXISTENT_RESOURCE_URL), eq(POST), any(), eq(String.class)))
                .thenThrow(new HttpServerErrorException(SERVICE_UNAVAILABLE, SERVICE_NOT_AVAILABLE_BODY))
                .thenThrow(new HttpServerErrorException(SERVICE_UNAVAILABLE, SERVICE_NOT_AVAILABLE_BODY))
                .thenReturn(new ResponseEntity<>("", OK));

        ResponseEntity<String> response =
                restClient.post(EXISTENT_RESOURCE_URL, "user", "password");

        assertThat(response.getStatusCode()).isEqualTo(OK);
        verify(restTemplate, times(3))
                .exchange(contains(EXISTENT_RESOURCE_URL), eq(POST), any(), eq(String.class));
    }

    @Test
    public void shouldHandleLocationRedirectOnPost() {
        var headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, EXISTENT_RESOURCE_URL);

        when(restTemplate.exchange(contains(MALFUNCTIONING_HOST_URL), eq(POST), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("Redirect", headers, PERMANENT_REDIRECT));
        when(restTemplate.exchange(contains(EXISTENT_RESOURCE_URL), eq(POST), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(EXISTENT_RESOURCE_URL, OK));

        ResponseEntity<String> response =
                restClient.post(MALFUNCTIONING_HOST_URL, "user", "password");

        assertThat(response.getStatusCode()).isEqualTo(OK);

        verify(restTemplate, times(1))
                .exchange(contains(MALFUNCTIONING_HOST_URL), eq(POST), any(), eq(String.class));
        verify(restTemplate, times(1))
                .exchange(contains(EXISTENT_RESOURCE_URL), eq(POST), any(), eq(String.class));
    }

    @Test
    public void shouldExecutePutWithRetries() {
        when(restTemplate.exchange(contains(EXISTENT_RESOURCE_URL), eq(PUT), any(), eq(String.class)))
                .thenThrow(new HttpServerErrorException(SERVICE_UNAVAILABLE, SERVICE_NOT_AVAILABLE_BODY))
                .thenThrow(new HttpServerErrorException(SERVICE_UNAVAILABLE, SERVICE_NOT_AVAILABLE_BODY))
                .thenReturn(new ResponseEntity<>("", OK));

        ResponseEntity<String> response =
                restClient.put(EXISTENT_RESOURCE_URL, "Body", "user", "password",
                        ContentType.TEXT_HTML.getMimeType());

        assertThat(response.getStatusCode()).isEqualTo(OK);
        verify(restTemplate, times(3))
                .exchange(contains(EXISTENT_RESOURCE_URL), eq(PUT), any(), eq(String.class));
    }

    @Test
    public void shouldReturnResponseEntityIfHttpStatusErrorExceptionOccurred() {
        when(restTemplate.exchange(contains(NON_EXISTENT_RESOURCE_URL), eq(GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(NON_EXISTENT_RESOURCE_BODY, NOT_FOUND));

        ResponseEntity<String> response =
                restClient.exchange(NON_EXISTENT_RESOURCE_URL, GET, new HttpHeaders(), "user", "password");

        assertEquals(NOT_FOUND, response.getStatusCode());
        assertEquals(NON_EXISTENT_RESOURCE_BODY, response.getBody());
    }

    @Test
    public void shouldReturnNotFoundStatusOnHeadMethodError() {
        when(restTemplate.exchange(contains(NON_EXISTENT_RESOURCE_URL), eq(HEAD), any(), eq(String.class)))
                .thenThrow(HttpClientErrorException.create(NOT_FOUND, "", new HttpHeaders(), new byte[0], defaultCharset()));

        final HttpStatusCode result = restClient.head(NON_EXISTENT_RESOURCE_URL, "user", "password");
        assertEquals(NOT_FOUND.value(), result.value());
        verify(restTemplate, times(1))
                .exchange(contains(NON_EXISTENT_RESOURCE_URL), eq(HEAD), any(), eq(String.class));
    }

    @Test
    public void shouldReturnOkWithRetriesOnHeadMethodSuccess() {
        when(restTemplate.exchange(contains(EXISTENT_RESOURCE_URL), eq(HEAD), any(), eq(String.class)))
                .thenThrow(new HttpServerErrorException(SERVICE_UNAVAILABLE, SERVICE_NOT_AVAILABLE_BODY))
                .thenThrow(new HttpServerErrorException(SERVICE_UNAVAILABLE, SERVICE_NOT_AVAILABLE_BODY))
                .thenReturn(new ResponseEntity<>("", OK));

        final HttpStatusCode result = restClient.head(EXISTENT_RESOURCE_URL, "user", "password");

        assertEquals(OK, result);
        verify(restTemplate, times(3))
                .exchange(contains(EXISTENT_RESOURCE_URL), eq(HEAD), any(), eq(String.class));
    }

    @Test
    public void shouldReturnNotFoundWithRetriesOnHeadMethodError() {
        when(restTemplate.exchange(contains(EXISTENT_RESOURCE_URL), eq(HEAD), any(), eq(String.class)))
                .thenThrow(new ResourceAccessException("I/O error on HEAD"))
                .thenThrow(new ResourceAccessException("I/O error on HEAD"))
                .thenThrow(HttpClientErrorException.create(NOT_FOUND, "", new HttpHeaders(), new byte[0], defaultCharset()))
                .thenThrow(new ResourceAccessException("I/O error on HEAD"));

        final HttpStatusCode result = restClient.head(EXISTENT_RESOURCE_URL, "user", "password");

        assertEquals(NOT_FOUND, result);
        verify(restTemplate, times(3))
                .exchange(contains(EXISTENT_RESOURCE_URL), eq(HEAD), any(), eq(String.class));
    }

    @Test
    public void shouldReturnOkOnHeadMethodSuccess() {
        when(restTemplate.exchange(contains(EXISTENT_RESOURCE_URL), eq(HEAD), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("", OK));

        final HttpStatusCode result = restClient.head(EXISTENT_RESOURCE_URL, "user", "password");

        assertEquals(OK.value(), result.value());
        verify(restTemplate, times(1))
                .exchange(contains(EXISTENT_RESOURCE_URL), eq(HEAD), any(), eq(String.class));
    }

    @Test
    public void shouldRetryOnException() {
        when(restTemplate.exchange(contains(EXISTENT_RESOURCE_URL), eq(GET), any(), eq(String.class)))
                .thenThrow(new ResourceAccessException("I/O error on POST"))
                .thenThrow(new HttpServerErrorException(SERVICE_UNAVAILABLE))
                .thenThrow(new HttpServerErrorException(SERVICE_UNAVAILABLE))
                .thenReturn(new ResponseEntity<>("", HttpStatus.OK));

        ResponseEntity<String> response =
                restClient.get(EXISTENT_RESOURCE_URL, "user", "password");

        verify(restTemplate, times(4))
                .exchange(contains(EXISTENT_RESOURCE_URL), eq(GET), any(), eq(String.class));
        assertEquals(OK, response.getStatusCode());
    }

    @Test
    public void shouldNotRetryOnNotFoundExceptionWhenGetFile() {
        when(restTemplate.exchange(contains(EXISTENT_RESOURCE_URL), eq(GET), any(), eq(String.class)))
                .thenThrow(new HttpServerErrorException(NOT_FOUND));

        Optional<File> response = restClient.getFile(EXISTENT_RESOURCE_URL, "filename.txt", "user", "password");
        assertTrue(response.isEmpty());
    }

    @Test
    public void shouldNotRetryOnNotFoundExceptionWhenGet() {
        when(restTemplate.exchange(contains(EXISTENT_RESOURCE_URL), eq(GET), any(), eq(String.class)))
                .thenThrow(new HttpServerErrorException(NOT_FOUND));

        assertThrows(HttpClientErrorException.class,
                () -> restClient.get(EXISTENT_RESOURCE_URL, "user", "password"));
    }

    @Test
    public void shouldNotRetryClientErrors() {
        when(restTemplate.exchange(contains(EXISTENT_RESOURCE_URL), eq(GET), any(), eq(String.class)))
                .thenThrow(new HttpClientErrorException(NOT_FOUND))
                .thenReturn(new ResponseEntity<>("", HttpStatus.OK));

        assertThrows(HttpClientErrorException.class,
                () -> restClient.get(EXISTENT_RESOURCE_URL, "user", "password"));
        verify(restTemplate, times(1))
                .exchange(contains(EXISTENT_RESOURCE_URL), eq(GET), any(), eq(String.class));
    }

    @Test
    public void shouldNotRetryNoDiskSpaceExceptionErrors() {
        when(restTemplate.exchange(contains(EXISTENT_RESOURCE_URL), eq(POST), any(), eq(String.class)))
                .thenThrow(new HttpServerErrorException(HttpStatusCode.valueOf(500),
                        "", "no space left on device".getBytes(), defaultCharset()))
                .thenReturn(new ResponseEntity<>("", HttpStatus.OK));

        assertThrows(HttpServerErrorException.class,
                () -> restClient.post(EXISTENT_RESOURCE_URL, "user", "password"));
        verify(restTemplate, times(1))
                .exchange(contains(EXISTENT_RESOURCE_URL), eq(POST), any(), eq(String.class));
    }

    @Test
    public void shouldExecutePostFileWithRetries() {
        when(restTemplate.exchange(contains(EXISTENT_RESOURCE_URL), eq(POST), any(), eq(String.class)))
                .thenThrow(new HttpServerErrorException(SERVICE_UNAVAILABLE, SERVICE_NOT_AVAILABLE_BODY))
                .thenThrow(new HttpServerErrorException(SERVICE_UNAVAILABLE, SERVICE_NOT_AVAILABLE_BODY))
                .thenReturn(new ResponseEntity<>("", OK));

        ResponseEntity<String> response =
                restClient.postFile(EXISTENT_RESOURCE_URL, Paths.get("some-file"), "user", "password");

        assertThat(response.getStatusCode()).isEqualTo(OK);
        verify(restTemplate, times(3))
                .exchange(contains(EXISTENT_RESOURCE_URL), eq(POST), any(), eq(String.class));
    }

    @Test
    public void shouldReturnResponseEntityOnPostFileIfHttpClientErrorExceptionOccurred() {
        when(restTemplate.exchange(contains(EXISTENT_RESOURCE_URL), eq(POST), any(), eq(String.class)))
                .thenThrow(HttpClientErrorException.create(CONFLICT,
                                                           CONFLICT.getReasonPhrase(),
                                                           HttpHeaders.EMPTY,
                                                           CONFLICT.getReasonPhrase().getBytes(defaultCharset()),
                                                           defaultCharset()));

        ResponseEntity<String> response =
                restClient.postFile(EXISTENT_RESOURCE_URL, Paths.get("some-file"), "user", "password");

        assertEquals(CONFLICT, response.getStatusCode());
        assertEquals(CONFLICT.getReasonPhrase(), response.getBody());
    }
}