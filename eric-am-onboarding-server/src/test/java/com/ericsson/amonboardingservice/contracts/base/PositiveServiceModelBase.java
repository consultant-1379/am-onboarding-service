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

import com.ericsson.amonboardingservice.model.ServiceModelRecordResponse;
import com.ericsson.amonboardingservice.presentation.controllers.PackageControllerImpl;
import com.ericsson.amonboardingservice.presentation.services.servicemodelservice.ServiceModelService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;

import static com.ericsson.amonboardingservice.utils.JsonUtils.readJsonFromResource;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class PositiveServiceModelBase {

    @InjectMocks
    PackageControllerImpl packageController;
    @Mock
    private ServiceModelService serviceModelService;

    @BeforeEach
    public void setup() throws IOException {
        String jsonString = readJsonFromResource("contracts/api/packages/positive/serviceModel/serviceModelResponse.json");
        ObjectMapper mapper = new ObjectMapper();
        ServiceModelRecordResponse serviceModel = mapper.readValue(jsonString, ServiceModelRecordResponse.class);

        given(serviceModelService.getServiceModelResponseByPackageId("b3def1ce-4cf4-477c-aab3-21cb04e6a379")).willReturn(Optional.of(serviceModel));
        RestAssuredMockMvc.standaloneSetup(packageController);
    }
}
