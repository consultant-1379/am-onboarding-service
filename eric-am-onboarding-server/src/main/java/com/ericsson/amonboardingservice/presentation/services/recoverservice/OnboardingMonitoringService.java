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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name = "onboarding.highAvailabilityMode", havingValue = "true")
public class OnboardingMonitoringService {

    @Autowired
    private RecoverService recoverService;

    @Autowired
    private AppPackageRepository appPackageRepository;

    @Value("${heartbeat.intervalSeconds}")
    private long heartbeatInterval;

    @Scheduled(fixedRateString = "${monitoring.intervalSeconds}", timeUnit = TimeUnit.SECONDS)
    public void monitorAbandonedPackages() {

        List<AppPackage> abandonedOnboardings =
                appPackageRepository.findAllNotFinishedWithoutProcessing(LocalDateTime.now()
                                                                                 .minus(2 * heartbeatInterval,
                                                                                        ChronoUnit.SECONDS));
        LOGGER.info("Found {} packages without processing", abandonedOnboardings.size());

        for (AppPackage appPackage: abandonedOnboardings) {
            if (recoverService.reassignAbandonedPackage(appPackage)) {
                recoverService.processAbandonedPackage(appPackage);
                break;
            }
        }
    }

}
