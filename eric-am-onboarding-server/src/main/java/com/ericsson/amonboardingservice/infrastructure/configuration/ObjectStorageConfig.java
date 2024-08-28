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

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.ericsson.amonboardingservice.utils.TLSConfigurationUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class ObjectStorageConfig {

    @Value("${objectStorage.host}")
    private String hostname;

    @Value("${objectStorage.port}")
    private String port;

    @Value("${objectStorage.credentials.accessKey}")
    private String accessKey;

    @Value("${objectStorage.credentials.secretKey}")
    private String secretKey;

    @Value("${objectStorage.retry.maxAttempts}")
    private int retryMaxAttempts;

    @Value("${objectStorage.secure}")
    private boolean secure;

    @Value("${objectStorage.certFilePath}")
    private String caCertFilePath;

    @Value("${objectStorage.keyStore.password}")
    private String keyStorePassword;

    @Bean
    @ConditionalOnProperty(name = "onboarding.highAvailabilityMode", havingValue = "true")
    public AmazonS3 amazonS3() {

        try {
            String objectStorageEndpoint = buildEndpointUrl(hostname, port, secure);
            ClientConfiguration clientConfiguration = createClientConfiguration(retryMaxAttempts);

            AmazonS3ClientBuilder amazonS3ClientBuilder = AmazonS3Client.builder()
                    .withEndpointConfiguration(new AwsClientBuilder
                            .EndpointConfiguration(objectStorageEndpoint, null))
                    .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                    .withPathStyleAccessEnabled(true)
                    .withClientConfiguration(clientConfiguration);

            if (secure) {
                LOGGER.info("Adding certificates to establish TLS connection");
                amazonS3ClientBuilder.withClientConfiguration(
                        TLSConfigurationUtil.getTlsClientConfiguration(caCertFilePath, keyStorePassword));
            }

            return amazonS3ClientBuilder.build();
        } catch (IOException | GeneralSecurityException ex) {
            LOGGER.error("AmazonS3 client configuration failed due to: {}", ex.getMessage());
            throw new RuntimeException("AmazonS3 client create failed");
        }
    }

    private static String buildEndpointUrl(String host, String port, boolean secure) {
        if (secure) {
            return String.format("https://%s:%s", host, port);
        }
        return String.format("http://%s:%s", host, port);
    }

    private static ClientConfiguration createClientConfiguration(int retryMaxAttempts) {
        return new ClientConfiguration()
                .withRetryPolicy(PredefinedRetryPolicies.getDefaultRetryPolicyWithCustomMaxRetries(retryMaxAttempts));
    }
}
