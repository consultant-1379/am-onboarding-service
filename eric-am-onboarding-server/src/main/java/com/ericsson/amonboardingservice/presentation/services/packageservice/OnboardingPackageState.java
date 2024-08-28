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

import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum OnboardingPackageState {

    CREATED_NOT_IN_USE_DISABLED(AppPackage.AppUsageStateEnum.NOT_IN_USE, AppPackage.OnboardingStateEnum.CREATED,
                             AppPackage.OperationalStateEnum.DISABLED),
    ERROR_NOT_IN_USE_DISABLED(AppPackage.AppUsageStateEnum.NOT_IN_USE, AppPackage.OnboardingStateEnum.ERROR,
            AppPackage.OperationalStateEnum.DISABLED),
    UPLOADING_NOT_IN_USE_DISABLED(AppPackage.AppUsageStateEnum.NOT_IN_USE, AppPackage.OnboardingStateEnum.UPLOADING,
                             AppPackage.OperationalStateEnum.DISABLED),
    PROCESSING_NOT_IN_USE_DISABLED(AppPackage.AppUsageStateEnum.NOT_IN_USE, AppPackage.OnboardingStateEnum.PROCESSING,
                             AppPackage.OperationalStateEnum.DISABLED),
    ONBOARDED_NOT_IN_USE_ENABLED(AppPackage.AppUsageStateEnum.NOT_IN_USE, AppPackage.OnboardingStateEnum.ONBOARDED,
                             AppPackage.OperationalStateEnum.ENABLED),
    ONBOARDED_IN_USE_ENABLED(AppPackage.AppUsageStateEnum.IN_USE, AppPackage.OnboardingStateEnum.ONBOARDED,
                             AppPackage.OperationalStateEnum.ENABLED),
    ONBOARDED_IN_USE_DISABLED(AppPackage.AppUsageStateEnum.IN_USE, AppPackage.OnboardingStateEnum.ONBOARDED,
                             AppPackage.OperationalStateEnum.DISABLED),
    ONBOARDED_NOT_IN_USE_DISABLED(AppPackage.AppUsageStateEnum.NOT_IN_USE, AppPackage.OnboardingStateEnum.ONBOARDED,
                             AppPackage.OperationalStateEnum.DISABLED);

    @Getter
    private final AppPackage.AppUsageStateEnum usageState;
    @Getter
    private final AppPackage.OnboardingStateEnum onboardingState;
    @Getter
    private final AppPackage.OperationalStateEnum operationalState;

    public AppPackage setPackageState(final AppPackage appPackage) {
        appPackage.setUsageState(usageState);
        appPackage.setOnboardingState(onboardingState);
        appPackage.setOperationalState(operationalState);
        return appPackage;
    }
}
