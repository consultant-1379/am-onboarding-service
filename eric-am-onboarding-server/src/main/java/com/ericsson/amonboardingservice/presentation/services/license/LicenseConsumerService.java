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

import java.util.EnumSet;

import org.springframework.web.client.RestClientException;

import com.ericsson.amonboardingservice.presentation.models.license.Permission;

public interface LicenseConsumerService {

    /**
     * @return list of permissions from license manager
     */
    EnumSet<Permission> getPermissions() throws RestClientException;

    /**
     * @return list of permissions from license manager
     */
    EnumSet<Permission> fetchPermissions() throws RestClientException;
}
