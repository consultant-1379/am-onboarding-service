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
package com.ericsson.amonboardingservice.presentation.services.packageservice;

import com.ericsson.amonboardingservice.model.AppPackageResponseV2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Interface for managing Onboarded package information.
 */
public interface PackageServiceV2 {

    /**
     * List all Packages for V2 API version that have been onboarded.
     */
    Page<AppPackageResponseV2> listPackagesV2(Pageable pageable);

    /**
     * List all Packages for V2 API version that matches the filter.
     */
    Page<AppPackageResponseV2> listPackagesV2(String filter, Pageable pageable);

    /**
     * List all Packages for V2 API version that have been onboarded with a specified verbosity.
     */
    Page<AppPackageResponseV2> listPackagesV2(PackageResponseVerbosity verbosity, Pageable pageable);

    /**
     * List all Packages for V2 API version that matches the filter with a specified verbosity.
     */
    Page<AppPackageResponseV2> listPackagesV2(String filter,
                                              PackageResponseVerbosity verbosity,
                                              Pageable pageable);

    /**
     * Retrieve details for V2 API version for a specific onboarded package.
     *
     * @param id
     * @return
     */
    Optional<AppPackageResponseV2> getPackageV2(String id);

}
