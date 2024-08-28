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
package com.ericsson.amonboardingservice.presentation.services.packageservice;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import static com.ericsson.amonboardingservice.presentation.models.ChartUrlsEntity.ChartTypeEnum.CNF;
import static com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage.AppUsageStateEnum.NOT_IN_USE;
import static com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage.OperationalStateEnum.DISABLED;

import java.util.ArrayList;
import java.util.List;

import com.ericsson.amonboardingservice.presentation.services.mapper.AppPackageInstanceMapper;
import com.ericsson.amonboardingservice.presentation.services.mapper.AppPackageInstanceMapperImpl;
import com.ericsson.amonboardingservice.presentation.services.mapper.AppPackageMapper;
import com.ericsson.amonboardingservice.presentation.services.mapper.AppPackageMapperImpl;
import com.ericsson.amonboardingservice.presentation.services.mapper.DockerImageMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ericsson.am.shared.vnfd.model.VnfDescriptorDetails;
import com.ericsson.amonboardingservice.model.AppUsageStateRequest;
import com.ericsson.amonboardingservice.presentation.models.ChartUrlsEntity;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackageInstance;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {
        DockerImageMapperImpl.class,
        AppPackageMapperImpl.class,
        AppPackageInstanceMapperImpl.class
})
public class AppPackageMappingServiceTest {

    @Autowired
    private AppPackageMapper appPackageMapper;

    @Autowired
    private AppPackageInstanceMapper appPackageInstanceMapper;

    @Test
    public void testCreatePackageInstanceDetails() {
        AppUsageStateRequest appUsageStateRequest = new AppUsageStateRequest();
        appUsageStateRequest.setVnfId("any");

        AppPackageInstance instance = appPackageInstanceMapper.toAppPackageInstance(appUsageStateRequest);

        assertThat(instance.getInstanceId()).isEqualTo("any");
        assertThat(instance.getAppPackage()).isNull();
    }

    @Test
    public void testCreatePackageDetails() {
        VnfDescriptorDetails vnfDescriptorDetails = new VnfDescriptorDetails();
        vnfDescriptorDetails.setVnfDescriptorId("descriptor");
        vnfDescriptorDetails.setVnfDescriptorVersion("descriptor-version");
        vnfDescriptorDetails.setVnfProvider("provider");
        vnfDescriptorDetails.setVnfProductName("product");
        vnfDescriptorDetails.setVnfSoftwareVersion("software");
        vnfDescriptorDetails.setDescriptorModel("model");

        List<ChartUrlsEntity> chartUri = new ArrayList<>();
        chartUri.add(getChartUrlsEntity());

        AppPackage appPackage = appPackageMapper.createPackageDetails(vnfDescriptorDetails, chartUri);
        assertThat(appPackage).hasFieldOrPropertyWithValue("descriptorId", "descriptor");
        assertThat(appPackage).hasFieldOrPropertyWithValue("descriptorVersion", "descriptor-version");
        assertThat(appPackage).hasFieldOrPropertyWithValue("descriptorModel", "model");
        assertThat(appPackage).hasFieldOrPropertyWithValue("provider", "provider");
        assertThat(appPackage).hasFieldOrPropertyWithValue("productName", "product");
        assertThat(appPackage).hasFieldOrPropertyWithValue("softwareVersion", "software");
        assertThat(appPackage).hasFieldOrPropertyWithValue("usageState", NOT_IN_USE);
        assertThat(appPackage).hasFieldOrPropertyWithValue("operationalState", DISABLED);
        assertThat(appPackage.getChartsRegistryUrl().get(0).getChartArtifactKey()).isEqualTo(getChartUrlsEntity().getChartArtifactKey());
        assertThat(appPackage.getChartsRegistryUrl().get(0).getChartsRegistryUrl()).isEqualTo(getChartUrlsEntity().getChartsRegistryUrl());
        assertThat(appPackage.getChartsRegistryUrl().get(0).getChartName()).isEqualTo(getChartUrlsEntity().getChartName());
        assertThat(appPackage.getChartsRegistryUrl().get(0).getChartVersion()).isEqualTo(getChartUrlsEntity().getChartVersion());
        assertThat(appPackage.getChartsRegistryUrl().get(0).getChartType()).isEqualTo(getChartUrlsEntity().getChartType());
    }

    private AppPackage createPackage() {
        AppPackage appPackage = new AppPackage();
        appPackage.setDescriptorId("descriptor");
        appPackage.setDescriptorVersion("descriptor-version");
        appPackage.setProvider("provider");
        appPackage.setProductName("product");
        appPackage.setSoftwareVersion("software");
        appPackage.setDescriptorModel("model");

        return appPackage;
    }

    private static ChartUrlsEntity getChartUrlsEntity() {
        ChartUrlsEntity chartUrlsEntity = new ChartUrlsEntity();
        chartUrlsEntity.setPriority(1);
        chartUrlsEntity.setChartsRegistryUrl(
                "http://eric-lcm-helm-chart-registry.process-engine-4-eric-am-onboarding-service:8080/onboarded/charts/sampledescriptor-0.0.1-223"
                        + ".tgz");
        chartUrlsEntity.setChartName("sampledescriptor");
        chartUrlsEntity.setChartVersion("0.0.1-223");
        chartUrlsEntity.setChartType(CNF);
        chartUrlsEntity.setChartArtifactKey("helm_package");
        return chartUrlsEntity;
    }
}