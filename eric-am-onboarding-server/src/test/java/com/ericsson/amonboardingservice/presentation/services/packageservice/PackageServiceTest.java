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
import com.ericsson.am.shared.vnfd.model.typedefinition.TypeDefinitions;
import com.ericsson.am.shared.vnfd.service.ToscaoService;
import com.ericsson.am.shared.vnfd.service.exception.ToscaoException;
import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.TestUtils;
import com.ericsson.amonboardingservice.model.AdditionalPropertyResponse;
import com.ericsson.amonboardingservice.model.AppPackageResponse;
import com.ericsson.amonboardingservice.model.CreateVnfPkgInfoRequest;
import com.ericsson.amonboardingservice.model.HelmPackage;
import com.ericsson.amonboardingservice.model.VnfPkgInfo;
import com.ericsson.amonboardingservice.presentation.exceptions.DataNotFoundException;
import com.ericsson.amonboardingservice.presentation.exceptions.IllegalPackageStateException;
import com.ericsson.amonboardingservice.presentation.exceptions.PackageNotFoundException;
import com.ericsson.amonboardingservice.presentation.models.ChartUrlsEntity;
import com.ericsson.amonboardingservice.presentation.models.ServiceModelRecordEntity;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackageArtifacts;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackageDockerImage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageArtifactsRepository;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.repositories.ChartUrlsRepository;
import com.ericsson.amonboardingservice.presentation.services.dockerservice.DockerService;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.ericsson.amonboardingservice.presentation.services.helmservice.HelmService;
import com.ericsson.amonboardingservice.presentation.services.onboarding.request.GenerateChecksum;
import com.ericsson.amonboardingservice.presentation.services.onboarding.request.Persist;
import com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaService;
import com.ericsson.amonboardingservice.utils.Constants;
import com.ericsson.amonboardingservice.infrastructure.client.RestClient;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.ericsson.amonboardingservice.TestUtils.getResource;
import static com.ericsson.amonboardingservice.TestUtils.readDataFromFile;
import static com.ericsson.amonboardingservice.presentation.models.ChartUrlsEntity.ChartTypeEnum.CNF;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_DEFINITIONS;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_HELM_DEFINITIONS;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_IMAGES;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_MANIFEST;
import static com.ericsson.amonboardingservice.utils.JsonUtils.parseJsonToClass;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
        properties = {
                "onboarding.skipToscaoValidation = true",
                "url="
        })
public class PackageServiceTest extends AbstractDbSetupTest {
    private static final String EXISTING_PACKAGE_ID = "b3def1ce-4cf4-477c-aab3-21cb04e6a379";
    private static final String NOT_EXISTING_PACKAGE_ID = "b3def1ce-4cf4-477c-1111-21cb04e6a379";
    private static final String USER_DEFINED_KEY = "testKey";
    private static final String USER_DEFINED_VALUE = "testVal";
    private static final byte[] HELMFILE_CONTENT = new byte[]{};

    private MockMultipartFile file;

    @Autowired
    private PackageServiceImpl packageService;

    @Autowired
    private AppPackageRepository appPackageRepository;

    @Autowired
    private AppPackageArtifactsRepository appPackageArtifactsRepository;

    @Autowired
    private Persist persist;

    @Autowired
    private ChartUrlsRepository chartUrlsRepository;

    @MockBean
    private RestClient restClient;

    @MockBean
    private FileService fileService;

    @MockBean
    private HelmService helmService;

    @MockBean
    private DockerService dockerService;

    @MockBean
    private ToscaMetaService toscaMetaService;

    @MockBean
    private ToscaoService toscaoService;

    @MockBean
    private GenerateChecksum headChainOfResponsibility;

    @Test
    @Transactional
    public void shouldGetExistingPackage() {
        Optional<AppPackageResponse> packageInfo = packageService.getPackage(EXISTING_PACKAGE_ID);
        assertThat(packageInfo.get().getAppDescriptorId()).matches(EXISTING_PACKAGE_ID);
        assertThat(packageInfo.get().getImagesURL()).hasSize(3);
    }

    @Test
    @Transactional
    public void shouldSaveNewPackage() {
        AppPackage appPackage = getAppPackage();
        assertThat(packageService.savePackage(appPackage)).isNotNull();
        packageService.deletePackage(appPackage.getPackageId());
    }

    @Test
    @Transactional
    public void shouldSaveNewPackageWithoutHelmileInside() {
        AppPackage appPackage = getAppPackage();
        appPackage.setHelmfile(null);
        assertThat(packageService.savePackage(appPackage)).isNotNull();
        packageService.deletePackage(appPackage.getPackageId());
    }

    @Test
    @Transactional
    public void shouldSaveNewPackageWithSizeOfHelmfileMoreThat32kb() throws URISyntaxException, IOException {
        AppPackage appPackage = getAppPackage();
        appPackage.setHelmfile(getHelmfileContent());
        assertThat(packageService.savePackage(appPackage)).isNotNull();
        AppPackage savedPacked = appPackageRepository.findByPackageId(appPackage.getPackageId()).get();
        assertThat(savedPacked.getHelmfile()).hasSizeGreaterThanOrEqualTo(32000);
        packageService.deletePackage(appPackage.getPackageId());
    }

    @Test
    @Transactional
    public void shouldSaveNewPackageWithTwoCharts() {
        // given
        AppPackage appPackage = getAppPackage();
        ChartUrlsEntity chartUrlsEntity = getChartUrlsEntity(appPackage);
        String chartsRegistryUrl = "http://eric-lcm-helm-chart-registry.process-engine-4-eric-am-onboarding-service" +
                ":8080/onboarded/charts/sampledescriptor-0.0.1-224.tgz";
        String chartName = "sampledescriptor";
        String chartVersion = "0.0.1-224";
        chartUrlsEntity.setChartsRegistryUrl(chartsRegistryUrl);
        chartUrlsEntity.setPriority(2);
        chartUrlsEntity.setChartName(chartName);
        chartUrlsEntity.setChartVersion(chartVersion);
        appPackage.getChartsRegistryUrl().add(chartUrlsEntity);

        // when
        AppPackage savedPackage = packageService.savePackage(appPackage);

        // then
        assertThat(savedPackage).isNotNull();

        AppPackageResponse appPackageResponse = packageService.getPackage(appPackage.getPackageId()).get();
        assertThat(appPackageResponse.getHelmPackageUrls().size()).isEqualTo(2);

        HelmPackage helmPackage = appPackageResponse.getHelmPackageUrls().get(1);
        assertThat(helmPackage.getChartUrl()).isEqualTo(chartsRegistryUrl);
        assertThat(helmPackage.getPriority()).isEqualTo(2);
        assertThat(helmPackage.getChartName()).isEqualTo(chartName);
        assertThat(helmPackage.getChartVersion()).isEqualTo(chartVersion);
        assertThat(helmPackage.getChartArtifactKey()).isEqualTo("helm_package");
        // cleanup
        packageService.deletePackage(appPackage.getPackageId());
    }

    @Test
    @Transactional
    public void shouldDeletePackageIdentiferWithNoChart() {
        CreateVnfPkgInfoRequest createVnfPkgInfoRequest = new CreateVnfPkgInfoRequest();
        Map<String, String> userDefinedData = new HashMap<>();
        userDefinedData.put(USER_DEFINED_KEY, USER_DEFINED_VALUE);
        createVnfPkgInfoRequest.setUserDefinedData(userDefinedData);
        VnfPkgInfo vnfPkgInfo = packageService.createVnfPackage(createVnfPkgInfoRequest);
        packageService.deletePackage(vnfPkgInfo.getId());
        Optional<AppPackageResponse> aPackage = packageService.getPackage(vnfPkgInfo.getId());
        assertThat(aPackage.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void shouldGetAllPackages() {
        int expectedAppPackagesCount = (int) appPackageRepository.count();

        Page<AppPackageResponse> packageInfoPage = packageService.listPackages(Pageable.unpaged());

        assertThat(packageInfoPage.getContent()).hasSize(expectedAppPackagesCount);
    }

    @Test
    public void shouldReturnNullForNonExistentPackageId() {
        Optional<AppPackageResponse> packageInfo = packageService.getPackage(NOT_EXISTING_PACKAGE_ID);
        assertThat(packageInfo).isEqualTo(Optional.empty());
    }

    @Test
    @Transactional
    public void shouldDeleteExistingPackage() throws URISyntaxException {
        when(restClient.buildUrl(anyString(), anyString())).thenCallRealMethod();
        when(helmService.getChart(anyString(), anyString()))
                .thenReturn(Optional.of(getResource("spider-app-pm-only-2.74.7.tgz").toFile()));
        doNothing().when(fileService).deleteFile(any(File.class));
        String packageId = "3e0f5f6a-bcc2-4279-82ef-b3a11a14d456";

        packageService.deletePackage(packageId);
        packageService.removeAppPackageWithResources(packageId);

        assertThat(packageService.getPackage(packageId)).isNotPresent();
        verify(helmService, times(1)).getChart(anyString(), anyString());
        verify(helmService, times(1)).deleteChart(anyString(), anyString());
        verify(dockerService, times(1)).removeDockerImagesByPackageId(eq(packageId));
    }

    @Test
    @Transactional
    public void shouldDeleteToscaOPackage() throws URISyntaxException, ToscaoException {
        when(restClient.buildUrl(anyString(), anyString())).thenCallRealMethod();
        when(helmService.getChart(anyString(), anyString()))
                .thenReturn(Optional.of(getResource("spider-app-pm-only-2.74.7.tgz").toFile()));
        doNothing().when(fileService).deleteFile(any(File.class));
        String packageId = "3e0f5f6a-bcc2-4279-82ef-b3a11a14d456";

        AppPackage appPackage = appPackageRepository.findByPackageId(packageId).get();
        ServiceModelRecordEntity serviceModelRecordEntity = new ServiceModelRecordEntity();
        String serviceModelId = "3e0f5f6a-bcc2-4279-82ef-b3a11a14d459";
        serviceModelRecordEntity.setServiceModelId(serviceModelId);
        appPackage.setServiceModelRecordEntity(serviceModelRecordEntity);
        appPackageRepository.save(appPackage);

        packageService.deletePackage(packageId);

        assertThat(packageService.getPackage(packageId)).isNotPresent();
    }

    @Test
    @Transactional
    public void shouldExtractProperChartNameForDeletion() throws URISyntaxException {
        when(restClient.buildUrl(anyString(), anyString())).thenCallRealMethod();
        when(helmService.getChart(anyString(), anyString()))
                .thenReturn(Optional.of(getResource("spider-app-pm-only-2.74.7.tgz").toFile()));
        doNothing().when(fileService).deleteFile(any(File.class));
        String packageId = "3e0f5f6a-bcc2-4279-82ef-b3a11a14d555";

        packageService.deletePackage(packageId);
        packageService.removeAppPackageWithResources(packageId);

        assertThat(packageService.getPackage(packageId)).isNotPresent();
        verify(helmService, times(1)).getChart(anyString(), eq("test-scale-chart-0.1.0.tgz"));
        verify(helmService, times(1)).deleteChart(anyString(), anyString());
        verify(dockerService, times(1)).removeDockerImagesByPackageId(eq(packageId));
    }

    @Test
    public void getTypeParameterValue() {
        CompletableFuture<List<String>> allType
                = packageService.getAutoCompleteResponse(Constants.TYPE, "SG", 0, 5);
        CompletableFuture.allOf(allType).join();
        try {
            List<String> types = allType.get();
            assertThat(types).isNotNull().isNotEmpty();
        } catch (Exception ex) {
            fail("Test fail");
        }
    }

    @Test
    public void getTypeParameterValueWithEmptyType() {
        CompletableFuture<List<String>> allType
                = packageService.getAutoCompleteResponse(Constants.TYPE, "", 0, 5);
        CompletableFuture.allOf(allType).join();
        try {
            List<String> types = allType.get();
            assertThat(types).isNotNull().isNotEmpty();
            assertThat(types.size()).isEqualTo(5);
        } catch (Exception ex) {
            fail("Test fail");
        }
    }

    @Test
    public void getTypeParameterValueWithNullType() {
        CompletableFuture<List<String>> allType
                = packageService.getAutoCompleteResponse(Constants.TYPE, null, 0, 5);
        CompletableFuture.allOf(allType).join();
        try {
            List<String> types = allType.get();
            assertThat(types).isNotNull().isEmpty();
        } catch (Exception ex) {
            fail("Test fail");
        }
    }

    @Test
    public void shouldCreateVnfPackageWithUserDefinedData() {
        CreateVnfPkgInfoRequest createVnfPkgInfoRequest = new CreateVnfPkgInfoRequest();
        Map<String, String> userDefinedData = new HashMap<>();
        userDefinedData.put(USER_DEFINED_KEY, USER_DEFINED_VALUE);
        createVnfPkgInfoRequest.setUserDefinedData(userDefinedData);

        VnfPkgInfo vnfPkgInfo = packageService.createVnfPackage(createVnfPkgInfoRequest);
        assertThat(vnfPkgInfo.getId()).isNotNull();
        assertThat(vnfPkgInfo.getOnboardingState()).isEqualTo(VnfPkgInfo.OnboardingStateEnum.CREATED);
        assertThat(vnfPkgInfo.getOperationalState()).isEqualTo(VnfPkgInfo.OperationalStateEnum.DISABLED);
        assertThat(vnfPkgInfo.getUsageState()).isEqualTo(VnfPkgInfo.UsageStateEnum.NOT_IN_USE);
        assertThat(vnfPkgInfo.getUserDefinedData().toString()).isEqualTo("{testKey=testVal}");
        assertThat(vnfPkgInfo.getLinks().getSelf()).isNotNull();
        assertThat(vnfPkgInfo.getChecksum()).isNull();
    }

    @Test
    public void shouldCreateVnfPackageWithoutUserDefinedData() {
        CreateVnfPkgInfoRequest createVnfPkgInfoRequest = new CreateVnfPkgInfoRequest();

        VnfPkgInfo vnfPkgInfo = packageService.createVnfPackage(createVnfPkgInfoRequest);
        assertThat(vnfPkgInfo.getId()).isNotNull();
        assertThat(vnfPkgInfo.getOnboardingState()).isEqualTo(VnfPkgInfo.OnboardingStateEnum.CREATED);
        assertThat(vnfPkgInfo.getOperationalState()).isEqualTo(VnfPkgInfo.OperationalStateEnum.DISABLED);
        assertThat(vnfPkgInfo.getUsageState()).isEqualTo(VnfPkgInfo.UsageStateEnum.NOT_IN_USE);
        assertThat(vnfPkgInfo.getLinks().getSelf()).isNotNull();
        assertThat(vnfPkgInfo.getChecksum()).isNull();
    }

    @Test
    public void shouldCreateVnfPackageWithEmptyUserDefinedData() {
        CreateVnfPkgInfoRequest createVnfPkgInfoRequest = new CreateVnfPkgInfoRequest();
        Map<String, String> userDefinedData = new HashMap<>();
        createVnfPkgInfoRequest.setUserDefinedData(userDefinedData);

        VnfPkgInfo vnfPkgInfo = packageService.createVnfPackage(createVnfPkgInfoRequest);
        assertThat(vnfPkgInfo.getId()).isNotNull();
        assertThat(vnfPkgInfo.getOnboardingState()).isEqualTo(VnfPkgInfo.OnboardingStateEnum.CREATED);
        assertThat(vnfPkgInfo.getOperationalState()).isEqualTo(VnfPkgInfo.OperationalStateEnum.DISABLED);
        assertThat(vnfPkgInfo.getUsageState()).isEqualTo(VnfPkgInfo.UsageStateEnum.NOT_IN_USE);
        assertThat(vnfPkgInfo.getLinks().getSelf()).isNotNull();
        assertThat(vnfPkgInfo.getChecksum()).isNull();
    }

    @Test
    public void shouldFailCreatePackageWithInvalidUserDefinedData() {
        CreateVnfPkgInfoRequest createVnfPkgInfoRequest = new CreateVnfPkgInfoRequest();
        createVnfPkgInfoRequest.setUserDefinedData("{invalidJson");
        assertThatThrownBy(() -> packageService.createVnfPackage(createVnfPkgInfoRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith(Constants.USER_DEFINED_DATA_INVALID_FORMAT);
    }

    @Test
    @Transactional
    public void getAllPackageWithAValidFilterOnProductName() {
        Page<AppPackageResponse> appPackageResponsePage =
                packageService.listPackages("(eq,packages/appProductName,SAPC)", Pageable.unpaged());
        assertThat(appPackageResponsePage.getContent())
                .isNotEmpty()
                .extracting(AppPackageResponse::getAppProductName)
                .containsOnly("SAPC");

        Page<AppPackageResponse> allSGSNMMEPackagesPage = packageService.listPackages(
                "(eq,packages/appProductName,SGSN-MME)", Pageable.unpaged());
        assertThat(allSGSNMMEPackagesPage.getContent())
                .isNotEmpty()
                .extracting(AppPackageResponse::getAppProductName)
                .containsOnly("SGSN-MME");
    }

    @Test
    public void testGetAllPackagesWithProductNameNotPresent() {
        Page<AppPackageResponse> appPackagePage =
                packageService.listPackages("(eq,packages/appProductName,test)", Pageable.unpaged());

        assertThat(appPackagePage.hasContent()).isFalse();
    }

    @Test
    public void testGetAllPackagesWithInvalidFilter() {
        assertThatThrownBy(() -> packageService.listPackages("(eq,appProductName)", Pageable.unpaged()))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(
                        "Invalid filter value provided eq,appProductName");
    }

    @Test
    public void testGetAllPackagesWithInvalidOperation() {
        assertThatThrownBy(() -> packageService.listPackages(
                "(test,packages/appProductName,test)", Pageable.unpaged()))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(
                        "Invalid operation provided test,packages/appProductName,test");
    }

    @Test
    public void testGetAllPackagesWithInvalidParameter() {
        assertThatThrownBy(() -> packageService.listPackages("(eq,test,test)", Pageable.unpaged()))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Filter eq,test,test not supported");
    }

    @Test
    @Transactional
    public void testGetAllPackagesWithMultipleFilters() {
        Page<AppPackageResponse> appPackagePage = packageService.listPackages(
                "(eq,packages/appProductName,SGSN-MME);(eq,packages/appSoftwareVersion,1.20 (CXS101289_R81E08))",
                Pageable.unpaged());

        assertThat(appPackagePage.getContent())
                .isNotEmpty()
                .extracting(AppPackageResponse::getAppProductName, AppPackageResponse::getAppSoftwareVersion)
                .containsOnly(tuple("SGSN-MME", "1.20 (CXS101289_R81E08)"));
    }

    @Test
    @Transactional
    public void testGetAllPackagesWithMultipleSoftwareVersion() {
        Page<AppPackageResponse> appPackagePage = packageService.listPackages(
                "(in,packages/appSoftwareVersion,1.20 (CXS101289_R81E08),1.21 (CXS101289_R81E08))",
                Pageable.unpaged());

        assertThat(appPackagePage.getContent())
                .isNotEmpty()
                .extracting(AppPackageResponse::getAppSoftwareVersion)
                .containsOnly("1.20 (CXS101289_R81E08)", "1.21 (CXS101289_R81E08)");
    }

    @Test
    public void testListPackagesWithNonExistentChartUrl() {
        List<AppPackage> packages = packageService.listPackagesWithChartUrl("this_chart_does_not_exist");
        assertThat(packages.isEmpty()).isTrue();
    }

    @Test
    public void testListPackagesWithExistentChartUrl() {
        List<AppPackage> packages = packageService.listPackagesWithChartUrl("http://10.210.53.96:31028/api/onboarded/charts/Ericsson.SGSN-MME/1.20");
        assertThat(packages.size()).isGreaterThan(1);
    }

    @Test
    public void testListPackagesWithExistentOnePackageChartUrl() {
        List<AppPackage> packages = packageService.listPackagesWithChartUrl("http://10.210.53.96:31028/api/onboarded/charts/Ericsson.SGSN-MME/1.22");
        assertThat(packages.size()).isEqualTo(1);
    }

    @Test
    @Transactional
    public void testDeletePackageWithChartUsedInMultiplePackages() {
        when(restClient.buildUrl(anyString(), anyString())).thenCallRealMethod();
        packageService.deletePackage("e3def1ce-4cf4-477c-aab3-21cb04e6a383");
        assertThat(packageService.getPackage("e3def1ce-4cf4-477c-aab3-21cb04e6a383")).isEqualTo(Optional.empty());
        verify(restClient, times(0)).getFile(anyString(), anyString(), anyString(), anyString());
        verify(restClient, times(0)).delete(anyString(), anyString(), anyString());
        // Put the database back into the state it was before, depending on execution order other tests were failing.
        AppPackage appPackage = getAppPackage();
        packageService.savePackage(appPackage);
    }

    @Transactional
    @Test
    public void testAllPackageArtifactsSaved() throws URISyntaxException {
        AppPackage appPackage = appPackageRepository.findByPackageId("e3def1ce-4cf4-477c-aab3-21cb04e6a381").get();

        ChartUrlsEntity chartUrlsEntity = getChartUrlsEntity(appPackage);
        appPackage.getChartsRegistryUrl().add(chartUrlsEntity);
        AppPackageDockerImage appPackageDockerImage = new AppPackageDockerImage();
        appPackageDockerImage.setImageId("armdocker.rnd.ericsson.se/proj-nose/adp-ns-gs:0.0.1-222");
        appPackageDockerImage.setAppPackage(appPackage);
        appPackage.getAppPackageDockerImages().add(appPackageDockerImage);
        Path csarFile = TestUtils.getResource("package-artifacts").toAbsolutePath();

        Map<String, Path> artifacts = prepareArtifactPaths(csarFile);
        when(toscaMetaService.getToscaMetaPath(csarFile)).thenReturn(csarFile.resolve("TOSCA-Metadata/TOSCA.meta"));
        persist.setPackageArtifacts(appPackage, artifacts, csarFile);
        appPackageRepository.save(appPackage);

        List<AppPackageArtifacts> appPackageArtifacts = appPackageArtifactsRepository.findByAppPackage(appPackage);
        assertThat(appPackageArtifacts.size()).isEqualTo(4);

        AppPackageArtifacts packageArtifacts = appPackageArtifactsRepository.findByAppPackageAndArtifactPath(appPackage,
                "sampledescriptor-0.0.1-223.mf").get();
        assertThat(packageArtifacts.getArtifactPath()).isEqualTo("sampledescriptor-0.0.1-223.mf");
        assertThat(packageArtifacts.getArtifact()).isNotEmpty();
        AppPackageArtifacts packageArtifactForEntryHelmDefinitions = appPackageArtifactsRepository.findByAppPackageAndArtifactPath(appPackage,
                "Definitions/OtherTemplates/valid_helm_definitions_values.yaml").get();
        assertThat(packageArtifactForEntryHelmDefinitions.getArtifactPath()).isEqualTo("Definitions/OtherTemplates/valid_helm_definitions_values.yaml");
        assertThat(packageArtifactForEntryHelmDefinitions.getArtifact()).isNotEmpty();
    }

    @Transactional
    @Test
    public void testAllPackageArtifactsSaved_ymlEntryHelmDefinitions_present() throws URISyntaxException {
        AppPackage appPackage = appPackageRepository.findByPackageId("e3def1ce-4cf4-477c-aab3-21cb04e6a381").get();

        ChartUrlsEntity chartUrlsEntity = getChartUrlsEntity(appPackage);
        appPackage.getChartsRegistryUrl().add(chartUrlsEntity);
        AppPackageDockerImage appPackageDockerImage = new AppPackageDockerImage();
        appPackageDockerImage.setImageId("armdocker.rnd.ericsson.se/proj-nose/adp-ns-gs:0.0.1-222");
        appPackageDockerImage.setAppPackage(appPackage);
        appPackage.getAppPackageDockerImages().add(appPackageDockerImage);
        Path csarFile = TestUtils.getResource("package-artifacts-definitions-yml").toAbsolutePath();

        Map<String, Path> artifacts = prepareArtifactPaths(csarFile);
        when(toscaMetaService.getToscaMetaPath(csarFile)).thenReturn(csarFile.resolve("TOSCA-Metadata/TOSCA.meta"));
        persist.setPackageArtifacts(appPackage, artifacts, csarFile);
        appPackageRepository.save(appPackage);

        List<AppPackageArtifacts> appPackageArtifacts = appPackageArtifactsRepository.findByAppPackage(appPackage);
        assertThat(appPackageArtifacts.size()).isEqualTo(5);

        AppPackageArtifacts packageArtifacts = appPackageArtifactsRepository.findByAppPackageAndArtifactPath(appPackage,
                "sampledescriptor-0.0.1-223.mf").get();
        assertThat(packageArtifacts.getArtifactPath()).isEqualTo("sampledescriptor-0.0.1-223.mf");
        assertThat(packageArtifacts.getArtifact()).isNotEmpty();
        boolean isYmlEntryHelmDefinitionSaved = appPackageArtifactsRepository.findByAppPackageAndArtifactPath(appPackage,
                "Definitions/OtherTemplates/helm_definitions_with_yml_extension.yml").isPresent();
        assertThat(isYmlEntryHelmDefinitionSaved).isEqualTo(true);
    }

    @Transactional
    @Test
    public void testAllPackageArtifactsSaved_scalingMapping_ignored() throws URISyntaxException {
        AppPackage appPackage = appPackageRepository.findByPackageId("e3def1ce-4cf4-477c-aab3-21cb04e6a381").get();

        ChartUrlsEntity chartUrlsEntity = getChartUrlsEntity(appPackage);
        appPackage.getChartsRegistryUrl().add(chartUrlsEntity);
        AppPackageDockerImage appPackageDockerImage = new AppPackageDockerImage();
        appPackageDockerImage.setImageId("armdocker.rnd.ericsson.se/proj-nose/adp-ns-gs:0.0.1-222");
        appPackageDockerImage.setAppPackage(appPackage);
        appPackage.getAppPackageDockerImages().add(appPackageDockerImage);
        Path csarFile = TestUtils.getResource("package-artifacts-definitions-with-scaling-mapping").toAbsolutePath();

        Map<String, Path> artifacts = prepareArtifactPaths(csarFile);
        when(toscaMetaService.getToscaMetaPath(csarFile)).thenReturn(csarFile.resolve("TOSCA-Metadata/TOSCA.meta"));
        persist.setPackageArtifacts(appPackage, artifacts, csarFile);
        appPackageRepository.save(appPackage);

        List<AppPackageArtifacts> appPackageArtifacts = appPackageArtifactsRepository.findByAppPackage(appPackage);
        assertThat(appPackageArtifacts.size()).isEqualTo(5);

        boolean isScalingMappingNotSaved = appPackageArtifactsRepository.findByAppPackageAndArtifactPath(appPackage,
                "Definitions/OtherTemplates/scaling_mapping.yaml").isEmpty();
        assertThat(isScalingMappingNotSaved).isEqualTo(false);
    }

    @Test
    @Transactional
    public void shouldFetchArtifact() {
        AppPackage appPackage = appPackageRepository.findByPackageId("f3def1ce-4cf4-477c-456").get();
        String artifactPath = "TOSCA-Metadata/TOSCA.meta";
        AppPackageArtifacts appPackageArtifacts = appPackageArtifactsRepository.findByAppPackageAndArtifactPath(appPackage, artifactPath).get();
        assertThat(appPackageArtifacts.getArtifact()).isNotEmpty();
    }

    @Test
    public void testGetAdditionalParamsForOperationType_instantiate() throws ToscaoException {
        String pkgId = "spider-app-multi-v2-2cb5";
        String operationType = "instantiate";
        String destinationDescriptorId = "";

        List<AdditionalPropertyResponse> propertyList = packageService.getAdditionalParamsForOperationType(pkgId, operationType, destinationDescriptorId);

        assert propertyList != null;
        assertThat(propertyList.size()).isEqualTo(23);

    }

    @Test
    public void testGetAdditionalParamsForOperationType_rollback() throws ToscaoException {
        String pkgId = "spider-app-multi-v2-2cb5";
        String operationType = "rollback";
        String destinationDescriptorId = "multi-chart-477c-aab3-2b04e6a383";
        String expectedPropName = "data_conversion_identifier";

        List<AdditionalPropertyResponse> propertyList = packageService.getAdditionalParamsForOperationType(pkgId, operationType, destinationDescriptorId);

        assert propertyList != null;
        assertThat(propertyList.size()).isEqualTo(1);
        assertThat(propertyList.get(0).getName()).isEqualTo(expectedPropName);
    }

    @Test
    public void testGetAdditionalParamsForOperationTypeChangePackageTosca1Dot2() throws ToscaoException {
        String pkgId = "spider-app-multi-v2-2cb5";
        String operationType = "change_package";
        String destinationDescriptorId = "multi-chart-477c-aab3-2b04e6a383";
        String expectedPropName = "vnfc2.ingress.host";
        List<AdditionalPropertyResponse> propertyList = packageService.getAdditionalParamsForOperationType(pkgId, operationType, destinationDescriptorId);

        assert propertyList != null;
        assertThat(propertyList.size()).isEqualTo(26);
        assertThat(propertyList.get(0).getName()).isEqualTo(expectedPropName);
    }

    @Test
    public void testGetAdditionalParamsForOperationTypeChangePackageTosca1Dot3() throws ToscaoException {
        String pkgId = "spider-app-multi-v2-2cb5";
        String operationType = "change_package";
        String expectedPropName = "vnfc2.ingress.host";
        List<AdditionalPropertyResponse> propertyList = packageService.getAdditionalParamsForOperationType(pkgId, operationType, null);

        assert propertyList != null;
        assertThat(propertyList.size()).isEqualTo(26);
        assertThat(propertyList.get(0).getName()).isEqualTo(expectedPropName);
    }

    @Test
    public void testGetAdditionalParamsForOperationTypeWrongChangePackageTosca1Dot3() throws ToscaoException {
        String pkgId = "spider-app-multi-v2-2cb5";
        String operationType = "change_package";
        String expectedPropName = "vnfc2.ingress.host";
        List<AdditionalPropertyResponse> propertyList = packageService.getAdditionalParamsForOperationType(pkgId, operationType, null);

        assert propertyList != null;
        assertThat(propertyList.size()).isEqualTo(26);
        assertThat(propertyList.get(0).getName()).isEqualTo(expectedPropName);
    }

    @Test
    public void testGetAdditionalParamsForOperationType_packageNotFound() {
        String pkgId = "pkg-not-found-477c-gt45-2cb5";
        String operationType = "instantiate";
        String destinationDescriptorId = "";

        assertThatThrownBy(() -> packageService.getAdditionalParamsForOperationType(pkgId, operationType, destinationDescriptorId))
                .isInstanceOf(PackageNotFoundException.class).hasMessage(String.format(Constants.PACKAGE_NOT_PRESENT_ERROR_MESSAGE, pkgId));
    }

    @Test
    public void testGetAdditionalParamsForOperationType_rollbackTosca_V_1Dot3() throws Exception {
        String pkgId = "spider-app-c-tosca-2551";
        String operationType = "rollback";
        String destinationDescriptorId = "a604346f-ecd0-4612-ac90-5fcb086597ed";
        String expectedPropName = "data_conversion_identifier";

        ServiceModel serviceModel = parseJsonToClass(
                readDataFromFile("service-model.json", StandardCharsets.UTF_8), ServiceModel.class);
        TypeDefinitions typeDefinitions = parseJsonToClass(
                readDataFromFile("single-type-definition-c.json", StandardCharsets.UTF_8),
                TypeDefinitions.class);
        when(toscaoService.getServiceModelByServiceModelId(any())).thenReturn(Optional.ofNullable(serviceModel));
        when(toscaoService.getTypeDefinitions(serviceModel.getId())).thenReturn(Optional.ofNullable(typeDefinitions));

        List<AdditionalPropertyResponse> propertyList = packageService.getAdditionalParamsForOperationType(pkgId, operationType, destinationDescriptorId);

        assert propertyList != null;
        assertThat(propertyList.size()).isEqualTo(1);
        assertThat(propertyList.get(0).getName()).isEqualTo(expectedPropName);
    }

    @Test
    public void testGetAdditionalParamsForOperationType_instantiateTosca_V_1Dot3() throws Exception {
        String pkgId = "spider-app-c-tosca-2551";
        String operationType = "instantiate";
        String expectedPropName = "ossTopology.snmpPrivProtocol";

        ServiceModel serviceModel = parseJsonToClass(
                readDataFromFile("service-model.json", StandardCharsets.UTF_8), ServiceModel.class);
        TypeDefinitions typeDefinitions = parseJsonToClass(
                readDataFromFile("single-type-definition-c.json", StandardCharsets.UTF_8),
                TypeDefinitions.class);
        when(toscaoService.getServiceModelByServiceModelId(any())).thenReturn(Optional.ofNullable(serviceModel));
        when(toscaoService.getTypeDefinitions(serviceModel.getId())).thenReturn(Optional.ofNullable(typeDefinitions));

        List<AdditionalPropertyResponse> propertyList = packageService.getAdditionalParamsForOperationType(pkgId, operationType, "");

        assert propertyList != null;
        assertThat(propertyList.size()).isEqualTo(45);
        assertThat(propertyList.get(0).getName()).isEqualTo(expectedPropName);

    }

    @Test
    @Transactional
    public void shouldMapServiceModelRecordIdToAppPackageResponse() {
        Optional<AppPackageResponse> packageInfo = packageService.getPackage("spider-app-c-tosca-2551");
        assertThat(packageInfo.get().getServiceModelId()).matches("37866813-a0cb-4ad4-9716-a5b4fa19f940");
    }

    @Test
    @Transactional
    public void shouldReturnHelmfileContent() throws IOException {
        AppPackage appPackage = setUpAppPackageWithHelmfile();
        String packageId = appPackage.getPackageId();
        Object result = packageService.getHelmfileContentByPackageId(packageId);
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(HELMFILE_CONTENT);
        packageService.deletePackage(packageId);
    }

    @Transactional
    @Test
    public void shouldThrowExceptionWhenHelmfileNotPresent() throws IOException {
        AppPackage appPackage = setUpAppPackage("vnfd/valid_vnfd.yaml");
        String packageId = appPackage.getPackageId();
        assertThatThrownBy(() -> packageService.getHelmfileContentByPackageId(packageId))
                .isInstanceOf(DataNotFoundException.class);
        packageService.deletePackage(packageId);
    }

    @Test
    @Transactional
    public void testGetPackageWithSecurityOption() {
        AppPackage appPackage = getAppPackage(AppPackage.PackageSecurityOption.OPTION_2);
        String packageId = appPackageRepository.save(appPackage).getPackageId();

        AppPackageResponse vnfPkgInfo = packageService.getPackage(packageId).get();
        assertThat(vnfPkgInfo).isNotNull();
        assertThat(vnfPkgInfo.getPackageSecurityOption())
                .isEqualTo(AppPackageResponse.PackageSecurityOptionEnum.OPTION_2);
        appPackageRepository.deleteByPackageId(appPackage.getPackageId());
    }

    @Test
    @Transactional
    public void testGetAllPackagesWithSecurityOption() {
        AppPackage appPackage1 = getAppPackage(AppPackage.PackageSecurityOption.OPTION_2);
        AppPackage appPackage2 = getAppPackage(AppPackage.PackageSecurityOption.OPTION_2);
        String packageId1 = appPackageRepository.save(appPackage1).getPackageId();
        String packageId2 = appPackageRepository.save(appPackage2).getPackageId();

        Page<AppPackageResponse> appPackagePage = packageService.listPackages(Pageable.unpaged());

        assertThat(appPackagePage.getContent())
                .isNotEmpty()
                .extracting(AppPackageResponse::getAppPkgId, AppPackageResponse::getPackageSecurityOption)
                .contains(tuple(packageId1, AppPackageResponse.PackageSecurityOptionEnum.OPTION_2),
                        tuple(packageId2, AppPackageResponse.PackageSecurityOptionEnum.OPTION_2));

        appPackageRepository.deleteByPackageId(packageId1);
        appPackageRepository.deleteByPackageId(packageId2);
    }

    @Test
    @Transactional
    public void testGetFilteredPackagesWithSecurityOption1() {
        AppPackage appPackage1 = getAppPackage(AppPackage.PackageSecurityOption.OPTION_1);
        AppPackage appPackage2 = getAppPackage(AppPackage.PackageSecurityOption.OPTION_2);
        String packageId1 = appPackageRepository.save(appPackage1).getPackageId();
        String packageId2 = appPackageRepository.save(appPackage2).getPackageId();

        List<AppPackageResponse> packageList = packageService
                .listPackages("(eq,packages/packageSecurityOption,OPTION_1)", Pageable.unpaged()).getContent();

        assertThat(packageList)
                .extracting(AppPackageResponse::getAppPkgId, AppPackageResponse::getPackageSecurityOption)
                .contains(tuple(packageId1, AppPackageResponse.PackageSecurityOptionEnum.OPTION_1))
                .doesNotContain(tuple(packageId2, AppPackageResponse.PackageSecurityOptionEnum.OPTION_2));

        appPackageRepository.deleteByPackageId(packageId1);
        appPackageRepository.deleteByPackageId(packageId2);
    }

    @Test
    public void uploadPackageFromInputStreamNoFilenameError() throws IOException {
        AppPackage appPackage = setUpAppPackageWithHelmfile();
        file = buildMultipartFileForPreUploadValidation();
        InputStream is = file.getInputStream();

        AssertionsForClassTypes.assertThatThrownBy(() -> packageService.packageUpload(appPackage.getPackageId(), is, 5000))
                .isInstanceOf(IllegalPackageStateException.class)
                .hasMessageContaining("Filename for package wasn't provided");

        packageService.deletePackage(appPackage.getPackageId());
    }

    private AppPackage getAppPackage(AppPackage.PackageSecurityOption securityOption) {
        AppPackage appPackage = new AppPackage();
        appPackage.setPackageSecurityOption(securityOption);
        appPackage.setOnboardingState(AppPackage.OnboardingStateEnum.ONBOARDED);
        return appPackage;
    }

    private MockMultipartFile buildMultipartFileForPreUploadValidation() {
        String fileName = "test-file";
        String fileContent = "Test Content";
        return new MockMultipartFile(fileName, fileContent.getBytes());
    }

    private Map<String, Path> prepareArtifactPaths(Path csarPath) {
        Map<String, Path> artifacts = new HashMap<>();
        artifacts.put(Constants.CSAR_DIRECTORY, csarPath);
        artifacts.put(ENTRY_DEFINITIONS, csarPath.resolve("Definitions/sampledescriptor-0.0.1-223.yaml"));
        artifacts.put(ENTRY_IMAGES, csarPath.resolve("Files/images/docker.tar"));
        artifacts.put(ENTRY_HELM_DEFINITIONS, csarPath.resolve("Definitions/OtherTemplates/"));
        artifacts.put(ENTRY_MANIFEST, csarPath.resolve("sampledescriptor-0.0.1-223.mf"));

        return artifacts;
    }

    private AppPackage setUpAppPackage(String fileName) throws IOException {
        String vnfd = readDataFromFile(fileName, StandardCharsets.UTF_8);
        AppPackage appPackage = getAppPackage();
        appPackage.setDescriptorModel(vnfd);
        assertThat(packageService.savePackage(appPackage)).isNotNull();
        return appPackage;
    }

    private AppPackage setUpAppPackageWithHelmfile() throws IOException {
        String vnfd = readDataFromFile("vnfd/valid_vnfd.yaml", StandardCharsets.UTF_8);
        AppPackage appPackage = getAppPackage();
        appPackage.setDescriptorModel(vnfd);
        appPackage.setHelmfile(HELMFILE_CONTENT);
        assertThat(packageService.savePackage(appPackage)).isNotNull();
        return appPackage;
    }

    public void shouldDeleteChartUrls() throws InterruptedException {
        //Given
        AppPackage appPackage = getAppPackage();
        packageService.savePackage(appPackage);
        int deletedRow = chartUrlsRepository.deleteByAppPackage(appPackage);
        assertThat(deletedRow).isEqualTo(1);
        doThrow(new RuntimeException()).when(headChainOfResponsibility).handle(any());
        //When
        packageService.asyncPackageUpload(file.getOriginalFilename(), null, appPackage.getPackageId(), LocalDateTime.now().plusMinutes(15));
        //Then
        TimeUnit.SECONDS.sleep(1);
        List<ChartUrlsEntity> urlsByAppPackage = chartUrlsRepository.findByAppPackage(appPackage);
        assertThat(urlsByAppPackage.size()).isEqualTo(0);
        //Cleanup
        packageService.deletePackage(appPackage.getPackageId());
    }

    @Test
    @Transactional
    public void testAppPackageRepositoryOnlyRetrievesSetForDeletionPackages() {
        AppPackage appPackage1 = getAppPackage();
        appPackage1.setForDeletion(true);

        AppPackage appPackage2 = getAppPackage();
        appPackage2.setForDeletion(false);

        AppPackage firstPackage = appPackageRepository.save(appPackage1);
        assertThat(appPackageRepository.findByPackageId(firstPackage.getPackageId())).isNotNull();
        assertThat(appPackageRepository.findByPackageId(firstPackage.getPackageId()).get().isSetForDeletion()).isTrue();

        AppPackage secondPackage = appPackageRepository.save(appPackage2);
        assertThat(appPackageRepository.findByPackageId(secondPackage.getPackageId())).isNotNull();
        assertThat(appPackageRepository.findByPackageId(secondPackage.getPackageId()).get().isSetForDeletion()).isFalse();

        List<AppPackage> appPackageListForDeletion = packageService.getPackagesSetForDeletion();

        assertThat(appPackageListForDeletion).contains(firstPackage);
        assertThat(appPackageListForDeletion).doesNotContain(secondPackage);
    }


    @Test
    public void testDeletePackageSuccess() {
        AppPackage appPackage = getAppPackage();

        var initialPackage = appPackageRepository.save(appPackage);

        packageService.deletePackage(initialPackage.getPackageId());
        packageService.removeAppPackageWithResources(initialPackage.getPackageId());


        var appPackageAfterMethodExecution = appPackageRepository.findByPackageId(initialPackage.getPackageId());

        assertThat(appPackageAfterMethodExecution.isPresent()).isFalse();
    }

    @Test
    public void testListPackagesShouldReturnCorrectPageData() {
        Pageable pageable = Pageable.ofSize(2);
        Page<AppPackageResponse> appPackagePage = packageService.listPackages(pageable);

        assertPage(appPackagePage, pageable);
    }

    @Test
    public void testListPackagesWithFilterShouldReturnCorrectPageData() {
        Pageable pageable = Pageable.ofSize(1);
        Page<AppPackageResponse> appPackagePage =
                packageService.listPackages("(eq,packages/appProductName,SGSN-MME)", pageable);

        assertPage(appPackagePage, pageable);
    }

    private static void assertPage(Page<?> page, Pageable pageable) {
        assertThat(page.getContent()).hasSize(pageable.getPageSize());
        assertThat(page.getTotalElements()).isGreaterThan(pageable.getPageSize());
        assertThat(page.getTotalPages()).isGreaterThan(pageable.getPageSize());
        assertThat(page.getNumber()).isEqualTo(pageable.getPageNumber());
    }

    private byte[] getHelmfileContent() throws URISyntaxException, IOException {
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


