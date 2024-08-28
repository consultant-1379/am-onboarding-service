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

import static org.mockito.BDDMockito.given;

import static com.ericsson.amonboardingservice.utils.JsonUtils.readJsonFromResource;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.ericsson.amonboardingservice.model.OperationDetailResponse;
import com.ericsson.amonboardingservice.presentation.controllers.PackageControllerImpl;
import com.ericsson.amonboardingservice.presentation.services.supportedoperationservice.SupportedOperationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PositiveSupportedOperationBase {

    @InjectMocks
    PackageControllerImpl packageController;
    @Mock
    private SupportedOperationService supportedOperationService;

    @BeforeEach
    public void setup() throws IOException {
        String jsonString = readJsonFromResource("contracts/api/packages/positive/supportedOperation/supportedOperations.json");
        String jsonWithErrorsString = readJsonFromResource(
                "contracts/api/packages/positive/supportedOperation/supportedOperationsFailedRollbackAndScale.json");
        ObjectMapper mapper = new ObjectMapper();
        List<OperationDetailResponse> operationDetailResponseList = mapper.readValue(jsonString, new TypeReference<>() {
        });
        List<OperationDetailResponse> operationDetailWithErrorsList = mapper.readValue(jsonWithErrorsString, new TypeReference<>() {
        });

        given(supportedOperationService.getSupportedOperations("spider-app-multi-v2-2cb5")).willReturn(operationDetailResponseList);
        given(supportedOperationService.getSupportedOperations("spider-app-multi-v2-2cb5-failed")).willReturn(operationDetailWithErrorsList);
        RestAssuredMockMvc.standaloneSetup(packageController);
    }
}
