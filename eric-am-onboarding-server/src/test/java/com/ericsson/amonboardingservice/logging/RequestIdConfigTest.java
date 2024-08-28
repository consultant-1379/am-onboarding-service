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

import com.ericsson.amonboardingservice.infrastructure.configuration.RequestIdConfiguration;
import com.ericsson.amonboardingservice.utils.logging.RequestIdInterceptor;
import com.ericsson.amonboardingservice.utils.logging.RequestIdRestTemplateCustomizer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.tracing.BraveAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

public class RequestIdConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestTemplateAutoConfiguration.class))
            .withConfiguration(AutoConfigurations.of(RequestIdConfiguration.class));

    @Test
    public void testConfigIgnoreWithDisabledProperties() {
        contextRunner
                .withPropertyValues("amonboardingservice.headers.request-id.enabled=false")
                .run((context -> {
                    assertThat(context).doesNotHaveBean(RequestIdInterceptor.class);
                    assertThat(context).doesNotHaveBean(RequestIdRestTemplateCustomizer.class);
                }));
    }

    @Test
    public void testConfigIgnoreWithDefaultProperties() {
        contextRunner
                .withConfiguration(AutoConfigurations.of(BraveAutoConfiguration.class))
                .run((context -> {
                    assertThat(context).hasSingleBean(RequestIdInterceptor.class);
                    assertThat(context).hasSingleBean(RequestIdRestTemplateCustomizer.class);
                }));
    }

}
