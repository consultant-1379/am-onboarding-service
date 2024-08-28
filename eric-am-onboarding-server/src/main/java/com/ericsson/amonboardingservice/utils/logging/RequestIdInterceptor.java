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

import brave.Span;
import brave.Tracer;
import brave.propagation.TraceContext;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;


@AllArgsConstructor
public class RequestIdInterceptor implements ClientHttpRequestInterceptor {

    private final Tracer tracer;
    private final String headerName;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        addXRequestIdToRequestIfNeeded(request);
        return execution.execute(request, body);
    }

    private void addXRequestIdToRequestIfNeeded(HttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        Optional.ofNullable(tracer.currentSpan())
                .map(Span::context)
                .map(TraceContext::traceIdString)
                .ifPresent(item -> headers.putIfAbsent(headerName, Collections.singletonList(item)));
    }

}
