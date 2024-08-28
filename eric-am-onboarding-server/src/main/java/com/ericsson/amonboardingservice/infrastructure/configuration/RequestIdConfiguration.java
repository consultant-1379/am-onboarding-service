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
package com.ericsson.amonboardingservice.infrastructure.configuration;

import brave.Tracer;
import com.ericsson.amonboardingservice.utils.logging.RequestIdInterceptor;
import com.ericsson.amonboardingservice.utils.logging.RequestIdRestTemplateCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "amonboardingservice.headers.request-id", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(RequestIdProperties.class)
public class RequestIdConfiguration {

    @Bean
    public RequestIdInterceptor interceptor(Tracer tracer, RequestIdProperties properties) {
        return new RequestIdInterceptor(tracer, properties.getHeaderName());
    }

    @Bean
    public RestTemplateCustomizer forwardRequestIdCustomizer(Tracer tracer, RequestIdProperties properties) {
        return new RequestIdRestTemplateCustomizer(interceptor(tracer, properties));
    }
}
