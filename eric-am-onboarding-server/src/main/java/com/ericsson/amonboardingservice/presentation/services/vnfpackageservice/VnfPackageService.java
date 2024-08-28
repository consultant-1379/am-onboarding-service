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
package com.ericsson.amonboardingservice.presentation.services.vnfpackageservice;

import com.ericsson.amonboardingservice.model.VnfPkgInfo;
import com.ericsson.amonboardingservice.model.VnfPkgInfoV2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface VnfPackageService {
    /**
     * Retrieve details for a specific onboarded vnf package.
     *
     * @param id
     *
     * @return
     */
    Optional<VnfPkgInfo> getVnfPackage(String id);

    /**
     * List a page of Vnf Packages that have been onboarded.
     */
    Page<VnfPkgInfo> listVnfPackages(Pageable pageable);

    /**
     * List a page of Vnf Packages that have been onboarded and match the filter.
     */
    Page<VnfPkgInfo> listVnfPackages(String filter, Pageable pageable);

    /**
     * Retrieve details for a specific onboarded vnf package.
     *
     * @param id
     *
     * @return
     */
    Optional<VnfPkgInfoV2> getVnfPackageV2(String id);

    /**
     * List a page of Vnf Packages that have been onboarded.
     */
    Page<VnfPkgInfoV2> listVnfPackagesV2(Pageable pageable);

    /**
     * List a page of Vnf Packages that have been onboarded and match the filter.
     */
    Page<VnfPkgInfoV2> listVnfPackagesV2(String filter, Pageable pageable);
}
