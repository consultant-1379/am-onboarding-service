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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.model.AppUsageStateRequest;
import com.ericsson.amonboardingservice.presentation.exceptions.PackageNotFoundException;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;

@SpringBootTest()
@ActiveProfiles("test")
public class AppPackageInstanceServiceTest extends AbstractDbSetupTest {
    private static final String EXISTING_PACKAGE_ID = "b3def1ce-4cf4-477c-aab3-21cb04e6a379";
    @Autowired
    private AppPackageInstanceService packageInstanceService;
    @Autowired
    private AppPackageRepository appPackageRepository;

    @Test
    public void updateUsageStateErrorForNonExistentPkgId() {
        AppUsageStateRequest appUsageStateRequest = new AppUsageStateRequest();
        appUsageStateRequest.setVnfId("a3def1ce-4cf4-477c-1111-21cb04e6a379");
        appUsageStateRequest.setIsInUse(true);

        assertThatThrownBy(() -> packageInstanceService.updatePackageUsageState("non-existent-pkgId-12345", appUsageStateRequest))
                .isInstanceOf(PackageNotFoundException.class)
                .hasMessageStartingWith("Package with id: non-existent-pkgId-12345 not found");
    }

    @Test
    public void shouldSaveNewAppPkgInstanceDetails() {
        String packageId = EXISTING_PACKAGE_ID;
        AppPackage appPackage = appPackageRepository.findByPackageId(packageId).get();

        AppUsageStateRequest appUsageStateRequest = new AppUsageStateRequest();
        appUsageStateRequest.setVnfId("a3def1ce-4cf4-477c-1111-21cb04e6a379");
        appUsageStateRequest.setIsInUse(true);

        packageInstanceService.updatePackageUsageState(packageId, appUsageStateRequest);

        assertThat(packageInstanceService.getPackageInstanceByAppPackageAndInstanceId(appPackage,
                "a3def1ce-4cf4-477c-1111-21cb04e6a379").isPresent()).isTrue();
    }

    @Test
    public void checkThatInstanceHaveTwoPackages() {
        AppUsageStateRequest appUsageStateRequest = new AppUsageStateRequest();
        appUsageStateRequest.setVnfId("a3def1ce-4cf4-477c-1111-21cb04e6a379");
        appUsageStateRequest.setIsInUse(true);

        packageInstanceService.updatePackageUsageState(EXISTING_PACKAGE_ID, appUsageStateRequest);

        AppPackage appPackageOld = appPackageRepository.findByPackageId(EXISTING_PACKAGE_ID).get();

        AppPackage appPackageNew = appPackageRepository.findByPackageId("e3def1ce-4cf4-477c-aab3-21cb04e6a379").get();

        packageInstanceService.updatePackageUsageState("e3def1ce-4cf4-477c-aab3-21cb04e6a379", appUsageStateRequest);

        assertThat(packageInstanceService.getPackageInstanceByAppPackageAndInstanceId(appPackageNew,
                "a3def1ce-4cf4-477c-1111-21cb04e6a379").isPresent()).isTrue();

        assertThat(packageInstanceService.getPackageInstanceByAppPackageAndInstanceId(appPackageOld,
                "a3def1ce-4cf4-477c-1111-21cb04e6a379").isPresent()).isTrue();
    }

    @Test
    public void shouldDeletePackageInstanceDtls() {
        AppPackage appPackage = appPackageRepository.findByPackageId(EXISTING_PACKAGE_ID).get();

        AppUsageStateRequest appUsageStateRequest = new AppUsageStateRequest();
        appUsageStateRequest.setVnfId("a3def1ce-4cf4-477c-1111-21cb04e6a379");
        appUsageStateRequest.setIsInUse(true);

        packageInstanceService.updatePackageUsageState(EXISTING_PACKAGE_ID, appUsageStateRequest);

        AppUsageStateRequest usageStateRequest = new AppUsageStateRequest();
        usageStateRequest.setVnfId("a3def1ce-4cf4-477c-1111-21cb04e6a379");
        usageStateRequest.setIsInUse(false);

        packageInstanceService.updatePackageUsageState(EXISTING_PACKAGE_ID, usageStateRequest);

        assertThat(packageInstanceService.getPackageInstanceByAppPackageAndInstanceId(appPackage, "a3def1ce-4cf4-477c-1111"
                        + "-21cb04e6a379")
                .isPresent()).isFalse();
    }
}