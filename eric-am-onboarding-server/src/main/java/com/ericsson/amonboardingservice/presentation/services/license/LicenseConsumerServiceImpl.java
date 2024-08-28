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
package com.ericsson.amonboardingservice.presentation.services.license;

import java.util.Arrays;
import java.util.EnumSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.ericsson.amonboardingservice.presentation.models.license.Permission;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LicenseConsumerServiceImpl implements LicenseConsumerService {

    private static final String LICENSE_CONSUMER_PATH = "/lc/v1/cvnfm/permissions";

    private final String licenseServiceUrl;
    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;

    @Autowired
    public LicenseConsumerServiceImpl(@Value("${license.host}") final String licenseUrlHost,
                                      @Qualifier("licenseRestTemplate") final RestTemplate restTemplate,
                                      @Qualifier("licenseRetryTemplate") final RetryTemplate retryTemplate) {
        this.restTemplate = restTemplate;
        this.retryTemplate = retryTemplate;
        this.licenseServiceUrl = licenseUrlHost + LICENSE_CONSUMER_PATH;
    }

    public EnumSet<Permission> getPermissions() {
        try {
            return retryTemplate.execute((RetryCallback<EnumSet<Permission>, RestClientException>) context -> {
                LOGGER.debug("Fetching license permissions. Attempt: {}", context.getRetryCount());
                return fetchPermissions();
            });
        } catch (RestClientException e) {
            LOGGER.debug("Error while working with license manager: {}", e.getMessage(), e);
            return EnumSet.noneOf(Permission.class);
        }
    }

    public EnumSet<Permission> fetchPermissions() {
        ResponseEntity<Permission[]> response = restTemplate.getForEntity(licenseServiceUrl, Permission[].class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody() != null
                    ? Sets.newEnumSet(Arrays.asList(response.getBody()), Permission.class)
                    : EnumSet.noneOf(Permission.class);
        } else {
            throw new RestClientException("Unable to retrieve license manager data due to: " + response.getStatusCode());
        }
    }

}