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
package com.ericsson.amonboardingservice.presentation.services.cleanupservice;

import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.ericsson.amonboardingservice.presentation.services.packageservice.OnboardingPackageState;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageService;
import com.ericsson.amonboardingservice.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class CleanupService {

    private static final String ERROR_DETAILS = "Can not onboard package: timeout has been exceeded";

    @Autowired
    private AppPackageRepository appPackageRepository;

    @Autowired
    private FileService fileService;

    @Autowired
    private PackageService packageService;

    @Scheduled(fixedDelayString = "${onboarding.cleanup.deleteIntervalSeconds}", timeUnit = TimeUnit.SECONDS)
    public void cleanupPackagesSetForDeletion() {

        if (appPackageRepository.existsByOnboardingState(AppPackage.OnboardingStateEnum.PROCESSING)) {
            LOGGER.debug("Cleanup job for set for deletion packages postponed due to processing onboarding");
        } else {
            LOGGER.debug("Starting to delete packages flagged with isSetForDeletion in onboarding");

            final List<AppPackage> appPackagesSetForDeletion = packageService.getPackagesSetForDeletion();
            if (appPackagesSetForDeletion.isEmpty()) {
                LOGGER.debug("No packages set for deletion found");
                return;
            }

            LOGGER.debug("Found {} packages set for deletion in onboarding", appPackagesSetForDeletion.size());
            appPackagesSetForDeletion.forEach(appPackage -> {
                LOGGER.info("Removing package set for deletion with id {} that is currently in {} onboarding state",
                            appPackage.getPackageId(),
                            appPackage.getOnboardingState());
                packageService.removeAppPackageWithResources(appPackage.getPackageId());
            });

            LOGGER.debug("Completed to remove packages set for deletion");
        }
    }

    @Scheduled(fixedDelayString = "${onboarding.cleanup.cleanupIntervalSeconds}", timeUnit = TimeUnit.SECONDS)
    @Transactional
    public void cleanupStuckPackages() {
        LOGGER.debug("Starting cleanup of packages stuck in onboarding");

        final LocalDateTime expiredOnboardingTime = LocalDateTime.now();

        LOGGER.debug("Querying for packages with expired onboarding time before {}", expiredOnboardingTime);

        final List<AppPackage> stuckPackages = appPackageRepository.findAllNotFinishedWithExpirationBefore(expiredOnboardingTime);

        LOGGER.debug("Found {} packages stuck in onboarding", stuckPackages.size());

        stuckPackages.forEach(pkg -> resetStuckPackage(pkg, ERROR_DETAILS));

        LOGGER.debug("Completed stuck packages cleanup");
    }

    @Transactional
    public void cleanupStuckPackage(final String packageId, final String error) {
        LOGGER.debug("Starting cleanup of packages stuck in onboarding");
        Optional<AppPackage> stuckPackageOptional = appPackageRepository.findByPackageId(packageId);
        stuckPackageOptional.ifPresent(stuckPackage -> resetStuckPackage(stuckPackage, error));
        LOGGER.debug("Completed stuck packages cleanup");
    }

    @EventListener(ContextRefreshedEvent.class)
    public void cleanupTmpDirectory(final ContextRefreshedEvent event) {
        LOGGER.debug("Starting cleanup /tmp directories");
        fileService.deleteTempDirectories();
        LOGGER.debug("Completed cleanup /tmp directories");
    }

    private void resetStuckPackage(final AppPackage appPackage, final String error) {
        LOGGER.info("Resetting stuck package with id {} that is currently in {} onboarding state",
                appPackage.getPackageId(),
                appPackage.getOnboardingState());

        deleteDependentEntities(appPackage);
        resetPackageToCreatedState(appPackage, error);
    }

    private void deleteDependentEntities(final AppPackage appPackage) {
        appPackage.setServiceModelRecordEntity(null);
        clearCollection(appPackage.getChartsRegistryUrl());
        clearCollection(appPackage.getAppPackageDockerImages());
        clearCollection(appPackage.getOperationDetails());
        clearCollection(appPackage.getAppPackageArtifacts());
        appPackage.setOnboardingDetail(null);

        appPackageRepository.saveAndFlush(appPackage);
    }

    private void resetPackageToCreatedState(final AppPackage appPackage, final String error) {
        OnboardingPackageState.ERROR_NOT_IN_USE_DISABLED.setPackageState(appPackage);
        appPackage.setDescriptorId(null);
        appPackage.setDescriptorVersion(Constants.DEFAULT_PACKAGE_DESCRIPTOR_VERSION);
        appPackage.setDescriptorModel(null);
        appPackage.setProvider(Constants.DEFAULT_PACKAGE_PROVIDER_NAME);
        appPackage.setProductName(Constants.DEFAULT_PACKAGE_PRODUCT_NAME);
        appPackage.setSoftwareVersion(appPackage.getPackageId());
        appPackage.setChecksum(null);
        appPackage.setFiles(null);
        appPackage.setHelmfile(null);
        appPackage.setErrorDetails(error);
        appPackage.setVnfdZip(null);
        appPackage.setMultipleVnfd(false);
        appPackage.setPackageSecurityOption(null);

        appPackageRepository.saveAndFlush(appPackage);
    }

    private static <T> void clearCollection(final Collection<T> collection) {
        if (collection != null) {
            collection.clear();
        }
    }
}