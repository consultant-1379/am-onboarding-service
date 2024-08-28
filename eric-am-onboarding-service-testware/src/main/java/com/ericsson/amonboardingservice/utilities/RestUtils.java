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
package com.ericsson.amonboardingservice.utilities;

import com.ericsson.amonboardingservice.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.exceptions.TestRuntimeException;
import com.ericsson.amonboardingservice.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.ericsson.amonboardingservice.utilities.TestConstants.HOST;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.PUT;

@Slf4j
public final class RestUtils {
    private static final String AUTH_URI = "/auth/v1";
    public static final String JSESSIONID_TEMPLATE = "JSESSIONID=%s";
    public static final String COOKIE = "cookie";
    public static final String TOKEN = requestToken(HOST);

    private RestUtils() {
    }

    public static <T> ResponseEntity<List<T>> httpGetCall(String uri, ParameterizedTypeReference<List<T>> reference) {
        RestTemplate restTemplate = getRestTemplate();

        HttpHeaders httpHeaders = new HttpHeaders();
        if (AccTestUtils.isRunLocal()) {
            httpHeaders.set(COOKIE, String.format(JSESSIONID_TEMPLATE, TOKEN));
        }

        HttpEntity<Object> entity = new HttpEntity<>(httpHeaders);
        return restTemplate.exchange(uri, HttpMethod.GET, entity, reference);
    }

    public static <T> ResponseEntity<Map<T, T>> httpGetCallMap(String uri, ParameterizedTypeReference<Map<T, T>> reference) {
        RestTemplate restTemplate = getRestTemplate();

        HttpHeaders httpHeaders = new HttpHeaders();
        if (AccTestUtils.isRunLocal()) {
            httpHeaders.set(COOKIE, String.format(JSESSIONID_TEMPLATE, TOKEN));
        }

        HttpEntity<Object> entity = new HttpEntity<>(httpHeaders);
        return restTemplate.exchange(uri, HttpMethod.GET, entity, reference);
    }

    public static <T> ResponseEntity<T> httpGetCall(String uri, Class<T> returnType) {
        RestTemplate restTemplate = getRestTemplate();

        HttpHeaders httpHeaders = new HttpHeaders();
        if (AccTestUtils.isRunLocal()) {
            httpHeaders.set(COOKIE, String.format(JSESSIONID_TEMPLATE, TOKEN));
        }

        HttpEntity<Object> entity = new HttpEntity<>(httpHeaders);
        return restTemplate.exchange(uri, HttpMethod.GET, entity, returnType);
    }

    public static <T> ResponseEntity<T> httpGetCall(String uri, HttpHeaders httpHeaders, Class<T> returnType) {
        RestTemplate restTemplate = getRestTemplate();
        HttpEntity<Object> entity = new HttpEntity<>(httpHeaders);
        return restTemplate.exchange(uri, HttpMethod.GET, entity, returnType);
    }

    public static <T> ResponseEntity<T> httpPutCall(String uri, HttpEntity httpEntity, Class<T> returnType) {
        RestTemplate restTemplate = getRestTemplate();
        return restTemplate.exchange(uri, PUT, httpEntity, returnType);
    }

    public static <T> ResponseEntity<T> httpPostCall(String uri, HttpEntity httpEntity, Class<T> returnType) {
        RestTemplate restTemplate = getRestTemplate();
        return restTemplate.postForEntity(uri, httpEntity, returnType);
    }

    public static <T> T httpPostCall(String uri, Object request, Class<T> returnType) {
        RestTemplate restTemplate = getRestTemplate();
        HttpHeaders headers = new HttpHeaders();

        headers.set("Idempotency-key", UUID.randomUUID().toString());
        headers.set("Connection", "close, TE");

        if (AccTestUtils.isRunLocal()) {
            headers.set(COOKIE, String.format(JSESSIONID_TEMPLATE, TOKEN));
        }

        HttpEntity<Object> entity = new HttpEntity<>(request, headers);
        return restTemplate.postForObject(uri, entity, returnType);
    }

    public static <T> ResponseEntity<T> httpDeleteCall(String uri, Class<T> requestType) {
        RestTemplate restTemplate = getRestTemplate();

        HttpHeaders httpHeaders = new HttpHeaders();

        httpHeaders.set("Idempotency-key", UUID.randomUUID().toString());
        if (AccTestUtils.isRunLocal()) {
            httpHeaders.set(COOKIE, String.format(JSESSIONID_TEMPLATE, TOKEN));
        }
        HttpEntity<Object> entity = new HttpEntity<>(httpHeaders);
        return restTemplate.exchange(uri, DELETE, entity, requestType);
    }

    public static <T> T getRequestObjFromJson(String jsonRequest, Class<T> returnType) {
        try {
            return new ObjectMapper().readValue(jsonRequest, returnType);
        } catch (IOException e) {
            LOGGER.error("Unable to parse the json string:", e);
            throw new TestRuntimeException("Unable to parse the json string" + jsonRequest, e);
        }
    }

    public static String getAuthenticationHeader(User user) {
        String auth = String.format("%s:%s", user.getName(), user.getPassword());
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.US_ASCII));
        return "Basic " + new String(encodedAuth, StandardCharsets.US_ASCII);
    }

    static class IgnoreErrorsResponseHandler extends DefaultResponseErrorHandler {
        @Override
        public boolean hasError(ClientHttpResponse response) {
            return false;
        }
    }

    public static ClientHttpRequestFactory getRequestFactory() {
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(getPoolingHttpClientConnectionManager())
                .setRetryStrategy(DefaultHttpRequestRetryStrategy.INSTANCE)
                .setConnectionManagerShared(true)
                .build();
        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    private static PoolingHttpClientConnectionManager getPoolingHttpClientConnectionManager() {
        try {
            return PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                            .setSslContext(SSLContextBuilder.create()
                                    .loadTrustMaterial(TrustAllStrategy.INSTANCE)
                                    .build())
                            .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                            .build())
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            LOGGER.error(e.getMessage(), e);
            throw new InternalRuntimeException("Cannot create Http client connection manager");
        }
    }

    public static RestTemplate getRestTemplate() {
        return new RestTemplateBuilder().requestFactory(RestUtils::getRequestFactory)
                .errorHandler(new IgnoreErrorsResponseHandler())
                .build();
    }

    public static String requestToken(String onboardingHost) {
        if (AccTestUtils.isRunLocal()) {
            String vnfmUser = System.getProperty("vnfm.user");
            String vnfmPassword = System.getProperty("vnfm.password");
            RestTemplate restTemplate = getRestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("X-login", vnfmUser);
            headers.add("X-password", vnfmPassword);
            HttpEntity<String> entity = new HttpEntity<>("body", headers);

            String evnfmHost = onboardingHost.replace("/vnfm/onboarding", "");
            String authUrl = evnfmHost + AUTH_URI;
            return restTemplate.exchange(authUrl, HttpMethod.POST, entity, String.class).getBody();
        }
        return null;
    }
}
