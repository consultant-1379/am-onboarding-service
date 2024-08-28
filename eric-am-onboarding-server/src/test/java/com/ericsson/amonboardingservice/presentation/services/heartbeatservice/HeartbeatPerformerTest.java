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

import static java.util.concurrent.TimeUnit.SECONDS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
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
public class HeartbeatPerformerTest extends AbstractDbSetupTest {

    protected static final String POD_NAME = "eric-am-onboarding-service-unit-test";

    @SpyBean
    private OnboardingHeartbeatRepository heartbeatRepository;

    @Test
    public void shouldPeriodicallyUpdateHeartbeat() {
        // given and when
        await().atMost(6, SECONDS)
                .untilAsserted(() -> verify(heartbeatRepository, atLeast(6)).save(any()));

        // then
        // verify the latest heartbeat
        final Optional<OnboardingHeartbeat> latestHeartbeat = heartbeatRepository.findById(POD_NAME);
        assertThat(latestHeartbeat)
                .isNotEmpty()
                .map(OnboardingHeartbeat::getLatestUpdateTime)
                .get()
                .asInstanceOf(InstanceOfAssertFactories.LOCAL_DATE_TIME)
                .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS));

        // verify heartbeats have been updated every second
        final ArgumentCaptor<OnboardingHeartbeat> heartbeatsCaptor = ArgumentCaptor.forClass(OnboardingHeartbeat.class);
        verify(heartbeatRepository, atLeast(6)).save(heartbeatsCaptor.capture());

        final List<OnboardingHeartbeat> capturedHeartbeats = heartbeatsCaptor.getAllValues();
        for (int i = 1; i < capturedHeartbeats.size(); i++) {
            final OnboardingHeartbeat previousHeartbeat = capturedHeartbeats.get(i - 1);
            final OnboardingHeartbeat currentHeartbeat = capturedHeartbeats.get(i);

            assertThat(currentHeartbeat.getLatestUpdateTime())
                    .isCloseTo(previousHeartbeat.getLatestUpdateTime().plusSeconds(1), within(500, ChronoUnit.MILLIS));
        }
    }
}
