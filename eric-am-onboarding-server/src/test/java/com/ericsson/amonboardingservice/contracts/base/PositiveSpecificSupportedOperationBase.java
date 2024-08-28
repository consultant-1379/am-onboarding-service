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

import com.ericsson.amonboardingservice.model.OperationDetailResponse;
import com.ericsson.amonboardingservice.presentation.controllers.PackageControllerImpl;
import com.ericsson.amonboardingservice.presentation.services.supportedoperationservice.SupportedOperationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static com.ericsson.amonboardingservice.utils.JsonUtils.readJsonFromResource;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class PositiveSpecificSupportedOperationBase {

    @InjectMocks
    PackageControllerImpl packageController;
    @Mock
    private SupportedOperationService supportedOperationService;

    @BeforeEach
    public void setup() throws IOException {
        String jsonString = readJsonFromResource("contracts/api/packages/positive/specificSupportedOperation/supportedOperations.json");
        ObjectMapper mapper = new ObjectMapper();
        List<OperationDetailResponse> operationDetailResponseList = mapper.readValue(jsonString, new TypeReference<>() {
        });

        given(supportedOperationService.getSupportedOperations("d3def1ce-4cf4-477c-aab3-21cb04e6a379")).willReturn(operationDetailResponseList);
        RestAssuredMockMvc.standaloneSetup(packageController);
    }
}
