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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import static com.ericsson.amonboardingservice.utils.JsonUtils.readJsonFromResource;

import java.util.List;

import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageResponseVerbosity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.ericsson.amonboardingservice.model.AppPackageResponseV2;
import com.ericsson.amonboardingservice.model.OperationDetailResponse;
import com.ericsson.amonboardingservice.presentation.controllers.PackageControllerV2Impl;
import com.ericsson.amonboardingservice.presentation.services.supportedoperationservice.SupportedOperationService;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageServiceV2;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PositiveGetAllPackagesV2Base {
    @Mock
    PackageServiceV2 packageService;

    @InjectMocks
    PackageControllerV2Impl packageController;

    @Mock
    SupportedOperationService supportedOperationService;

    @BeforeEach
    public void setup() {
        Pageable pageable = Pageable.ofSize(100);
        given(packageService.listPackagesV2(pageable)).willAnswer(invocation -> {
            String jsonString = readJsonFromResource("contracts/api/packages/positive/getAllPackagesV2/allPackageDetails.json");
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonString, new TypeReference<List<AppPackageResponseV2>>() {
            });
        });
        given(packageService.listPackagesV2(anyString(), eq(pageable))).willAnswer(invocation -> {
            String jsonString = readJsonFromResource("contracts/api/packages/positive/getAllPackagesV2/allPackageDetails.json");
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonString, new TypeReference<List<AppPackageResponseV2>>() {
            });
        });
        given(packageService.listPackagesV2(any(PackageResponseVerbosity.class), eq(pageable))).willAnswer(invocation -> {
            String jsonString = readJsonFromResource("contracts/api/packages/positive/getAllPackagesV2/allPackageDetailsUI.json");
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonString, new TypeReference<List<AppPackageResponseV2>>() {
            });
        });
        given(packageService.listPackagesV2(anyString(), any(PackageResponseVerbosity.class), eq(pageable))).willAnswer(invocation -> {
            String jsonString = readJsonFromResource("contracts/api/packages/positive/getAllPackagesV2/allPackageDetailsUI.json");
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonString, new TypeReference<List<AppPackageResponseV2>>() {
            });
        });
        OperationDetailResponse operationDetailResponse = setUpOperationDetail();
        given(supportedOperationService.getSupportedOperations(anyString())).willReturn(List.of(operationDetailResponse));
        RestAssuredMockMvc.standaloneSetup(packageController);
    }

    private OperationDetailResponse setUpOperationDetail() {
        OperationDetailResponse operationDetailResponse = new OperationDetailResponse();
        operationDetailResponse.setOperationName("instantiate");
        operationDetailResponse.setSupported(true);
        operationDetailResponse.setError(null);
        return operationDetailResponse;
    }
}
