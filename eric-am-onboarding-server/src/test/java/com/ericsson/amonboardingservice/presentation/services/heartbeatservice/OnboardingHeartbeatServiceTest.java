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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.presentation.models.OnboardingHeartbeat;
import com.ericsson.amonboardingservice.presentation.repositories.OnboardingHeartbeatRepository;

@SpringBootTest
@TestPropertySource(properties = {
        "heartbeat.intervalSeconds=1",
        "heartbeat.podName=" + HeartbeatPerformerTest.POD_NAME })
@ActiveProfiles("test")
public class OnboardingHeartbeatServiceTest extends AbstractDbSetupTest {

    protected static final String POD_NAME = "eric-am-onboarding-service-unit-test";

    @Autowired
    private OnboardingHeartbeatService onboardingHeartbeatService;

    @MockBean
    private OnboardingHeartbeatRepository onboardingHeartbeatRepository;

    @Test
    public void findOrCreateHeartbeatWhenExist() {
        OnboardingHeartbeat onboardingHeartbeat = new OnboardingHeartbeat();
        onboardingHeartbeat.setPodName(POD_NAME);
        onboardingHeartbeat.setLatestUpdateTime(LocalDateTime.now());

        when(onboardingHeartbeatRepository.findById(eq(POD_NAME)))
                .thenReturn(Optional.of(onboardingHeartbeat));

        OnboardingHeartbeat heartbeatFromDb = onboardingHeartbeatService.findOrCreateHeartbeat();
        assertThat(heartbeatFromDb).isNotNull();
    }

    @Test
    public void findOrCreateHeartbeatWhenNotExist() {

        when(onboardingHeartbeatRepository.findById(eq(POD_NAME)))
                .thenReturn(Optional.empty());
        when(onboardingHeartbeatRepository.save(any(OnboardingHeartbeat.class)))
                .thenAnswer(i -> i.getArguments()[0]);


        OnboardingHeartbeat heartbeatFromDb = onboardingHeartbeatService.findOrCreateHeartbeat();
        assertThat(heartbeatFromDb).isNotNull();
        assertThat(heartbeatFromDb.getPodName()).isEqualTo(POD_NAME);
        assertThat(heartbeatFromDb.getLatestUpdateTime()).isNotNull();

    }
}