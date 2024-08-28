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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.ericsson.amonboardingservice.presentation.controllers.VnfPackageControllerImpl;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageService;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PositiveGetHelmfilesBase {

    @Mock
    private PackageService packageService;

    @InjectMocks
    private VnfPackageControllerImpl vnfPackageController;

    @BeforeEach
    public void setup() {
        given(packageService.getHelmfileContentByPackageId(anyString())).willReturn(getContent());
        RestAssuredMockMvc.standaloneSetup(vnfPackageController);
    }

    private byte[] getContent() {
        return new byte[]{1, 2, 3};
    }

}
