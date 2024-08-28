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

import com.ericsson.am.shared.vnfd.model.servicemodel.ServiceModel;
import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.TestUtils;
import com.ericsson.amonboardingservice.presentation.models.ChartUrlsEntity;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.repositories.ChartUrlsRepository;
import com.ericsson.amonboardingservice.presentation.services.ToscaHelper;
import com.ericsson.amonboardingservice.presentation.services.dockerservice.DockerService;
import com.ericsson.amonboardingservice.presentation.services.helmservice.HelmChartStatus;
import com.ericsson.amonboardingservice.presentation.services.helmservice.HelmService;
import com.ericsson.amonboardingservice.presentation.services.onboarding.request.PackageUploadRequestContext;
import com.ericsson.amonboardingservice.presentation.services.supportedoperationservice.SupportedOperationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static com.ericsson.amonboardingservice.presentation.models.ChartUrlsEntity.ChartTypeEnum.CNF;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
        properties = {
                "onboarding.skipToscaoValidation = true",
                "url="
        })
public class CleanUpOnFailureServiceTest extends AbstractDbSetupTest {

    @Autowired
    private CleanUpOnFailureService failureService;

    @Autowired
    private AppPackageRepository appPackageRepository;

    @MockBean
    private ChartUrlsRepository chartUrlsRepository;

    @MockBean
    private SupportedOperationService supportedOperationService;

    @MockBean
    private HelmService helmService;

    @MockBean
    private DockerService dockerService;

    @MockBean
    private ToscaHelper toscaHelper;

    @Test
    @Transactional
    public void cleanUpOnFailureTest(@TempDir Path tempFolder) throws IOException {
        AppPackage appPackage = appPackageRepository.save(getAppPackage());
        when(helmService.checkChartPresent(any(Path.class))).thenReturn(HelmChartStatus.NOT_PRESENT);

        failureService.cleanUpOnFailure(appPackage.getPackageId(), getContext(tempFolder));

        verify(helmService).checkChartPresent(any(Path.class));
        verify(dockerService).removeDockerImagesByPackageId(eq(appPackage.getPackageId()));
        verify(chartUrlsRepository).deleteByAppPackage(eq(appPackage));
        verify(toscaHelper).optionallyDeleteServiceModel(anyString());
        verify(supportedOperationService).deleteSupportedOperations(anyString());
    }

    private byte[]getHelmfileContent() throws URISyntaxException, IOException {
        Path helmfile = TestUtils.getResource("helmfile-b.tgz");
        return Files.readAllBytes(helmfile);
    }

    private static AppPackage getAppPackage() {
        AppPackage appPackage = new AppPackage();
        appPackage.setOnboardingState(AppPackage.OnboardingStateEnum.CREATED);
        ChartUrlsEntity chartUrlsEntity = getChartUrlsEntity(appPackage);
        List<ChartUrlsEntity> chartUrlsEntities = new ArrayList<>();
        chartUrlsEntities.add(chartUrlsEntity);
        appPackage.setChartsRegistryUrl(chartUrlsEntities);
        return appPackage;
    }

    private static PackageUploadRequestContext getContext(@TempDir Path tempFolder) throws IOException {
        ServiceModel model = new ServiceModel();
        model.setId("model-id");
        Path chartPath = Files.createFile(tempFolder.resolve("existent-chart-1.tgz"));
        Map<Path, HelmChartStatus> statusMap = Map.of(chartPath, HelmChartStatus.NOT_PRESENT);
        PackageUploadRequestContext context = new PackageUploadRequestContext();
        context.setHelmChartStatus(statusMap);
        context.setServiceModel(model);
        return context;
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


