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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.ApplicationServer;
import com.ericsson.amonboardingservice.presentation.controllers.VnfPackageControllerImpl;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.WebApplicationContext;

import jakarta.inject.Inject;

@SpringBootTest(classes = ApplicationServer.class)
public class NegativeUpdateUsageStateBase extends AbstractDbSetupTest {

    @Inject
    private WebApplicationContext context;

    @SpyBean
    private VnfPackageControllerImpl vnfPackageController;

    @BeforeEach
    public void setup() {
        doReturn(new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE)).when(vnfPackageController)
                .updatePackagesUsageState(eq("939986865745351198"), any());
        doReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR)).when(vnfPackageController)
                .updatePackagesUsageState(eq("failed-vnf-package-id"), any());
        RestAssuredMockMvc.webAppContextSetup(context);
    }
}
