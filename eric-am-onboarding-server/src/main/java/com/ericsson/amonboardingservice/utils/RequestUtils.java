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
package com.ericsson.amonboardingservice.utils;

import com.ericsson.amonboardingservice.infrastructure.client.RetryProperties;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.impl.DefaultConnectionReuseStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestTemplate;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class RequestUtils {
    private RequestUtils() {
    }

    public static RestTemplate noSslRestTemplate(String registryUser, String registryPassword)
            throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(getPoolingHttpClientConnectionManager())
                .setConnectionReuseStrategy(DefaultConnectionReuseStrategy.INSTANCE)
                .build();
        HttpComponentsClientHttpRequestFactory requestFactory = getClientHttpRequestFactory(httpClient);

        final List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(new BasicAuthenticationInterceptor(registryUser, registryPassword));
        return new RestTemplateBuilder().additionalInterceptors(interceptors)
                .requestFactory(() -> requestFactory)
                .build();
    }

    private static HttpComponentsClientHttpRequestFactory getClientHttpRequestFactory(CloseableHttpClient httpClient) {
        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);
        requestFactory.setConnectTimeout(Duration.ofMillis(RetryProperties.LICENSE.connectTimeout()).toMillisPart());
        requestFactory.setConnectionRequestTimeout(Duration.ofMillis(RetryProperties.LICENSE.requestTimeout()).toMillisPart());
        return requestFactory;
    }

    private static PoolingHttpClientConnectionManager getPoolingHttpClientConnectionManager()
            throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        return PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                        .setSslContext(SSLContextBuilder.create()
                                .loadTrustMaterial(TrustAllStrategy.INSTANCE)
                                .build())
                        .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                        .build())
                .build();
    }

}
