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
package com.ericsson.amonboardingservice.presentation.services;

import com.ericsson.am.shared.vnfd.model.servicemodel.ServiceModel;
import com.ericsson.am.shared.vnfd.service.ToscaoServiceImpl;
import com.ericsson.am.shared.vnfd.service.exception.ToscaoException;
import com.ericsson.amonboardingservice.presentation.models.ServiceModelRecordEntity;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageArtifactsRepository;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.ericsson.amonboardingservice.presentation.services.onboarding.request.PackageUploadRequestContext;
import com.ericsson.amonboardingservice.presentation.services.packageservice.AppPackageValidator;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageDatabaseService;
import com.ericsson.amonboardingservice.presentation.services.vnfdservice.VnfdServiceImpl;
import com.ericsson.amonboardingservice.utils.Constants;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static com.ericsson.amonboardingservice.TestUtils.getResource;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_DEFINITIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
    ToscaHelper.class,
    VnfdServiceImpl.class })
@MockBean(classes = {
        AppPackageValidator.class,
        AppPackageArtifactsRepository.class,
        PackageDatabaseService.class
})
public class ToscaHelperTest {

    private static final String VALID_TOSCA_1_3_MULTI_CHART_VNFD = "vnfd/tosca_1_3/valid_tosca_1_3_multi_charts.yaml";
    private static final String EMPTY_VNFD = "vnfd/empty_vnfd.yaml";
    @TempDir
    public Path tempFolder;
    private Path emptyVnfdPath;
    private Path validTosca1dot3VnfdPath;
    @Autowired
    private VnfdServiceImpl vnfdService;
    @MockBean
    private ToscaoServiceImpl toscaoService;
    @MockBean
    private FileService fileService;
    @MockBean
    private AppPackageRepository appPackageRepository;
    @Autowired
    private ToscaHelper toscaHelper;
    @MockBean(name = "toscaRestTemplate")
    private RestTemplate toscaRestTemplate;
    @MockBean(name = "retryToscaTemplate")
    private RetryTemplate retryToscaTemplate;

    @BeforeEach
    public void setup() throws URISyntaxException, IOException {
        validTosca1dot3VnfdPath = getResource(VALID_TOSCA_1_3_MULTI_CHART_VNFD);
        emptyVnfdPath = getResource(EMPTY_VNFD);
    }

    @Test
    public void testDeleteServiceModelFromToscaoByAppPackage() {
        ServiceModelRecordEntity entity = new ServiceModelRecordEntity();
        entity.setServiceModelId("id");
        AppPackage packageSetForDeletion = new AppPackage();
        packageSetForDeletion.setPackageId("dummy-id");
        packageSetForDeletion.setServiceModelRecordEntity(entity);

        when(appPackageRepository.findByPackageId(anyString())).thenReturn(Optional.of(packageSetForDeletion));
        when(toscaoService.deleteServiceModel(anyString())).thenReturn(true);

        toscaHelper.deleteServiceModelFromToscaoByAppPackage(packageSetForDeletion.getPackageId());

        verify(toscaoService).deleteServiceModel(anyString());
    }

    @Test
    public void testGetToscaDefinitionVersionFromExistingVnfDescriptor() {
        JSONObject vnfDescriptor = vnfdService.getVnfDescriptor(validTosca1dot3VnfdPath);
        assertThat(toscaHelper.getToscaDefinitionsVersion(vnfDescriptor)).isEqualTo(Constants.TOSCA_1_3_DEFINITIONS_VERSION);
    }

    @Test
    public void testGetToscaDefinitionVersionFromEmptyVnfDescriptor() {
        JSONObject vnfDescriptor = vnfdService.getVnfDescriptor(emptyVnfdPath);
        assertThatThrownBy(() -> toscaHelper.getToscaDefinitionsVersion(vnfDescriptor))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(Constants.VNF_DESCRIPTOR_DOES_NOT_CONTAIN_DEFINITIONS_VERSION_FIELD);
    }

    @Test
    public void testGetToscaDefinitionsVersionFromNonExistingVnfDescriptor() {
        assertThatThrownBy(() -> toscaHelper.getToscaDefinitionsVersion(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage(Constants.VNF_DESCRIPTOR_MUST_NOT_BE_NULL);
    }

    @Test
    public void testUploadPackageToToscao() throws IOException {
        var packageContent = Files.createFile(tempFolder.resolve("fakeVnfPackage.zip"));
        ArgumentCaptor<Path> repackedCSARcaptor = ArgumentCaptor.forClass(Path.class);
        Map<String, Path> artifactPath = Map.of(Constants.PATH_TO_PACKAGE, packageContent);
        ServiceModel serviceModel = new ServiceModel();

        PackageUploadRequestContext context = new PackageUploadRequestContext();
        context.setArtifactPaths(artifactPath);
        context.setServiceModel(serviceModel);

        when(toscaoService.uploadPackageToToscao(repackedCSARcaptor.capture())).thenReturn(Optional.of(serviceModel));

        toscaHelper.uploadPackageToToscao(context);

        verify(fileService).deleteFile(repackedCSARcaptor.getValue().toFile());
        verify(toscaoService).uploadPackageToToscao(repackedCSARcaptor.getValue());
    }

    @Test
    public void testUploadPackageToToscaoCatchesToscaoException() throws IOException {
        var packageContent = Files.createFile(tempFolder.resolve("fakeVnfPackage.zip"));
        ArgumentCaptor<Path> repackedCSARcaptor = ArgumentCaptor.forClass(Path.class);
        Map<String, Path> artifactPath = Map.of(Constants.PATH_TO_PACKAGE, packageContent);
        ServiceModel serviceModel = new ServiceModel();

        PackageUploadRequestContext context = new PackageUploadRequestContext();
        context.setArtifactPaths(artifactPath);
        context.setServiceModel(serviceModel);
        when(toscaoService.uploadPackageToToscao(repackedCSARcaptor.capture())).thenThrow(new ToscaoException(null));

        toscaHelper.uploadPackageToToscao(context);

        assertThat(context.getErrors()).isNotNull();
        assertThat(context.getErrors().get(0).getMessage()).contains("Failed to upload VNF package from ");
        verify(toscaoService).uploadPackageToToscao(repackedCSARcaptor.getValue());
        verify(fileService).deleteFile(repackedCSARcaptor.getValue().toFile());
    }

    @Test
    public void testIsEtsiPackage() throws IOException {
        var packageContent = Files.createFile(tempFolder.resolve("fakeVnfPackage.zip"));
        Map<String, Path> artifactPath = Map.of(Constants.PATH_TO_PACKAGE, packageContent, ENTRY_DEFINITIONS, packageContent);
        PackageUploadRequestContext context = new PackageUploadRequestContext();
        context.setArtifactPaths(artifactPath);
        boolean isEtsi = toscaHelper.isEtsiPackage(context);

        assertThat(isEtsi).isTrue();
    }
}