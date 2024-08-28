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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import static com.ericsson.amonboardingservice.utils.JsonUtils.readJsonFromResource;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.ericsson.amonboardingservice.model.VnfPkgInfo;
import com.ericsson.amonboardingservice.model.VnfPkgInfoV2;
import com.ericsson.amonboardingservice.presentation.controllers.VnfPackageControllerV2Impl;
import com.ericsson.amonboardingservice.presentation.services.vnfpackageservice.VnfPackageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PositiveGetAllVnfPackagesV2Base {

    @Mock
    VnfPackageService vnfPackageService;

    @InjectMocks
    VnfPackageControllerV2Impl vnfPackageControllerV2;

    @BeforeEach
    public void setup() {
        given(vnfPackageService.listVnfPackagesV2(any())).willAnswer(invocationOnMock -> {
            String responseTemplate = readJsonFromResource("contracts/api/vnf_packages/positive/getAllVnfPackagesV2/vnfpackagesWithErrorDetails"
                                                                   + ".json");
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(responseTemplate, new TypeReference<List<VnfPkgInfoV2>>() {
            });
        });

        given(vnfPackageService.listVnfPackagesV2(eq("(eq,vnfdId,3d02c5c9-7a9b-48da-8ceb-46fcc83f584c)"), any())).willAnswer(invocationOnMock -> {
            String responseTemplate = readJsonFromResource(
                    "contracts/api/vnf_packages/positive/getAllVnfPackagesV2/filteredVnfPackageV2ForDowngrade1.json");
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(responseTemplate, new TypeReference<List<VnfPkgInfoV2>>() {
            });
        });

        given(vnfPackageService.listVnfPackagesV2(eq("(eq,vnfdId,3d02c5c9-7a9b-48da-8ceb-46fcc83f694c)"), any())).willAnswer(invocationOnMock -> {
            String responseTemplate = readJsonFromResource(
                    "contracts/api/vnf_packages/positive/getAllVnfPackagesV2/filteredVnfPackageV2ForDowngrade2.json");
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(responseTemplate, new TypeReference<List<VnfPkgInfoV2>>() {
            });
        });

        given(vnfPackageService.listVnfPackagesV2(eq("(eq,vnfdId,3d02c5c9-7a9b-48da-8ceb-46fcc83f4321)"), any())).willAnswer(invocationOnMock -> {
            String responseTemplate = readJsonFromResource(
                    "contracts/api/vnf_packages/positive/getAllVnfPackagesV2/filteredVnfPackageV2ForDowngrade3.json");
            ObjectMapper mapper = new ObjectMapper();
            return mapper.<List<VnfPkgInfo>>readValue(responseTemplate, new TypeReference<List<VnfPkgInfo>>() {
            });
        });

        given(vnfPackageService.listVnfPackagesV2(eq("(eq,vnfdId,b1bb0ce7-ebca-4fa7-95ed-4840d70a1177)"), any())).willAnswer(invocationOnMock -> {
            String responseTemplate = readJsonFromResource(
                    "contracts/api/vnf_packages/positive/getAllVnfPackagesV2/filteredVnfPackageV2ForDowngrade4.json");
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(responseTemplate, new TypeReference<List<VnfPkgInfoV2>>() {
            });
        });

        given(vnfPackageService.listVnfPackagesV2(eq("(eq,vnfdId,d3def1ce-4cf4-477c-aab3-123456789101)"), any())).willAnswer(invocationOnMock -> {
            String responseTemplate = readJsonFromResource(
                    "contracts/api/vnf_packages/positive/getAllVnfPackagesV2/filteredVnfPackagesV2WithCrdCharts.json");
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(responseTemplate, new TypeReference<List<VnfPkgInfoV2>>() {
            });
        });

        given(vnfPackageService.listVnfPackagesV2(eq("(eq,vnfdId,a3f91625-c12c-4897-854e-3bd49b84263b)"), any())).willAnswer(invocationOnMock -> {
            String responseTemplate = readJsonFromResource(
                    "contracts/api/vnf_packages/positive/getAllVnfPackagesV2/filteredVnfPackagesV2WithCrdChartsForUpgrade.json");
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(responseTemplate, new TypeReference<List<VnfPkgInfoV2>>() {
            });
        });

        RestAssuredMockMvc.standaloneSetup(vnfPackageControllerV2);
    }
}
