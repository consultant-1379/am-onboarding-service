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
import com.ericsson.amonboardingservice.presentation.controllers.VnfPackageControllerImpl;
import com.ericsson.amonboardingservice.presentation.services.vnfpackageservice.VnfPackageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PositiveGetAllVnfPackagesFilterBase {
    @Mock
    VnfPackageService vnfPackageService;

    @InjectMocks
    VnfPackageControllerImpl vnfPackageController;

    @BeforeEach
    public void setup() {
        mockGetPackageFilterRequest("(eq,id,d3def1ce-4cf4-477c-aab3-21cb04e6a379)");
        mockGetPackageFilterRequest("(eq,id,no-scaling)");
        mockGetPackageFilterRequest("(eq,id,single-helm-chart)");
        mockGetPackageFilterRequest("(eq,id,single-helm-chart-2)");
        mockGetPackageFilterRequest("(eq,id,UPDATED-SCALING)");
        mockGetPackageFilterRequest("(eq,id,levels-no-vdu)");
        mockGetPackageFilterRequest("(eq,id,single-helm-rollback-2)");
        mockGetPackageFilterRequest("(eq,vnfdId,multi-helm-rollback-3)");
        mockGetPackageFilterRequest("(eq,id,upgrade-no-vdu-levels)");
        mockGetPackageFilterRequest("(eq,vnfdId,multi-helm-rollback)");
        mockGetPackageFilterRequest("(eq,vnfdId,multi-helm-rollback-2)");
        mockGetPackageFilterRequest("(eq,id,multi-helm-rollback-3)");
        mockGetPackageFilterRequest("(eq,packageSecurityOption,UNSIGNED)");
        mockGetPackageFilterRequest("(eq,id,levels-no-vdu-no-scaling-mapping)");
        mockGetPackageFilterRequest("(eq,id,levels-no-extensions-no-scaling-mapping)");
        mockGetPackageFilterRequest("(eq,id,scale-non-scalable-chart)");
        mockGetPackageFilterRequest("(eq,vnfdId,multi-helm-chart-disabled)");
        mockGetPackageFilterRequest("(eq,vnfdId,single-helm-chart-for-release-naming-instantiate)");
        mockGetPackageFilterRequest("(eq,vnfdId,multi-helm-chart-for-release-naming-instantiate)");
        mockGetPackageFilterRequest("(eq,vnfdId,multi-helm-chart-for-release-naming-upgrade-1)");
        mockGetPackageFilterRequest("(eq,vnfdId,multi-helm-chart-for-release-naming-upgrade-2)");
        mockGetPackageFilterRequest("(eq,vnfdId,multi-rollback-4cf4-477c-aab3-21cb04e6b)");
        mockGetPackageFilterRequest("(eq,vnfdId,4c096964-69e7-11ee-8c99-0242ac120002)");

        RestAssuredMockMvc.standaloneSetup(vnfPackageController);
    }

    private void mockGetPackageFilterRequest(final String filter) {
        given(vnfPackageService.listVnfPackages(eq(filter), any())).willAnswer(invocationOnMock -> {
            String responseTemplate;
            if (filter.equals("(eq,vnfdId,multi-helm-rollback)") || filter.equals("(eq,vnfdId,multi-helm-rollback-2)")) {
                responseTemplate = readJsonFromResource("contracts/api/vnf_packages/positive/getAllVnfPackagesFilter/filteredvnfpackagesMulti.json");
            } else if (filter.equals("(eq,id,multi-helm-rollback-3)") || filter.equals("(eq,vnfdId,multi-helm-rollback-3)")) {
                responseTemplate = readJsonFromResource("contracts/api/vnf_packages/positive/getAllVnfPackagesFilter/filteredVnfPackagesWithMultipleHelmChartRollback.json");
            } else if (filter.equals("(eq,id,levels-no-vdu-no-scaling-mapping)")) {
                responseTemplate = readJsonFromResource("contracts/api/vnf_packages/positive/getAllVnfPackagesFilter/filteredVnfPackageWithLevelsNoVDULevelsNoScalingMapping.json");
            } else if (filter.equals("(eq,id,levels-no-extensions-no-scaling-mapping)")) {
                responseTemplate = readJsonFromResource("contracts/api/vnf_packages/positive/getAllVnfPackagesFilter/filteredVnfPackageWithLevelsNoExtensionsNoScalingMapping.json");
            } else if (filter.equals("(eq,vnfdId,multi-helm-chart-disabled)")) {
                responseTemplate = readJsonFromResource("contracts/api/vnf_packages/positive/getAllVnfPackagesFilter/filteredVnfPackagesWithDisabledState.json");
            } else if (filter.equals("(eq,vnfdId,single-helm-chart-for-release-naming-instantiate)")) {
                responseTemplate = readJsonFromResource(
                        "contracts/api/vnf_packages/positive/getAllVnfPackagesFilter/filteredVnfPackageForReleaseNamingSingleInstantiate.json");
            } else if (filter.equals("(eq,vnfdId,multi-helm-chart-for-release-naming-instantiate)")) {
                responseTemplate = readJsonFromResource(
                        "contracts/api/vnf_packages/positive/getAllVnfPackagesFilter/filteredVnfPackageForReleaseNamingMultiInstantiate.json");
            } else if (filter.equals("(eq,vnfdId,multi-helm-chart-for-release-naming-upgrade-1)")) {
                responseTemplate = readJsonFromResource(
                        "contracts/api/vnf_packages/positive/getAllVnfPackagesFilter/filteredVnfPackageForReleaseNamingMultiUpgrade1.json");
            } else if (filter.equals("(eq,vnfdId,multi-helm-chart-for-release-naming-upgrade-2)")) {
                responseTemplate = readJsonFromResource(
                        "contracts/api/vnf_packages/positive/getAllVnfPackagesFilter/filteredVnfPackageForReleaseNamingMultiUpgrade2.json");
            } else if (filter.equals("(eq,vnfdId,multi-rollback-4cf4-477c-aab3-21cb04e6b)")) {
                responseTemplate = readJsonFromResource(
                        "contracts/api/vnf_packages/positive/getAllVnfPackagesFilter/filteredVnfPackagesWithRollback.json");
            } else if (filter.equals("(eq,vnfdId,4c096964-69e7-11ee-8c99-0242ac120002)")) {
                responseTemplate = readJsonFromResource(
                        "contracts/api/vnf_packages/positive/getAllVnfPackagesFilter/filteredVnfPackageWithUpgradePattern.json");
            } else {
                responseTemplate = readJsonFromResource("contracts/api/vnf_packages/positive/getAllVnfPackagesFilter/filteredvnfpackages.json");
            }

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(responseTemplate, new TypeReference<List<VnfPkgInfo>>(){});
        });
    }
}

