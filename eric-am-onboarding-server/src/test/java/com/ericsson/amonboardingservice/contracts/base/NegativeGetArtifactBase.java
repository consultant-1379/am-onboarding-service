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
package com.ericsson.amonboardingservice.contracts.base;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.when;

import java.util.Optional;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.ApplicationServer;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageArtifactsRepository;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@SpringBootTest(classes = ApplicationServer.class)
public class NegativeGetArtifactBase extends AbstractDbSetupTest {

    @Inject
    private WebApplicationContext context;

    @MockBean
    AppPackageRepository appPackageRepository;

    @MockBean
    AppPackageArtifactsRepository appPackageArtifactsRepository;

    @BeforeEach
    public void setup() {
        when(appPackageRepository.findByPackageId(matches(".*NOT_FOUND"))).thenReturn(getPackage());
        when(appPackageArtifactsRepository.findByAppPackageAndArtifactPath(any(), matches(".*NOT_FOUND"))).thenReturn(null);
        RestAssuredMockMvc.webAppContextSetup(context);
    }

    public Optional<AppPackage> getPackage() {
        AppPackage appPackage = new AppPackage();
        appPackage.setOnboardingState(AppPackage.OnboardingStateEnum.ONBOARDED);
        return Optional.of(appPackage);
    }
}


