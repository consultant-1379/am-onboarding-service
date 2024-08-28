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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.presentation.exceptions.PackageNotFoundException;
import com.ericsson.amonboardingservice.presentation.models.OnboardingDetail;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.repositories.OnboardingDetailRepository;
import com.ericsson.amonboardingservice.presentation.services.heartbeatservice.OnboardingHeartbeatService;
import com.ericsson.amonboardingservice.presentation.services.onboarding.request.PackageUploadRequestContext;

@SpringBootTest(classes = OnboardingDetailsService.class)
@MockBean(classes = {
        OnboardingHeartbeatService.class
})
public class OnboardingDetailsServiceTest {

    @Autowired
    private OnboardingDetailsService onboardingDetailsService;

    @MockBean
    private AppPackageRepository appPackageRepository;

    @MockBean
    private PackageDatabaseService databaseService;

    @MockBean
    private OnboardingDetailRepository onboardingDetailRepository;

    @Test
    public void saveOnboardingContextShouldSaveOnboardingDetailsWithUpdatedContext() {
        // given
        final OnboardingDetail onboardingDetails = new OnboardingDetail();
        onboardingDetails.setPackageUploadContext(new PackageUploadRequestContext());
        when(onboardingDetailRepository.findByAppPackagePackageId(any())).thenReturn(Optional.of(onboardingDetails));

        final PackageUploadRequestContext updatedContext = new PackageUploadRequestContext();

        // when
        onboardingDetailsService.saveOnboardingContext("package-id", updatedContext);

        // then
        final OnboardingDetail updatedOnboardingDetails = captureOnboardingDetails();
        assertThat(updatedOnboardingDetails.getPackageUploadContext()).isSameAs(updatedContext);
    }

    @Test
    public void findOnboardingDetailsShouldReturnExistingOnboardingDetails() {
        // given
        when(onboardingDetailRepository.findByAppPackagePackageId(any())).thenReturn(Optional.of(new OnboardingDetail()));

        // when and then
        assertThat(onboardingDetailsService.findOnboardingDetails("package-id")).isNotNull();
    }

    @Test
    public void findOnboardingDetailsShouldReturnThrowExceptionWhenOnboardingDetailsNotExist() {
        // given
        when(onboardingDetailRepository.findByAppPackagePackageId(any())).thenReturn(Optional.empty());

        // when and then
        assertThatException().isThrownBy(() -> onboardingDetailsService.findOnboardingDetails("package-id"))
                .isInstanceOf(InternalRuntimeException.class)
                .withMessage("Unexpected problem: onboarding details for app package package-id does not exist");
    }

    @Test
    public void setPhaseAndRetrieveCurrentContextShouldUpdatePhaseAndReturnContext() {
        // given
        final OnboardingDetail onboardingDetails = new OnboardingDetail();
        final PackageUploadRequestContext context = new PackageUploadRequestContext();
        onboardingDetails.setPackageUploadContext(context);
        when(onboardingDetailRepository.findByAppPackagePackageId(any())).thenReturn(Optional.of(onboardingDetails));
        when(onboardingDetailRepository.save(any())).thenReturn(onboardingDetails);

        // when
        final PackageUploadRequestContext result = onboardingDetailsService.setPhaseAndRetrieveCurrentContext("package-id", "phase1");

        // then
        final OnboardingDetail updatedOnboardingDetails = captureOnboardingDetails();
        assertThat(updatedOnboardingDetails.getOnboardingPhase()).isEqualTo("phase1");

        assertThat(result).isSameAs(context);
    }

    @Test
    public void resetOnboardingDetailsShouldThrowExceptionWhenAppPackageNotExist() {
        // given
        when(databaseService.getAppPackageById(any())).thenThrow(new PackageNotFoundException("Package with id: package-id not found"));

        // when and then
        assertThatException().isThrownBy(() -> onboardingDetailsService.resetOnboardingDetails("package-id"))
                .isInstanceOf(PackageNotFoundException.class)
                .withMessage("Package with id: package-id not found");
    }

    @Test
    public void resetOnboardingDetailsShouldDeleteOnboardingDetails() {
        // given
        final AppPackage appPackage = new AppPackage();
        final OnboardingDetail onboardingDetails = new OnboardingDetail();
        appPackage.setOnboardingDetail(onboardingDetails);
        when(databaseService.getAppPackageById(any())).thenReturn(appPackage);

        // when
        onboardingDetailsService.resetOnboardingDetails("package-id");

        // then
        final ArgumentCaptor<AppPackage> captor = ArgumentCaptor.forClass(AppPackage.class);
        verify(appPackageRepository).save(captor.capture());
        final AppPackage updatedAppPackage = captor.getValue();

        assertThat(updatedAppPackage.getOnboardingDetail()).isNull();
    }

    private OnboardingDetail captureOnboardingDetails() {
        final ArgumentCaptor<OnboardingDetail> captor = ArgumentCaptor.forClass(OnboardingDetail.class);
        verify(onboardingDetailRepository).save(captor.capture());

        return captor.getValue();
    }
}
