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

import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ericsson.amonboardingservice.presentation.models.OnboardingHeartbeat;
import com.ericsson.amonboardingservice.presentation.repositories.OnboardingHeartbeatRepository;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OnboardingHeartbeatService {

    @Autowired
    private OnboardingHeartbeatRepository onboardingHeartbeatRepository;

    @Value("${heartbeat.podName}")
    private String podName;

    @Transactional
    public void performHeartbeatUpdate() {
        LOGGER.debug("Starting heartbeat update, pod name {}", podName);

        final Optional<OnboardingHeartbeat> existingHeartbeatOptional = onboardingHeartbeatRepository.findById(podName);

        if (existingHeartbeatOptional.isPresent()) {
            final OnboardingHeartbeat existingHeartbeat = existingHeartbeatOptional.get();

            LOGGER.debug("Heartbeat entry exists in the database, latest update time is {}", existingHeartbeat.getLatestUpdateTime());
            existingHeartbeat.setLatestUpdateTime(LocalDateTime.now());
            LOGGER.debug("Updating heartbeat latest update time to {}", existingHeartbeat.getLatestUpdateTime());
            onboardingHeartbeatRepository.save(existingHeartbeat);
        } else {
            createHeartbeat();
        }

        LOGGER.debug("Completed heartbeat update");
    }

    public OnboardingHeartbeat findOrCreateHeartbeat() {
        final Optional<OnboardingHeartbeat> existingHeartbeatOptional = onboardingHeartbeatRepository.findById(podName);

        return existingHeartbeatOptional.orElseGet(this::createHeartbeat);
    }

    private OnboardingHeartbeat createHeartbeat() {
        final OnboardingHeartbeat heartbeat = new OnboardingHeartbeat();
        heartbeat.setPodName(podName);
        heartbeat.setLatestUpdateTime(LocalDateTime.now());

        LOGGER.debug("Heartbeat entry does not exist in the database, creating a new one");
        return onboardingHeartbeatRepository.save(heartbeat);
    }
}
