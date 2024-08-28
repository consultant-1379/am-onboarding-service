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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ericsson.signatureservice.SignatureService;
import com.ericsson.signatureservice.SignatureServiceImpl;
import com.ericsson.signatureservice.ca.TrustStoreServiceLocalImpl;

@Configuration
public class SignatureServiceConfig {

    @Value("${onboarding.root_ca_path}")
    private String rootCaPath;

    @Value("${onboarding.ignoreCrlInfo}")
    private boolean ignoreCrlInfo;

    @Value("${onboarding.skipCertificateKeyUsageValidation}")
    private boolean skipCertificateKeyUsageValidation;

    @Bean
    @ConditionalOnProperty(name = "onboarding.skipCertificateValidation", havingValue = "false")
    public SignatureService signatureService() {
        return new SignatureServiceImpl(new TrustStoreServiceLocalImpl(rootCaPath), ignoreCrlInfo, skipCertificateKeyUsageValidation);
    }
}
