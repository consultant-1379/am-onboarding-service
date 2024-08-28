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
package com.ericsson.amonboardingservice.presentation.services.recoverservice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.amonboardingservice.presentation.services.onboarding.request.OnboardingPhase.IMAGES_ONBOARDING_PHASE;

import java.nio.file.Path;
import java.time.LocalDateTime;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ericsson.amonboardingservice.presentation.exceptions.FailedOnboardingValidationException;
import com.ericsson.amonboardingservice.presentation.exceptions.ImageOnboardingException;
import com.ericsson.amonboardingservice.presentation.exceptions.ObjectStorageException;
import com.ericsson.amonboardingservice.presentation.models.OnboardingDetail;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.services.cleanupservice.CleanupService;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.ericsson.amonboardingservice.presentation.services.filestorage.FileStorageService;
import com.ericsson.amonboardingservice.presentation.services.onboarding.request.OnboardingChain;
import com.ericsson.amonboardingservice.presentation.services.onboarding.request.OnboardingProcessHandler;
import com.ericsson.amonboardingservice.presentation.services.onboarding.request.PackageUploadRequestContext;
import com.ericsson.amonboardingservice.presentation.services.packageservice.CleanUpOnFailureService;
import com.ericsson.amonboardingservice.presentation.services.packageservice.OnboardingDetailsService;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageDatabaseService;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { RecoverService.class })
public class RecoverServiceTest {

    @MockBean
    private OnboardingDetailsService onboardingDetailsService;

    @MockBean
    private OnboardingChain onboardingChain;

    @MockBean
    private OnboardingProcessHandler onboardingProcessHandler;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private AppPackageRepository repository;

    @MockBean
    private PackageDatabaseService databaseService;

    @MockBean
    private CleanUpOnFailureService failureService;

    @MockBean
    private FileService fileService;

    @MockBean
    private CleanupService cleanupService;

    @Autowired
    private RecoverService recoverService;

    @Test
    public void testReassignAbandonedPackageSuccessful() {

        AppPackage appPackage = buildPackage();

        doNothing().when(onboardingDetailsService).setOnboardingHeartBeat(any());

        boolean result = recoverService.reassignAbandonedPackage(appPackage);

        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void testReassignAbandonedPackageFailed() {

        AppPackage appPackage = buildPackage();

        doThrow(new ObjectOptimisticLockingFailureException("Concurrent db entry modification", new Exception()))
                .when(onboardingDetailsService).setOnboardingHeartBeat(any());

        boolean result = recoverService.reassignAbandonedPackage(appPackage);

        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testProcessAbandonedPackageSuccessful() throws Exception {
        AppPackage appPackage = buildPackage();
        OnboardingDetail detail = appPackage.getOnboardingDetail();
        PackageUploadRequestContext context = detail.getPackageUploadContext();
        when(onboardingDetailsService.findOnboardingDetails(anyString())).thenReturn(detail);

        recoverService.processAbandonedPackage(appPackage);

        verify(fileStorageService, times(1)).storePackageFromObjectStorage(eq(context.getPackageContents().toString()),
                                                                           eq(context.getOriginalFileName()));
        verify(onboardingChain, times(1)).buildChainByLastPhase(eq(detail.getOnboardingPhase()));
        verify(onboardingProcessHandler, times(1)).startOnboardingProcess(eq(appPackage.getPackageId()), any());
        verify(fileStorageService, times(1)).deleteFileFromObjectStorage(any());
        verify(onboardingDetailsService, times(1)).resetOnboardingDetails(any());
    }

    @Test
    public void testProcessAbandonedPackageFailOnValidation() throws Exception {
        AppPackage appPackage = buildPackage();
        OnboardingDetail detail = appPackage.getOnboardingDetail();
        PackageUploadRequestContext context = detail.getPackageUploadContext();
        when(onboardingDetailsService.findOnboardingDetails(anyString())).thenReturn(detail);
        when(databaseService.getAppPackageById(anyString())).thenReturn(appPackage);

        doThrow(new FailedOnboardingValidationException("VNFD is not valid"))
                .when(onboardingChain).buildChainByLastPhase(any());

        recoverService.processAbandonedPackage(appPackage);

        verify(fileStorageService, times(1)).storePackageFromObjectStorage(eq(context.getPackageContents().toString()),
                                                                           eq(context.getOriginalFileName()));
        verify(onboardingChain, times(1)).buildChainByLastPhase(eq(detail.getOnboardingPhase()));
        verify(fileStorageService, times(1)).deleteFileFromObjectStorage(any());
        verify(onboardingDetailsService, times(1)).resetOnboardingDetails(any());
    }

    @Test
    public void testProcessAbandonedPackageFailOnImagePush() throws Exception {
        AppPackage appPackage = buildPackage();
        OnboardingDetail detail = appPackage.getOnboardingDetail();
        PackageUploadRequestContext context = detail.getPackageUploadContext();
        when(onboardingDetailsService.findOnboardingDetails(anyString())).thenReturn(detail);
        when(databaseService.getAppPackageById(anyString())).thenReturn(appPackage);

        doThrow(new ImageOnboardingException("Failed to push layer"))
                .when(onboardingChain).buildChainByLastPhase(any());

        recoverService.processAbandonedPackage(appPackage);

        verify(fileStorageService, times(1)).storePackageFromObjectStorage(eq(context.getPackageContents().toString()),
                                                                           eq(context.getOriginalFileName()));
        verify(onboardingChain, times(1)).buildChainByLastPhase(eq(detail.getOnboardingPhase()));
        verify(failureService, times(1)).cleanUpOnFailure(any(), any());
        verify(fileStorageService, times(1)).deleteFileFromObjectStorage(any());
        verify(onboardingDetailsService, times(1)).resetOnboardingDetails(any());
    }

    @Test
    public void testProccessAbandonedPackageFailOnSavingPackage() throws Exception {
        AppPackage appPackage = buildPackage();

        doThrow(new ObjectStorageException("Error while downloading file"))
                .when(fileStorageService).storePackageFromObjectStorage(any(), any());

        recoverService.processAbandonedPackage(appPackage);

        verify(onboardingDetailsService, times(1)).saveOnboadingHeartbeat(any(), any());
        verify(onboardingChain, times(0)).buildChainByLastPhase(any());
        verify(onboardingProcessHandler, times(0)).startOnboardingProcess(any(), any());
    }

    @Test
    public void testProcessAbandonedPackageFailWithEmptyContext() {
        OnboardingDetail onboardingDetail = new OnboardingDetail();
        onboardingDetail.setId(5);
        onboardingDetail.setVersion(359L);
        AppPackage appPackage = new AppPackage();
        appPackage.setPackageId("dummy-package-id");
        appPackage.setOnboardingDetail(onboardingDetail);

        recoverService.processAbandonedPackage(appPackage);

        verify(cleanupService, times(1)).cleanupStuckPackage(any(), any());
        verify(onboardingChain, times(0)).buildChainByLastPhase(any());
        verify(onboardingProcessHandler, times(0)).startOnboardingProcess(any(), any());
    }

    private AppPackage buildPackage() {
        PackageUploadRequestContext context = new PackageUploadRequestContext("dummy-filename",
                                                                              Path.of("dummy/directory/file.zip"),
                                                                              LocalDateTime.now(),
                                                                              "dummy-package-id");
        OnboardingDetail onboardingDetail = new OnboardingDetail();
        onboardingDetail.setOnboardingPhase(IMAGES_ONBOARDING_PHASE.toString());
        onboardingDetail.setPackageUploadContext(context);
        AppPackage appPackage = new AppPackage();
        appPackage.setPackageId("dummy-package-id");
        appPackage.setOnboardingDetail(onboardingDetail);

        return appPackage;
    }
}