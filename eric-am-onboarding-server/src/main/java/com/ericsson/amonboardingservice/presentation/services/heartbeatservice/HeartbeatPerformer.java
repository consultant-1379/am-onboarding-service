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
package com.ericsson.amonboardingservice.presentation.services.heartbeatservice;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class HeartbeatPerformer {

    @Autowired
    private OnboardingHeartbeatService onboardingHeartbeatService;

    @Scheduled(fixedRateString = "${heartbeat.intervalSeconds}", timeUnit = TimeUnit.SECONDS)
    public void updateHeartbeat() {

        onboardingHeartbeatService.performHeartbeatUpdate();

    }
}
