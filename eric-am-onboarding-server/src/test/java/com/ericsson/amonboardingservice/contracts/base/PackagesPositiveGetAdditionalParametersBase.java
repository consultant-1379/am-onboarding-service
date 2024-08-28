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

import static org.mockito.Mockito.when;

import static com.ericsson.amonboardingservice.utils.JsonUtils.readJsonFromResource;

import java.io.IOException;
import java.util.List;

import com.ericsson.amonboardingservice.model.AdditionalPropertyResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.ericsson.am.shared.vnfd.service.exception.ToscaoException;
import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.presentation.controllers.PackageControllerImpl;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageService;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class PackagesPositiveGetAdditionalParametersBase extends AbstractDbSetupTest {

    @Mock
    private PackageService packageService;

    @InjectMocks
    private PackageControllerImpl packageController;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setup() throws IOException, ToscaoException {
        ObjectMapper mapper = new ObjectMapper();
        List<AdditionalPropertyResponse> additionalParamsForInstantiate = mapper.readValue(
                readJsonFromResource("contracts/api/packages/positiveGetAdditionalParameters/additionalAttributesForInstantiateVnfdVersion1dot2"
                        + ".json"),
                new TypeReference<>() {
                });
        List<AdditionalPropertyResponse> additionalParamsForRollback = mapper.readValue(
                readJsonFromResource("contracts/api/packages/positiveGetAdditionalParameters/additionalAttributesForRollbackVnfdVersion1dot2.json"),
                new TypeReference<>() {
                });
        List<AdditionalPropertyResponse> additionalParamsForRollbackWithWildCat = mapper.readValue(
                readJsonFromResource("contracts/api/packages/positiveGetAdditionalParameters/additionalAttributesForRollbackWithWildCatVnfdVersion1dot2.json"),
                new TypeReference<>() {
                });
        when(packageService.getAdditionalParamsForOperationType("spider-app-multi-v2-2cb5", "instantiate", "")).thenReturn(
                additionalParamsForInstantiate);
        when(packageService.getAdditionalParamsForOperationType("57c24601-e70d-418d-8477-64fb58a7563c",
                "rollback",
                "multi-chart-477c-aab3-2b04e6a383")).thenReturn(additionalParamsForRollback);
        when(packageService.getAdditionalParamsForOperationType("57c24601-e70d-418d-8477-64fb58a7563c",
                "rollback",
                "multi-chart-477c-aab3-2b04e6a381")).thenReturn(additionalParamsForRollbackWithWildCat);
        RestAssuredMockMvc.standaloneSetup(packageController);
    }
}
