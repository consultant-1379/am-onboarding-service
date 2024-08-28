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
package com.ericsson.amonboardingservice.presentation.services.packageinstanceservice;

import java.util.Optional;

import com.ericsson.amonboardingservice.model.AppUsageStateRequest;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackageInstance;

public interface AppPackageInstanceService {

    Optional<AppPackageInstance> getPackageInstanceByAppPackageAndInstanceId(AppPackage appPackage, String vnfInstanceId);

    AppPackageInstance savePackageInstance(AppPackageInstance packageInstance);

    /***
     * Update the usage state of the onboarded package.
     *
     * @param pkgId
     *        The id of the package that was onboarded
     * @param appUsageStateRequest
     *        Details about the vnfInstanceId and the status to update
     */
    void updatePackageUsageState(String pkgId, AppUsageStateRequest appUsageStateRequest);
}
