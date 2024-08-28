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
package com.ericsson.amonboardingservice.utils.logging;

import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.List;


public class RequestIdRestTemplateCustomizer implements RestTemplateCustomizer {

    private final RequestIdInterceptor interceptor;

    public RequestIdRestTemplateCustomizer(RequestIdInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void customize(RestTemplate restTemplate) {
        List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
        boolean noRequestIdInterceptor = interceptors.stream().noneMatch(item -> item instanceof RequestIdInterceptor);
        if (noRequestIdInterceptor) {
            interceptors.add(interceptor);
        }
    }
}
