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
package com.ericsson.amonboardingservice.presentation.services.servicemodelservice;

import java.util.Optional;

import com.ericsson.amonboardingservice.model.ServiceModelRecordResponse;
import com.ericsson.amonboardingservice.presentation.models.ServiceModelRecordEntity;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.services.onboarding.request.PackageUploadRequestContext;

public interface ServiceModelService {

    /**
     * Retrieve details service model record by package id.
     *
     * @param packageId
     *
     * @return
     */
    Optional<ServiceModelRecordEntity> getServiceModelByPackageId(String packageId);

    /**
     * Creates and saves ServiceModelRecordEntity to repository if ServiceModel is present in
     * PackageUploadRequestContext. Returns null if there is no ServiceModel in PackageUploadRequestContext.
     *
     * @param context
     * @param appPackage
     *
     * @return ServiceModelRecordEntity
     */

    Optional<ServiceModelRecordEntity> saveServiceModelFromRequestContext(PackageUploadRequestContext context, AppPackage appPackage);

    /**
     * Retrieve service model record response by package id.
     *
     * @param packageId
     *
     * @return
     */
    Optional<ServiceModelRecordResponse> getServiceModelResponseByPackageId(String packageId);
}
