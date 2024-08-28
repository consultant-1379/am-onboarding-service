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

import com.ericsson.amonboardingservice.presentation.interceptors.ThreadLoggingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.ericsson.amonboardingservice.presentation.interceptors.VnfPackageLicenseInterceptor;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    private static final String VNF_PACKAGES_PATH = "/api/vnfpkgm/v1/vnf_packages/**";

    @Value("${onboarding.logConcurrentRequests}")
    private boolean logConcurrentRequests;

    @Autowired
    private VnfPackageLicenseInterceptor vnfPackageLicenseInterceptor;

    @Autowired
    private ThreadLoggingInterceptor threadLoggingInterceptor;

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseTrailingSlashMatch(true);
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        if (logConcurrentRequests) {
            registry.addInterceptor(threadLoggingInterceptor);
        }
        registry.addInterceptor(vnfPackageLicenseInterceptor)
                .addPathPatterns(VNF_PACKAGES_PATH);
    }
}
