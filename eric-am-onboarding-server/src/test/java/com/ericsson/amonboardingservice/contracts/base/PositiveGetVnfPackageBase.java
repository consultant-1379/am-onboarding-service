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

import com.ericsson.amonboardingservice.model.VnfPkgInfo;
import com.ericsson.amonboardingservice.presentation.controllers.VnfPackageControllerImpl;
import com.ericsson.amonboardingservice.presentation.services.vnfpackageservice.VnfPackageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.ericsson.amonboardingservice.utils.JsonUtils.readJsonFromResource;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class PositiveGetVnfPackageBase {


    @Mock
    VnfPackageService vnfPackageService;

    @InjectMocks
    VnfPackageControllerImpl vnfPackageController;

    @BeforeEach
    public void setup() {
        given(vnfPackageService.getVnfPackage(anyString())).willAnswer(invocationOnMock -> {
            Object[] args = invocationOnMock.getArguments();
            String param = (String) args[0];
            String responseTemplate = readJsonFromResource("contracts/api/vnf_packages/positive/getVnfPackage/vnfpackage.json");
            ObjectMapper mapper = new ObjectMapper();
            return Optional.of(mapper.readValue(responseTemplate.replaceFirst("<APP_PKG_ID>", param), VnfPkgInfo.class));
        });
        RestAssuredMockMvc.standaloneSetup(vnfPackageController);
    }



}
