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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.model.AppUsageStateRequest;
import com.ericsson.amonboardingservice.presentation.controllers.VnfPackageControllerImpl;
import com.ericsson.amonboardingservice.presentation.services.packageinstanceservice.AppPackageInstanceService;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ExtendWith(SpringExtension.class)
public class PositiveUpdateUsageStateBase extends AbstractDbSetupTest {

    @Mock
    private AppPackageInstanceService packageInstanceService;

    @InjectMocks
    private VnfPackageControllerImpl vnfPackageController;

    @BeforeEach
    public void setup() {
        doNothing().when(packageInstanceService).updatePackageUsageState(anyString(), any(AppUsageStateRequest.class));
        RestAssuredMockMvc.standaloneSetup(vnfPackageController);
    }
}
