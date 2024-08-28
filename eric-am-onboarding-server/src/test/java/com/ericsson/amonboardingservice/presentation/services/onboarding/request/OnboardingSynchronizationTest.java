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
package com.ericsson.amonboardingservice.presentation.services.onboarding.request;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.presentation.models.OnboardingDetail;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageDatabaseService;

@SpringBootTest(classes = {
        OnboardingSynchronization.class
})
@TestPropertySource(
        properties = {
                "onboarding.sleepTime = 1",
        })
public class OnboardingSynchronizationTest {

    @Autowired
    private OnboardingSynchronization onboardingSynchronization;

    @MockBean
    private AppPackageRepository appPackageRepository;

    @MockBean
    private PackageDatabaseService packageDatabaseService;

    @Test
    public void testOnboardingSynchronization() {
        when(packageDatabaseService.getAppPackageById(any())).thenReturn(buildPackage(AppPackage.OnboardingStateEnum.UPLOADING,
                                                                                      LocalDateTime.now().plusMinutes(5)));
        when(appPackageRepository.existsByIsSetForDeletion(eq(true))).thenReturn(true, true, false);

        onboardingSynchronization.handle(new PackageUploadRequestContext(null,
                                                                         null,
                                                                         null,
                                                                         "dummy-id"));

        verify(appPackageRepository, times(3)).existsByIsSetForDeletion(eq(true));
        verify(appPackageRepository, times(1)).save(any());
    }

    @Test
    public void testSynchronizationForAlreadyProcessingPackage() {
        when(packageDatabaseService.getAppPackageById(any())).thenReturn(buildPackage(AppPackage.OnboardingStateEnum.PROCESSING,
                                                                                      LocalDateTime.now().plusMinutes(5)));

        onboardingSynchronization.handle(new PackageUploadRequestContext(null,
                                                                         null,
                                                                         null,
                                                                         "dummy-id"));

        verify(appPackageRepository, times(0)).existsByIsSetForDeletion(eq(true));
        verify(appPackageRepository, times(0)).save(any());
    }

    @Test
    public void testSynchronizationWithExpiredTime() {
        when(packageDatabaseService.getAppPackageById(any())).thenReturn(buildPackage(AppPackage.OnboardingStateEnum.UPLOADING,
                                                                                      LocalDateTime.now().minusSeconds(1)));

        assertThatThrownBy(() -> onboardingSynchronization.handle(new PackageUploadRequestContext(null,
                                                                         null,
                                                                         null,
                                                                                                  "dummy-id")))
                .isInstanceOf(InternalRuntimeException.class)
                .hasMessage("Package dummy-id could be processed due to timeout");

        verify(appPackageRepository, times(0)).existsByIsSetForDeletion(eq(true));
        verify(appPackageRepository, times(0)).save(any());
    }

    private AppPackage buildPackage(AppPackage.OnboardingStateEnum stateEnum, LocalDateTime expirationTime) {
        AppPackage appPackage = new AppPackage();
        appPackage.setPackageId("dummy-id");
        appPackage.setOnboardingState(stateEnum);
        OnboardingDetail onboardingDetail = new OnboardingDetail();
        onboardingDetail.setExpiredOnboardingTime(expirationTime);
        appPackage.setOnboardingDetail(onboardingDetail);
        return appPackage;
    }
}