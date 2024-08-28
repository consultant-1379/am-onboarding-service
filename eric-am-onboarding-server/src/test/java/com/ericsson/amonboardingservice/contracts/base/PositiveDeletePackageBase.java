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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

import jakarta.inject.Inject;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.ApplicationServer;
import com.ericsson.amonboardingservice.model.AppPackageResponse;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageService;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("dev")
@SpringBootTest(classes = ApplicationServer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class PositiveDeletePackageBase extends AbstractDbSetupTest {

    @Inject
    private WebApplicationContext context;

    @MockBean
    private PackageService packageService;

    @BeforeEach
    public void setup() {
        AppPackageResponse value = new AppPackageResponse();
        value.setUsageState(AppPackageResponse.UsageStateEnum.NOT_IN_USE);
        given(packageService.getPackage(anyString())).willReturn(Optional.of(value));
        doNothing().when(packageService).deletePackage(anyString());

        RestAssuredMockMvc.webAppContextSetup(context);
    }
}
