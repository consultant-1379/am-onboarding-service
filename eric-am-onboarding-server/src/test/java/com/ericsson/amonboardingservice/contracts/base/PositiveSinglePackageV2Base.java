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

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.model.AppPackageResponseV2;
import com.ericsson.amonboardingservice.presentation.controllers.PackageControllerV2Impl;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageServiceV2;
import com.ericsson.amonboardingservice.presentation.services.supportedoperationservice.SupportedOperationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import static com.ericsson.amonboardingservice.utils.JsonUtils.readJsonFromResource;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class PositiveSinglePackageV2Base extends AbstractDbSetupTest {
    @Mock
    SupportedOperationService supportedOperationService;

    @Mock
    PackageServiceV2 packageService;

    @InjectMocks
    PackageControllerV2Impl packageControllerV2;

    @BeforeEach
    public void setup() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Optional<AppPackageResponseV2> packageDetailsSpecific1 = Optional.of(mapper.readValue(
                readJsonFromResource("contracts/api/packages/positive/singlePackageV2/packageDetailsSpecific1.json"),
                AppPackageResponseV2.class));
        Optional<AppPackageResponseV2> packageDetailsSpecific2 = Optional.of(mapper.readValue(
                readJsonFromResource("contracts/api/packages/positive/singlePackageV2/packageDetailsSpecific2.json"),
                AppPackageResponseV2.class));
        Optional<AppPackageResponseV2> packageDetailsSpecific3 = Optional.of(mapper.readValue(
                readJsonFromResource("contracts/api/packages/positive/singlePackageV2/packageDetailsSpecific3.json"),
                AppPackageResponseV2.class));
        Optional<AppPackageResponseV2> packageDetailsSpecific4 = Optional.of(mapper.readValue(
                readJsonFromResource("contracts/api/packages/positive/singlePackageV2/packageDetailsSpecific4.json"),
                AppPackageResponseV2.class));
        Optional<AppPackageResponseV2> packageDetailsSpecific5 = Optional.of(mapper.readValue(
                readJsonFromResource("contracts/api/packages/positive/singlePackageV2/packageDetailsSpecific5.json"),
                AppPackageResponseV2.class));
        Optional<AppPackageResponseV2> packageDetailsSpecific6 = Optional.of(mapper.readValue(
                readJsonFromResource("contracts/api/packages/positive/singlePackageV2/packageDetailsSpecific6.json"),
                AppPackageResponseV2.class));
        Optional<AppPackageResponseV2> packageDetailsSpecific7 = Optional.of(mapper.readValue(
                readJsonFromResource("contracts/api/packages/positive/singlePackageV2/packageDetailsSpecific7.json"),
                AppPackageResponseV2.class));
        when(packageService.getPackageV2(eq("d3def1ce-4cf4-477c-aab3-21cb04e6a381"))).thenReturn(packageDetailsSpecific1);
        when(packageService.getPackageV2(eq("d3def1ce-4cf4-477c-aab3-21cb04e6a380"))).thenReturn(packageDetailsSpecific2);
        when(packageService.getPackageV2(eq("d3def1ce-4cf4-477c-aab3-21cb04e6a382"))).thenReturn(packageDetailsSpecific3);
        when(packageService.getPackageV2(eq("d3def1ce-4cf4-477c-aab3-21cb04e6a383"))).thenReturn(packageDetailsSpecific4);
        when(packageService.getPackageV2(eq("d3def1ce-4cf4-477c-aab3-21cb04e6a385"))).thenReturn(packageDetailsSpecific5);
        when(packageService.getPackageV2(eq("d3def1ce-4cf4-477c-aab3-21cb04e6a386"))).thenReturn(packageDetailsSpecific6);
        when(packageService.getPackageV2(matches("(^(?!d3def1ce-4cf4-477c-aab3-21cb04e6a38)).*"))).thenAnswer(invocationOnMock -> {
            Object[] args = invocationOnMock.getArguments();
            String param = (String) args[0];
            final String genericResponseTemplate = readJsonFromResource("contracts/api/packages/positive/singlePackageV2/packageDetailsGeneric.json");
            return Optional.of(mapper.readValue(genericResponseTemplate.replaceAll("<APP_PKG_ID>", param), AppPackageResponseV2.class));
        });
        when(packageService.getPackageV2(eq("d2def1ce-4cdff1-477c-aab3-21cb04e6a3236"))).thenReturn(packageDetailsSpecific7);
        when(supportedOperationService.getSupportedOperations(anyString())).thenReturn(Collections.emptyList());
        RestAssuredMockMvc.standaloneSetup(packageControllerV2);
    }
}