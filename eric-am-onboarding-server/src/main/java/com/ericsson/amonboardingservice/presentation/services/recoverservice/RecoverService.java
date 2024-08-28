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

import static com.ericsson.amonboardingservice.presentation.services.packageservice.PackageServiceImpl.createErrorDetails;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.ericsson.amonboardingservice.presentation.exceptions.DataNotFoundException;
import com.ericsson.amonboardingservice.presentation.exceptions.FailedOnboardingException;
import com.ericsson.amonboardingservice.presentation.exceptions.FailedOnboardingValidationException;
import com.ericsson.amonboardingservice.presentation.exceptions.ObjectStorageException;
import com.ericsson.amonboardingservice.presentation.exceptions.PackageContextNotFoundException;
import com.ericsson.amonboardingservice.presentation.exceptions.UserInputException;
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
import com.ericsson.amonboardingservice.presentation.services.packageservice.OnboardingPackageState;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageDatabaseService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RecoverService {

    private static final String ERROR_DETAILS = "Can not recover package with id: %s due to %s";

    @Autowired
    private OnboardingDetailsService onboardingDetailsService;

    @Autowired
    private OnboardingChain onboardingChain;

    @Autowired
    private OnboardingProcessHandler onboardingProcessHandler;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private FileService fileService;

    @Autowired
    private CleanUpOnFailureService failureService;

    @Autowired
    private AppPackageRepository appPackageRepository;

    @Autowired
    private PackageDatabaseService databaseService;

    @Autowired
    private CleanupService cleanupService;

    @Async
    public void processAbandonedPackage(AppPackage appPackage) {
        String packageId = appPackage.getPackageId();
        LOGGER.info("Handling unfinished package {} due to failed pod", packageId);
        OnboardingDetail onboardingDetail = appPackage.getOnboardingDetail();

        try {
            downloadPackageFromObjectStore(onboardingDetail);
        } catch (ObjectStorageException ex) {
            LOGGER.error("Can not download package content for {} from object store due to {}", packageId, ex.getMessage());

            OnboardingDetail actualOnboardingDetail = onboardingDetailsService.findOnboardingDetails(packageId);
            /* In case of download package failure it needed that another pod or this pod later
               try to continue this onboarding process. So heartbeat is set to null
             */
            LOGGER.warn("Unmark package {} as being processed", packageId);
            onboardingDetailsService.saveOnboadingHeartbeat(actualOnboardingDetail, null);
            return;
        } catch (Exception ex) {
            LOGGER.error(String.format(ERROR_DETAILS, packageId, ex.getMessage()));
            cleanupService.cleanupStuckPackage(appPackage.getPackageId(), String.format(ERROR_DETAILS, packageId, ex.getMessage()));
            return;
        }

        String lastPhase = onboardingDetail.getOnboardingPhase();

        LOGGER.info("Onboarding process is continuing for package {} stopped on {} phase", packageId, lastPhase);

        tryStartOnboardingProcess(packageId, lastPhase);
    }

    public boolean reassignAbandonedPackage(AppPackage appPackage) {
        String packageId = appPackage.getPackageId();
        LOGGER.info("Assigning unfinished package {} due to failed pod", packageId);

        try {
            onboardingDetailsService.setOnboardingHeartBeat(appPackage.getOnboardingDetail());
            return true;
        } catch (ObjectOptimisticLockingFailureException ex) {
            LOGGER.warn("Unable to assign package {} as it's handled by another pod", packageId);
            return false;
        }
    }

    private void tryStartOnboardingProcess(String packageId, String lastPhase) {
        try {
            onboardingProcessHandler.startOnboardingProcess(packageId,
                                                            onboardingChain.buildChainByLastPhase(lastPhase));
        } catch (FailedOnboardingValidationException | FailedOnboardingException | DataNotFoundException
                 | UserInputException | IllegalArgumentException ex) {

            final OnboardingDetail onboardingDetails = onboardingDetailsService.findOnboardingDetails(packageId);

            persistErrorInformation(createErrorDetails(onboardingDetails.getOnboardingPhase(), ex.getMessage()),
                                    packageId, ex);
        } catch (Exception ex) {
            final OnboardingDetail onboardingDetails = onboardingDetailsService.findOnboardingDetails(packageId);

            persistErrorInformation(createErrorDetails(onboardingDetails.getOnboardingPhase(), ex.getMessage()),
                                    packageId, ex);
            failureService.cleanUpOnFailure(packageId, onboardingDetails.getPackageUploadContext());
        } finally {
            LOGGER.info("Finished onboarding processing");

            final OnboardingDetail onboardingDetails = onboardingDetailsService.findOnboardingDetails(packageId);
            final PackageUploadRequestContext updatedContext = onboardingDetails.getPackageUploadContext();

            if (!CollectionUtils.isEmpty(updatedContext.getArtifactPaths())) {
                updatedContext.getArtifactPaths().values()
                        .forEach(artifact -> fileService.deleteDirectory(artifact.getFileName().toString()));
            }
            fileStorageService.deleteFileFromObjectStorage(updatedContext.getPackageContents());
            onboardingDetailsService.resetOnboardingDetails(packageId);

            LOGGER.info("Finished cleanup resources after onboarding");
        }
    }

    private void persistErrorInformation(String errorDetails, String packageId, Throwable throwable) {
        if (!StringUtils.isEmpty(packageId)) {
            updatePackageStateOnException(OnboardingPackageState.ERROR_NOT_IN_USE_DISABLED,
                                          packageId,
                                          errorDetails,
                                          throwable);
        }
        LOGGER.error("Onboarding is failed with ErrorCode: {} and with Message: {}",
                     HttpStatus.BAD_REQUEST.value(), errorDetails, throwable);
    }

    private void updatePackageStateOnException(OnboardingPackageState onboardingPackageState, String packageId,
                                               String errorDetails, Throwable throwable) {
        try {
            AppPackage appPackage = databaseService.getAppPackageById(packageId);
            appPackage.setErrorDetails(errorDetails);
            onboardingPackageState.setPackageState(appPackage);
            appPackageRepository.save(appPackage);
        } catch (Exception e) {
            throwable.addSuppressed(e);
        }
    }

    private void downloadPackageFromObjectStore(final OnboardingDetail onboardingDetail) {
        PackageUploadRequestContext context = onboardingDetail.getPackageUploadContext();
        if (context == null) {
            throw new PackageContextNotFoundException("Package context hasn't been created");
        }
        fileStorageService.storePackageFromObjectStorage(context.getPackageContents().toString(),
                                                         context.getOriginalFileName());
    }
}
