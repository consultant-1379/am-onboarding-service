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

import static com.ericsson.amonboardingservice.presentation.services.onboarding.request.OnboardingPhase.ONBOARDING_SYNCHRONIZATION_PHASE;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.services.packageservice.OnboardingPackageState;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageDatabaseService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OnboardingSynchronization  implements RequestHandler {

    @Autowired
    private AppPackageRepository appPackageRepository;

    @Autowired
    private PackageDatabaseService packageDatabaseService;

    @Value("${onboarding.sleepTime}")
    private int onboardingSleepTime;

    @Override
    public void handle(final PackageUploadRequestContext context) {
        String packageId = context.getPackageId();
        AppPackage appPackage = packageDatabaseService.getAppPackageById(packageId);
        LocalDateTime expiredOnboardingTime = appPackage.getOnboardingDetail().getExpiredOnboardingTime();


        if (appPackage.getOnboardingState() != AppPackage.OnboardingStateEnum.PROCESSING) {
            while (isNotExpired(expiredOnboardingTime) && appPackageRepository.existsByIsSetForDeletion(true)) {
                LOGGER.info("Onboarding process for package {} postponed due to ongoing package deletion", packageId);
                sleepForTime(onboardingSleepTime);
            }

            if (isNotExpired(expiredOnboardingTime)) {
                updatePackageState(appPackage);
            } else {
                throw new InternalRuntimeException(String.format("Package %s could be processed due to timeout", packageId));
            }
        }
        LOGGER.info("Package {} is ready to process", packageId);
    }

    @Override
    public String getName() {
        return ONBOARDING_SYNCHRONIZATION_PHASE.toString();
    }

    private void sleepForTime(int sleepTime) {
        try {
            TimeUnit.of(ChronoUnit.SECONDS).sleep(sleepTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Thread has been interrupted by some other thread during sleeping. Exception details: {}", e.getMessage());
        }
    }

    private void updatePackageState(AppPackage appPackage) {
        LOGGER.info("Started updating package state with {} for package with id: {}",
                    OnboardingPackageState.PROCESSING_NOT_IN_USE_DISABLED,
                    appPackage.getPackageId());
        appPackageRepository.save(OnboardingPackageState.PROCESSING_NOT_IN_USE_DISABLED.setPackageState(appPackage));
        LOGGER.info("Completed updating package state with {} for package with id: {}",
                    OnboardingPackageState.PROCESSING_NOT_IN_USE_DISABLED, appPackage.getPackageId());
    }

    private boolean isNotExpired(LocalDateTime expirationTime) {
        return LocalDateTime.now().isBefore(expirationTime);
    }
}
