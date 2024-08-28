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

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.ApplicationServer;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@SpringBootTest(classes = ApplicationServer.class)
public class NegativeGetHelmfileBase extends AbstractDbSetupTest {

    @Inject
    private WebApplicationContext context;

    @MockBean
    private AppPackageRepository appPackageRepository;

    @BeforeEach
    public void setup() {
        AppPackage value = new AppPackage();
        given(appPackageRepository.findByPackageId(contains("NO_HELMFILE"))).willReturn(Optional.of(value));
        RestAssuredMockMvc.webAppContextSetup(context);
    }

}
