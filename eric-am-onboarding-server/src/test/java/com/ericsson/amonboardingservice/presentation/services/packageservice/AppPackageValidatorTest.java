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

import com.ericsson.amonboardingservice.presentation.exceptions.StateConflictException;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;

@SpringBootTest(classes = AppPackageValidator.class)
public class AppPackageValidatorTest {

    @Autowired
    private AppPackageValidator validator;

    @Test
    public void testValidatePackageStateIsCreated() {
        AppPackage appPackage = new AppPackage();
        appPackage.setOnboardingState(AppPackage.OnboardingStateEnum.CREATED);
        assertThatNoException().isThrownBy(() -> validator.validatePackageStateIsCreated(appPackage));
    }

    @Test
    public void testValidatePackageStateIsCreatedFails() {
        AppPackage appPackage = new AppPackage();
        appPackage.setPackageId("dummy-package-id");
        appPackage.setOnboardingState(AppPackage.OnboardingStateEnum.UPLOADING);
        assertThatExceptionOfType(StateConflictException.class)
                .isThrownBy(() -> validator.validatePackageStateIsCreated(appPackage))
                .withMessage("ID: dummy-package-id is not in CREATED state");
    }

    @Test
    public void testValidateOperationSupportedFails() {
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> validator.validateOperationSupported("UNSUPPORTED_OPERATION"))
                .withMessage("UNSUPPORTED_OPERATION operation not supported");
    }

    @Test
    public void testValidateIsAppPackageOnboardedState() {
        AppPackage appPackage = new AppPackage();
        appPackage.setPackageId("dummy-package-id");
        appPackage.setOnboardingState(AppPackage.OnboardingStateEnum.UPLOADING);

        assertThatExceptionOfType(StateConflictException.class)
                .isThrownBy(() -> validator.validateIsAppPackageOnboardedState(appPackage))
                .withMessage("ID: dummy-package-id is not in ONBOARDED state");
    }
}