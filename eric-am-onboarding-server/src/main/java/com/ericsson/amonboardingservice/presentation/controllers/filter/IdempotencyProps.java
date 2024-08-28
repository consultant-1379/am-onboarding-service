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

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "idempotency")
public class IdempotencyProps {

    private List<RequestLatency> retryAfter;

    public Integer findEndpointLatency(String endpoint, String method) {

        Optional<RequestLatency> requestLatency = retryAfter
                .stream().filter(obj -> obj.getMethod().equals(method) && obj.matches(endpoint))
                .findFirst();
        return requestLatency.map(RequestLatency::getLatency).orElse(null);
    }

    @Getter
    public static class RequestLatency {
        private String endpoint;
        private String method;
        private Integer latency;
        private Pattern compiledPattern;

        public void setEndpoint(final String endpoint) {
            this.endpoint = endpoint;
            compiledPattern = Pattern.compile(endpoint);
        }

        public void setMethod(final String method) {
            this.method = method;
        }

        public void setLatency(final Integer latency) {
            this.latency = latency;
        }

        public boolean matches(String url) {
            return compiledPattern.matcher(url).matches();
        }
    }
}