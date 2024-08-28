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
import static org.mockito.BDDMockito.given;
import static com.ericsson.amonboardingservice.utils.JsonUtils.readJsonFromResource;

import com.ericsson.amonboardingservice.model.AppPackageResponse;
import com.ericsson.amonboardingservice.model.OperationDetailResponse;
import com.ericsson.amonboardingservice.presentation.controllers.PackageControllerImpl;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.ericsson.amonboardingservice.presentation.services.supportedoperationservice.SupportedOperationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PositiveGetAllPackagesBase {

    @Mock
    private PackageService packageService;

    @InjectMocks
    private PackageControllerImpl packageController;

    @Mock
    private SupportedOperationService supportedOperationService;

    @BeforeEach
    public void setup() {
        given(packageService.listPackages(any(Pageable.class))).willAnswer(invocation -> {
            String jsonString = readJsonFromResource("contracts/api/packages/positive/getAllPackages/allPackageDetails.json");
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonString, new TypeReference<List<AppPackageResponse>>(){});
        });
        given(packageService.listPackages(anyString(), any(Pageable.class))).willAnswer(invocation -> {
            String jsonString = readJsonFromResource("contracts/api/packages/positive/getAllPackages/allPackageDetails.json");
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonString, new TypeReference<List<AppPackageResponse>>(){});
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
