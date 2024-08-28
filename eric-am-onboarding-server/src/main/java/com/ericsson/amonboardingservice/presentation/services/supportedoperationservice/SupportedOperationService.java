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
package com.ericsson.amonboardingservice.presentation.services.supportedoperationservice;

import com.ericsson.amonboardingservice.model.OperationDetailResponse;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.models.vnfd.OperationDetailEntity;

import java.util.List;

public interface SupportedOperationService {
    List<OperationDetailResponse> getSupportedOperations(String packageId);

    void deleteSupportedOperations(String appPackageId);

    List<OperationDetailEntity> parsePackageSupportedOperations(AppPackage appPackage);
}
