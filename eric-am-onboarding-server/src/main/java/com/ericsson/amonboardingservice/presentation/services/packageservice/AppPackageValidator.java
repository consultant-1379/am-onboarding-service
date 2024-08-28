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

import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.amonboardingservice.presentation.exceptions.StateConflictException;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.utils.Constants;
import org.springframework.stereotype.Component;

import static com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage.OnboardingStateEnum.CREATED;
import static com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage.OnboardingStateEnum.ONBOARDED;
import static java.lang.String.format;

@Component
public class AppPackageValidator {

    public void validatePackageStateIsCreated(AppPackage vnfPackage) {
        if (!CREATED.equals(vnfPackage.getOnboardingState())) {
            throw new StateConflictException(format(Constants.VNFD_CONFLICT_PACKAGE_STATE,
                    vnfPackage.getPackageId(), CREATED));
        }
    }

    public void validateOperationSupported(final String operationType) {
        if (!LCMOperationsEnum.isOperationSupported(operationType)) {
            throw new UnsupportedOperationException(format(Constants.OPERATION_NOT_SUPPORTED, operationType));
        }
    }

    public void validateIsAppPackageOnboardedState(AppPackage appPackage) {
        if (!ONBOARDED.equals(appPackage.getOnboardingState())) {
            throw new StateConflictException(format(Constants.VNFD_CONFLICT_PACKAGE_STATE, appPackage.getPackageId(), ONBOARDED));
        }
    }
}
