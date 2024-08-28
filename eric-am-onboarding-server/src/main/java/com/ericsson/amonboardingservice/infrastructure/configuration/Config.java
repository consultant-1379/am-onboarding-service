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

import java.time.Duration;
import java.util.List;

import com.ericsson.amonboardingservice.infrastructure.client.DiskSpaceAwareRetryPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.services.dockerservice.RetrySkopeoCommand;
import com.ericsson.amonboardingservice.presentation.services.filter.EtsiVnfPackageQuery;
import com.ericsson.amonboardingservice.presentation.services.filter.VnfPackageQuery;
import com.ericsson.amonboardingservice.utils.Constants;
import com.ericsson.amonboardingservice.infrastructure.client.RetryProperties;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
@Profile({"dev", "prod", "db", "itest", "test"})
public class Config {

    @Autowired
    private AppPackageRepository appPackageRepository;

    @Bean
    public VnfPackageQuery getVnfPackageQuery() {
        return new VnfPackageQuery(appPackageRepository);
    }

    @Bean
    public EtsiVnfPackageQuery getEtsiVnfPackageQuery() {
        return new EtsiVnfPackageQuery(appPackageRepository);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(RetryProperties.DEFAULT.connectTimeout()))
                .setReadTimeout(Duration.ofMillis(RetryProperties.DEFAULT.requestTimeout())).build();
    }

    @Bean
    @Qualifier("licenseRestTemplate")
    public RestTemplate licenseRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(RetryProperties.LICENSE.connectTimeout()))
                .setReadTimeout(Duration.ofMillis(RetryProperties.LICENSE.requestTimeout())).build();
    }

    @Bean
    @Qualifier("basicRetryTemplate")
    public RetryTemplate retryTemplate() {
        return RetryTemplate.builder()
                .customPolicy(new DiskSpaceAwareRetryPolicy(RetryProperties.DEFAULT.maxAttempts()))
                .notRetryOn(HttpClientErrorException.class)
                .exponentialBackoff(1000, 2.0, 32000)
                .build();
    }

    @Bean
    @Qualifier("licenseRetryTemplate")
    public RetryTemplate licenseRetryTemplate() {
        return RetryTemplate.builder()
                .retryOn(RestClientException.class)
                .fixedBackoff(RetryProperties.LICENSE.backOff())
                .maxAttempts(RetryProperties.LICENSE.maxAttempts())
                .build();
    }

    @Bean
    @Qualifier("retrySkopeoCommand")
    public RetrySkopeoCommand retrySkopeoCommand() {
        return RetrySkopeoCommand.builder()
                .fixedBackOff(RetryProperties.SKOPEO.backOff())
                .maxAttempts(RetryProperties.SKOPEO.maxAttempts())
                .retryOn(List.of(Constants.SERVICE_UNAVAILABLE_EXCEPTION))
                .build();
    }

}