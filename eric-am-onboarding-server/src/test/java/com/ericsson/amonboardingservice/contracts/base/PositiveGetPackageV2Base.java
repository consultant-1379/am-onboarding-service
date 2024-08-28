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

import com.ericsson.amonboardingservice.model.AppPackageResponseV2;
import com.ericsson.amonboardingservice.model.OperationDetailResponse;
import com.ericsson.amonboardingservice.presentation.controllers.PackageControllerV2Impl;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageServiceV2;
import com.ericsson.amonboardingservice.presentation.services.supportedoperationservice.SupportedOperationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.ericsson.amonboardingservice.utils.JsonUtils.readJsonFromResource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class PositiveGetPackageV2Base {

    @Mock
    PackageServiceV2 packageService;

    @InjectMocks
    PackageControllerV2Impl packageControllerV2;

    @Mock
    SupportedOperationService supportedOperationService;

    @BeforeEach
    public void setup() {
        given(packageService.getPackageV2(any())).willAnswer(invocation -> {
            String jsonString = readJsonFromResource("contracts/api/packages/positive/getPackageV2/packageDetails.json");
            ObjectMapper mapper = new ObjectMapper();
            return Optional.of(mapper.<AppPackageResponseV2>readValue(jsonString, new TypeReference<>(){}));
        });
        OperationDetailResponse operationDetailResponse = setUpOperationDetail();
        given(supportedOperationService.getSupportedOperations(anyString())).willReturn(List.of(operationDetailResponse));
        RestAssuredMockMvc.standaloneSetup(packageControllerV2);
    }

    private OperationDetailResponse setUpOperationDetail() {
        OperationDetailResponse operationDetailResponse = new OperationDetailResponse();
        operationDetailResponse.setOperationName("instantiate");
        operationDetailResponse.setSupported(true);
        operationDetailResponse.setError(null);
        return operationDetailResponse;
    }
}
