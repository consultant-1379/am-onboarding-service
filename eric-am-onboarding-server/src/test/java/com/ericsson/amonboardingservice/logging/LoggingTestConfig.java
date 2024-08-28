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
import org.springframework.boot.actuate.endpoint.invoke.OperationParameter;
import org.springframework.boot.actuate.endpoint.invoke.ParameterMappingException;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.mock.env.MockPropertySource;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@Configuration
public class LoggingTestConfig {
    @Bean
    @ConditionalOnMissingBean
    HelmService helmService(WireMockServer mockServer, ConfigurableApplicationContext applicationContext) {
        MutablePropertySources propertySources = applicationContext.getEnvironment().getPropertySources();
        MockPropertySource mockEnvVars = new MockPropertySource().withProperty("helm.registry.port", mockServer.port());
        propertySources.addFirst(mockEnvVars);
        return new HelmService();
    }

    @Bean(destroyMethod = "stop")
    @ConditionalOnProperty(prefix = "amonboardingservice.test", name = "use-wire-mock", havingValue = "true")
    public WireMockServer mockServer() {
        WireMockServer mockServer = new WireMockServer(wireMockConfig().dynamicPort());
        mockServer.start();
        return mockServer;
    }

    @Bean
    public ParameterValueMapper parameterValueMapper() {
        return new ParameterValueMapper() {
            @Override
            public Object mapParameterValue(OperationParameter parameter, Object value) throws ParameterMappingException {
                return null;
            }
        };
    }
}
