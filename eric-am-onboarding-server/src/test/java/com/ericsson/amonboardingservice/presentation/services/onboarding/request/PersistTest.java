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
package com.ericsson.amonboardingservice.presentation.services.onboarding.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static com.ericsson.amonboardingservice.TestUtils.getResource;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_DEFINITIONS;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_HELM_DEFINITIONS;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_IMAGES;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_MANIFEST;
import static com.ericsson.amonboardingservice.utils.Constants.CSAR_DIRECTORY;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.am.shared.vnfd.model.VnfDescriptorDetails;
import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.presentation.exceptions.FailedOnboardingException;
import com.ericsson.amonboardingservice.presentation.models.Chart;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.models.vnfd.OperationDetailEntity;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageArtifactsRepository;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageDockerImageRepository;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.repositories.ChartRepository;
import com.ericsson.amonboardingservice.presentation.repositories.OperationDetailEntityRepository;
import com.ericsson.amonboardingservice.presentation.services.ToscaHelper;
import com.ericsson.amonboardingservice.presentation.services.supportedoperationservice.SupportedOperationService;
import com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaService;

@SpringBootTest
public class PersistTest extends AbstractDbSetupTest {

    private static final String DESCRIPTOR_ID = "descriptor-id";
    private static final String CHART_FILE_NAME = "chart-that-should-not-be-saved.tgz";

    @Autowired
    private Persist persist;

    @Autowired
    private AppPackageRepository appPackageRepository;

    @Autowired
    private AppPackageArtifactsRepository appPackageArtifactsRepository;

    @Autowired
    private AppPackageDockerImageRepository appPackageDockerImageRepository;

    @Autowired
    private OperationDetailEntityRepository operationDetailEntityRepository;

    @Autowired
    private ChartRepository chartRepository;

    @MockBean
    private ToscaMetaService toscaMetaService;

    @MockBean
    private SupportedOperationService supportedOperationService;

    @MockBean
    private ToscaHelper toscaHelper;

    private String packageId;
    @BeforeEach
    public void setUp() {
        packageId = appPackageRepository.save(new AppPackage()).getPackageId();
    }

    @AfterEach
    public void tearDown() {
        appPackageRepository.deleteByPackageId(packageId);
    }

    @Test
    public void shouldRollbackTransactionWhenExceptionOccurs() throws URISyntaxException {
        // given
        final Path csarDir = getResource("package-artifacts").toAbsolutePath();

        mockServices(csarDir);

        final PackageUploadRequestContext context = prepareContext(csarDir, packageId);

        // when
        assertThatThrownBy(() -> persist.handle(context)).isInstanceOf(FailedOnboardingException.class);

        // then
        final AppPackage updatedAppPackage = appPackageRepository.findByPackageId(packageId).get();

        assertThat(updatedAppPackage.getDescriptorId()).isNull();
        assertThat(updatedAppPackage.getServiceModelRecordEntity()).isNull();

        assertThat(chartRepository.findAll()).extracting(Chart::getHelmChart).doesNotContain(CHART_FILE_NAME);
        assertThat(appPackageArtifactsRepository.findByAppPackage(updatedAppPackage)).isEmpty();
        assertThat(appPackageDockerImageRepository.findByAppPackage(updatedAppPackage)).isEmpty();
        assertThat(operationDetailEntityRepository.findByAppPackagePackageId(packageId)).isEmpty();
    }

    private void mockServices(final Path csarDir) {
        when(toscaMetaService.getToscaMetaPath(any())).thenReturn(csarDir.resolve("TOSCA-Metadata/TOSCA.meta"));
        when(supportedOperationService.parsePackageSupportedOperations(any()))
            .thenAnswer(invocation -> List.of(createOperationDetail(invocation.getArgument(0))));
        when(toscaHelper.isTosca1Dot2(any())).thenReturn(false);
    }


    private static OperationDetailEntity createOperationDetail(final AppPackage appPackage) {
        final OperationDetailEntity operationDetail = new OperationDetailEntity();
        operationDetail.setOperationName("operation-name");
        operationDetail.setSupported(true);
        operationDetail.setAppPackage(appPackage);

        return operationDetail;
    }

    private static PackageUploadRequestContext prepareContext(final Path csarDir, final String packageId) {
        final PackageUploadRequestContext context = new PackageUploadRequestContext("filename.csar",
                                                                                    csarDir,
                                                                                    LocalDateTime.now().plusHours(3),
                                                                                    packageId);
        context.setHelmChartPaths(Set.of(Paths.get(CHART_FILE_NAME)));

        final VnfDescriptorDetails vnfd = new VnfDescriptorDetails();
        vnfd.setVnfDescriptorId(DESCRIPTOR_ID);
        context.setVnfd(vnfd);

        context.setArtifactPaths(prepareArtifactPaths(csarDir));
        context.setRenamedImageList(List.of("image-1", "image-2", "image-3"));

        return context;
    }

    private static Map<String, Path> prepareArtifactPaths(Path csarPath) {
        Map<String, Path> artifacts = new HashMap<>();
        artifacts.put(CSAR_DIRECTORY, csarPath);
        artifacts.put(ENTRY_DEFINITIONS, csarPath.resolve("Definitions/sampledescriptor-0.0.1-223.yaml"));
        artifacts.put(ENTRY_IMAGES, csarPath.resolve("Files/images/docker.tar"));
        artifacts.put(ENTRY_HELM_DEFINITIONS, csarPath.resolve("Definitions/OtherTemplates/"));
        artifacts.put(ENTRY_MANIFEST, csarPath.resolve("sampledescriptor-0.0.1-223.mf"));

        return artifacts;
    }
}
