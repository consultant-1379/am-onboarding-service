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

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.presentation.models.ChartUrlsEntity;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.services.dockerservice.DockerService;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.ericsson.amonboardingservice.presentation.services.helmservice.HelmService;
import com.ericsson.amonboardingservice.infrastructure.client.RestClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.ericsson.amonboardingservice.TestUtils.getResource;
import static com.ericsson.amonboardingservice.presentation.models.ChartUrlsEntity.ChartTypeEnum.CNF;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest()
@ActiveProfiles("test")
@TestPropertySource(
        properties = {
                "container.registry.enabled = false"
        })
public class PackageServiceNoRegistryTest extends AbstractDbSetupTest {

    @Autowired
    private PackageServiceImpl packageService;

    @MockBean
    private RestClient restClient;

    @MockBean
    private FileService fileService;

    @MockBean
    private HelmService helmService;

    @MockBean
    private DockerService dockerService;

    @MockBean
    private AppPackageRepository appPackageRepository;


    @Test
    @Transactional
    public void shouldDeleteExistingPackageWithRegistryDisabled() throws URISyntaxException {

        AppPackage appPackage = getAppPackage();
        String packageId = appPackage.getPackageId();

        when(appPackageRepository.findByPackageId(any())).thenReturn(Optional.of(appPackage));
        when(restClient.buildUrl(anyString(), anyString())).thenCallRealMethod();
        when(helmService.getChart(anyString(), anyString()))
                .thenReturn(Optional.of(getResource("spider-app-pm-only-2.74.7.tgz").toFile()));
        doNothing().when(fileService).deleteFile(any(File.class));

        packageService.deletePackage(packageId);
        packageService.removeAppPackageWithResources(packageId);

        verify(appPackageRepository, times(1)).deleteByPackageId(eq(packageId));
        verify(helmService, times(1)).getChart(anyString(), anyString());
        verify(helmService, times(1)).deleteChart(anyString(), anyString());
        verify(dockerService, times(0)).removeDockerImagesByPackageId(eq(packageId));
    }

    private static AppPackage getAppPackage() {
        AppPackage appPackage = new AppPackage();
        appPackage.setPackageId("dummy-id");
        appPackage.setOnboardingState(AppPackage.OnboardingStateEnum.CREATED);
        ChartUrlsEntity chartUrlsEntity = getChartUrlsEntity(appPackage);
        List<ChartUrlsEntity> chartUrlsEntities = new ArrayList<>();
        chartUrlsEntities.add(chartUrlsEntity);
        appPackage.setChartsRegistryUrl(chartUrlsEntities);
        return appPackage;
    }

    private static ChartUrlsEntity getChartUrlsEntity(final AppPackage appPackage) {
        ChartUrlsEntity chartUrlsEntity = new ChartUrlsEntity();
        chartUrlsEntity.setPriority(1);
        chartUrlsEntity.setChartsRegistryUrl(
                "http://eric-lcm-helm-chart-registry.process-engine-4-eric-am-onboarding-service:8080/onboarded/charts/sampledescriptor-0.0.1-223"
                        + ".tgz");
        chartUrlsEntity.setChartName("sampledescriptor");
        chartUrlsEntity.setChartVersion("0.0.1-223");
        chartUrlsEntity.setAppPackage(appPackage);
        chartUrlsEntity.setChartType(CNF);
        chartUrlsEntity.setChartArtifactKey("helm_package");
        return chartUrlsEntity;
    }
}
