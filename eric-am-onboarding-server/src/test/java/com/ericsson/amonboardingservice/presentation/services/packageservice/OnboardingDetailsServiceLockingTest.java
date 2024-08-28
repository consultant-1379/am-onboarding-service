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

import static java.util.concurrent.TimeUnit.SECONDS;

import static org.assertj.core.api.Assertions.assertThatException;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import java.time.LocalDateTime;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.presentation.models.OnboardingDetail;
import com.ericsson.amonboardingservice.presentation.models.OnboardingHeartbeat;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.repositories.OnboardingHeartbeatRepository;
import com.ericsson.amonboardingservice.presentation.services.heartbeatservice.OnboardingHeartbeatService;

@SpringBootTest()
@ActiveProfiles("test")
public class OnboardingDetailsServiceLockingTest extends AbstractDbSetupTest {

    @Autowired
    private OnboardingDetailsService onboardingDetailsService;

    @Autowired
    private OnboardingHeartbeatRepository onboardingHeartbeatRepository;

    @Autowired
    private AppPackageRepository appPackageRepository;

    @MockBean
    private OnboardingHeartbeatService onboardingHeartbeatService;

    @BeforeEach
    public void setUp() {
        await().atMost(10, SECONDS).until(this::heartbeatIsCreated);
    }

    @Test
    public void testHeartBeatConcurrentUpdate() {
        final OnboardingHeartbeat basicHeartBeat = onboardingHeartbeatRepository.findAll().get(0);

        OnboardingHeartbeat firstOnboardingPodHeartBeat = createHeartbeat("eric-am-onboarding-service-1");
        OnboardingHeartbeat secondOnboardingPodHeartBeat = createHeartbeat("eric-am-onboarding-service-2");
        AppPackage appPackage = createAppPackage(basicHeartBeat);

        Mockito.when(onboardingHeartbeatService.findOrCreateHeartbeat()).thenReturn(firstOnboardingPodHeartBeat)
                        .thenReturn(secondOnboardingPodHeartBeat);

        onboardingDetailsService.setOnboardingHeartBeat(appPackage.getOnboardingDetail());
        assertThatException().isThrownBy(() -> onboardingDetailsService
                        .setOnboardingHeartBeat(appPackage.getOnboardingDetail()))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        OnboardingDetail onboardingDetailAfterUpdate = onboardingDetailsService.findOnboardingDetails(appPackage.getPackageId());
        OnboardingHeartbeat heartbeatAfterUpdate = onboardingDetailAfterUpdate.getOnboardingHeartbeat();

        Assertions.assertThat(heartbeatAfterUpdate.getPodName()).isEqualTo(firstOnboardingPodHeartBeat.getPodName());
    }

    private AppPackage createAppPackage(OnboardingHeartbeat heartbeat) {
        final AppPackage appPackage = new AppPackage();
        OnboardingPackageState.UPLOADING_NOT_IN_USE_DISABLED.setPackageState(appPackage);
        appPackage.setOnboardingDetail(createOnboardingDetail(appPackage, LocalDateTime.now().plusHours(1), heartbeat));

        return appPackageRepository.save(appPackage);
    }

    private static OnboardingDetail createOnboardingDetail(final AppPackage appPackage,
                                                           final LocalDateTime expiredOnboardingTime,
                                                           final OnboardingHeartbeat heartbeat) {
        final OnboardingDetail onboardingDetail = new OnboardingDetail();
        onboardingDetail.setAppPackage(appPackage);
        onboardingDetail.setExpiredOnboardingTime(expiredOnboardingTime);
        onboardingDetail.setOnboardingHeartbeat(heartbeat);
        onboardingDetail.setOnboardingPhase("Generate Cheksum");

        return onboardingDetail;
    }

    private OnboardingHeartbeat createHeartbeat(String podName) {
        OnboardingHeartbeat heartbeat = new OnboardingHeartbeat();
        heartbeat.setPodName(podName);
        heartbeat.setLatestUpdateTime(LocalDateTime.now());
        return onboardingHeartbeatRepository.save(heartbeat);
    }

    private boolean heartbeatIsCreated() {
        final List<OnboardingHeartbeat> existingHeartbeats = onboardingHeartbeatRepository.findAll();

        return !existingHeartbeats.isEmpty();
    }


}
