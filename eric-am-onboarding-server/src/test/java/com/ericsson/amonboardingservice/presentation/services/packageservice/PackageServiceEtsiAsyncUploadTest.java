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

import com.ericsson.am.shared.vnfd.model.HelmChart;
import com.ericsson.am.shared.vnfd.model.HelmChartType;
import com.ericsson.am.shared.vnfd.model.ImageDetails;
import com.ericsson.am.shared.vnfd.model.VnfDescriptorDetails;
import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.Flavour;
import com.ericsson.am.shared.vnfd.model.servicemodel.ServiceModel;
import com.ericsson.am.shared.vnfd.service.ToscaoServiceImpl;
import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.TestUtils;
import com.ericsson.amonboardingservice.aspect.AsyncExecutionTrackerAspect;
import com.ericsson.amonboardingservice.model.AppPackageResponse;
import com.ericsson.amonboardingservice.model.CreateVnfPkgInfoRequest;
import com.ericsson.amonboardingservice.model.HelmPackage;
import com.ericsson.amonboardingservice.presentation.exceptions.ChartOnboardingException;
import com.ericsson.amonboardingservice.presentation.exceptions.ObjectStorageException;
import com.ericsson.amonboardingservice.presentation.models.Chart;
import com.ericsson.amonboardingservice.presentation.models.OnboardingDetail;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.models.vnfd.OperationDetailEntity;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.services.ToscaHelper;
import com.ericsson.amonboardingservice.presentation.services.auditservice.AuditService;
import com.ericsson.amonboardingservice.presentation.services.dockerservice.DockerService;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.ericsson.amonboardingservice.presentation.services.helmservice.HelmChartStatus;
import com.ericsson.amonboardingservice.presentation.services.helmservice.HelmService;
import com.ericsson.amonboardingservice.presentation.services.objectstorage.ObjectStorageServiceImpl;
import com.ericsson.amonboardingservice.presentation.services.onboarding.request.OnboardingSynchronization;
import com.ericsson.amonboardingservice.presentation.services.onboarding.request.PackageUploadRequestContext;
import com.ericsson.amonboardingservice.presentation.services.onboarding.request.ToscaValidation;
import com.ericsson.amonboardingservice.presentation.services.onboarding.request.ToscaVersionIdentification;
import com.ericsson.amonboardingservice.presentation.services.supportedoperationservice.SupportedOperationServiceImpl;
import com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaService;
import com.ericsson.amonboardingservice.presentation.services.vnfdservice.VnfdService;
import com.ericsson.amonboardingservice.utils.Constants;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.ericsson.amonboardingservice.TestUtils.readDataFromFile;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_DEFINITIONS;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_IMAGES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;

@SpringBootTest()
@ActiveProfiles("test")
@TestPropertySource(
        properties = {
                "onboarding.skipToscaoValidation=false",
                "url=",
                "onboarding.highAvailabilityMode=true",
                "onboarding.logConcurrentRequests=true"
        })
public class PackageServiceEtsiAsyncUploadTest extends AbstractDbSetupTest {
    private static final String FAKE_CSAR_FILE = "fakeCsar.zip";
    private static final String FAKE_CSAR_ORIGINAL_FILE_NAME = "fakeCsar.csar";
    private static final String VNF_SOFTWARE_VERSION = "softwareVersion";
    private static final String VNF_DESCRIPTOR_ID = "newVNFDID";
    private static final String VNF_DESCRIPTOR_VERSION = "version";
    private static final String CHART_FILE_NAME = "sampledescriptor-0.0.1-223.tgz";
    private static final String CHART_NAME = "sampledescriptor";
    private static final String CHART_VERSION = "0.0.1-223";
    private static final String SERVICE_MODEL_ID = "service-model-id";
    private static final String CHART_ONBOARDING_ERROR_MESSAGE = "Failed to upload chart to registry";
    private static final String ERROR_DETAILS_WITH_EXCEPTION_MESSAGE_TEMPLATE = Constants.ERROR_DETAILS_TEMPLATE + "\n%s";


    @TempDir
    public Path tempFolder;

    @Autowired
    private PackageService packageService;

    @Autowired
    private AppPackageRepository appPackageRepository;

    @SpyBean
    private AsyncExecutionTrackerAspect asyncExecutionTrackerAspect;

    @MockBean
    private FileService fileService;

    @MockBean
    private ObjectStorageServiceImpl objectStorageService;

    @MockBean
    private HelmService helmService;

    @MockBean
    private DockerService dockerService;

    @MockBean
    private VnfdService vnfdService;

    @MockBean
    private AuditService auditService;

    @MockBean
    private ToscaMetaService toscaMetaService;

    @Autowired
    ToscaHelper toscaHelper;
    @Autowired
    ToscaValidation toscaValidation;

    @MockBean
    private SupportedOperationServiceImpl supportedOperationService;

    @MockBean
    private ToscaoServiceImpl toscaoService;

    @MockBean
    private ToscaVersionIdentification toscaVersionIdentification;

    @MockBean
    private OnboardingSynchronization onboardingSynchronization;


    @Test
    public void testEtsiAsyncOnboardingWithSimpleTosca1v2Vnfd()
    throws IOException, InterruptedException, URISyntaxException {
        // given
        String packageId = createVnfPackage();
        PackageArtifactPaths packageArtifacts = buildPackageArtifacts();
        Map<String, Path> artifactsPaths = buildArtifactPaths(packageArtifacts);
        VnfDescriptorDetails descriptorDetails = buildVnfDescriptorDetails(packageArtifacts);
        mockServicesForSuccessfulOnboardingOfTosca1v2Package(
                packageId, descriptorDetails, packageArtifacts, artifactsPaths);
        createOnboardingDetails(packageId);

        // when
        packageService.asyncPackageUpload(FAKE_CSAR_ORIGINAL_FILE_NAME, packageArtifacts.csarPath, packageId, getTimeoutDate());
        waitForPackageOnboardingToComplete();

        // then
        verifyAppPackageFields(packageId, OnboardingPackageState.ONBOARDED_NOT_IN_USE_ENABLED, null);
        verifyAppPackageResponseFields(packageId, 1);
        verifyPackageHasNotBeenUploadedToToscaO();
        verifySupportedOperationsHaveBeenParsed();
        verify(objectStorageService, times(1)).deleteFile(any(String.class));

        // cleanup
        cleanupPackage(packageId);
    }

    @Test
    public void testEtsiAsyncOnboardingWithSimpleTosca1v3Vnfd()
    throws IOException, InterruptedException, URISyntaxException {
        // given
        String packageId = createVnfPackage();
        PackageArtifactPaths packageArtifacts = buildPackageArtifacts();
        Map<String, Path> artifactsPaths = buildArtifactPaths(packageArtifacts);
        VnfDescriptorDetails descriptorDetails = buildVnfDescriptorDetails(packageArtifacts);
        mockServicesForSuccessfulOnboardingOfTosca1v3Package(
                packageId, descriptorDetails, packageArtifacts, artifactsPaths);
        createOnboardingDetails(packageId);

        // when
        packageService.asyncPackageUpload(FAKE_CSAR_ORIGINAL_FILE_NAME, packageArtifacts.csarPath, packageId, getTimeoutDate());
        waitForPackageOnboardingToComplete();

        // then
        verifyAppPackageFields(packageId, OnboardingPackageState.ONBOARDED_NOT_IN_USE_ENABLED, null);
        verifyAppPackageResponseFields(packageId, 1);
        verifyPackageHasBeenUploadedToToscaO(1);
        verifySupportedOperationsHaveBeenParsed();
        verify(objectStorageService, times(1)).deleteFile(any(String.class));

        // cleanup
        cleanupPackage(packageId);
    }

    private void cleanupPackage(final String packageId) {
        packageService.deletePackage(packageId);
        packageService.removeAppPackageWithResources(packageId);
    }

    @Test
    public void testEtsiAsyncOnboardingWithNestedTosca1v2Vnfd()
    throws IOException, InterruptedException, URISyntaxException {
        // given
        String packageId = createVnfPackage();
        PackageArtifactPaths packageArtifacts = buildPackageArtifacts();
        Map<String, Path> artifactsPaths = buildArtifactPaths(packageArtifacts);
        VnfDescriptorDetails descriptorDetails = buildVnfDescriptorDetailsWithFlavour(packageArtifacts);
        mockServicesForSuccessfulOnboardingOfTosca1v2Package(
                packageId, descriptorDetails, packageArtifacts, artifactsPaths);
        createOnboardingDetails(packageId);

        // when
        packageService.asyncPackageUpload(FAKE_CSAR_ORIGINAL_FILE_NAME, packageArtifacts.csarPath, packageId, getTimeoutDate());
        waitForPackageOnboardingToComplete();

        // then
        verifyAppPackageFields(packageId, OnboardingPackageState.ONBOARDED_NOT_IN_USE_ENABLED, null);
        verifyAppPackageResponseFields(packageId, 0);
        verifyPackageHasNotBeenUploadedToToscaO();
        verifySupportedOperationsHaveBeenParsed();
        verify(objectStorageService, times(1)).deleteFile(any(String.class));

        // cleanup
        cleanupPackage(packageId);
    }

    @Test
    public void testEtsiAsyncOnboardingErrorWhenOnboardingChartsAndUploadAgainForTosca1v2Csar()
    throws IOException, URISyntaxException, InterruptedException {
        // given
        String packageId = createVnfPackage();
        PackageArtifactPaths packageArtifacts = buildPackageArtifacts();
        Map<String, Path> artifactsPaths = buildArtifactPaths(packageArtifacts);
        VnfDescriptorDetails descriptorDetails = buildVnfDescriptorDetails(packageArtifacts);
        mockServicesForFailedAndThenSuccessfulOnboardingOfTosca1v2Package(
                packageId, descriptorDetails, packageArtifacts, artifactsPaths);
        createOnboardingDetails(packageId);

        // when
        packageService.asyncPackageUpload(FAKE_CSAR_ORIGINAL_FILE_NAME, packageArtifacts.csarPath, packageId, getTimeoutDate());
        waitForPackageOnboardingToComplete();

        // then
        String expectedErrorMessage = String.format(ERROR_DETAILS_WITH_EXCEPTION_MESSAGE_TEMPLATE, "Charts onboarding",
                CHART_ONBOARDING_ERROR_MESSAGE);
        verifyAppPackageFields(packageId, OnboardingPackageState.ERROR_NOT_IN_USE_DISABLED, expectedErrorMessage);
        verifyPackageHasNotBeenUploadedAndRemovedFromToscaO();
        verifySupportedOperationsHaveBeenDeleted(packageId);
        verify(objectStorageService, times(1)).deleteFile(any(String.class));

        // given
        createOnboardingDetails(packageId);

        // when
        packageService.asyncPackageUpload(FAKE_CSAR_ORIGINAL_FILE_NAME, packageArtifacts.csarPath, packageId, getTimeoutDate());
        waitForPackageOnboardingToComplete();

        // then
        verifyAppPackageFields(packageId, OnboardingPackageState.ONBOARDED_NOT_IN_USE_ENABLED, null);
        verifyAppPackageResponseFields(packageId, 1);
        verifyPackageHasNotBeenUploadedToToscaO();
        verifySupportedOperationsHaveBeenParsed();
        verify(objectStorageService, times(2)).deleteFile(any(String.class));

        // cleanup
        cleanupPackage(packageId);
    }

    @Test
    public void testAsyncOnboardingErrorWhenFailedToUploadHelmChartByTimeout() throws IOException, URISyntaxException,
            InterruptedException {
        // given
        String packageId = createVnfPackage();
        PackageArtifactPaths packageArtifacts = buildPackageArtifacts();
        Map<String, Path> artifactsPaths = buildArtifactPaths(packageArtifacts);
        VnfDescriptorDetails descriptorDetails = buildVnfDescriptorDetails(packageArtifacts);
        mockServicesForFailedOnboardingOfTosca1v2PackageByTimeout(
                packageId, descriptorDetails, packageArtifacts, artifactsPaths);
        createOnboardingDetails(packageId);

        // when
        packageService.asyncPackageUpload(FAKE_CSAR_ORIGINAL_FILE_NAME, packageArtifacts.csarPath, packageId, LocalDateTime.now());
        waitForPackageOnboardingToComplete();

        // then
        String expectedErrorMessage = String.format(Constants.ERROR_DETAILS_TEMPLATE, "Charts onboarding");
        verifyAppPackageFields(packageId, OnboardingPackageState.ERROR_NOT_IN_USE_DISABLED, expectedErrorMessage);
        verifyPackageHasNotBeenUploadedAndRemovedFromToscaO();
        verifySupportedOperationsHaveBeenDeleted(packageId);
        verify(objectStorageService, times(1)).deleteFile(any(String.class));

        // cleanup
        cleanupPackage(packageId);
    }

    @Test
    public void testEtsiAsyncOnboardingErrorWhenOnboardingChartsAndUploadAgainForTosca1v3Csar()
    throws IOException, InterruptedException, URISyntaxException {
        // given
        String packageId = createVnfPackage();
        PackageArtifactPaths packageArtifacts = buildPackageArtifacts();
        Map<String, Path> artifactsPaths = buildArtifactPaths(packageArtifacts);
        VnfDescriptorDetails descriptorDetails = buildVnfDescriptorDetails(packageArtifacts);
        mockServicesForFailedAndThenSuccessfulOnboardingOfTosca1v3Package(
                packageId, descriptorDetails, packageArtifacts, artifactsPaths);
        createOnboardingDetails(packageId);

        // when
        packageService.asyncPackageUpload(FAKE_CSAR_ORIGINAL_FILE_NAME, packageArtifacts.csarPath, packageId, getTimeoutDate());
        waitForPackageOnboardingToComplete();

        // then
        String expectedErrorMessage = String.format(ERROR_DETAILS_WITH_EXCEPTION_MESSAGE_TEMPLATE, "Charts onboarding",
                CHART_ONBOARDING_ERROR_MESSAGE);
        verifyAppPackageFields(packageId, OnboardingPackageState.ERROR_NOT_IN_USE_DISABLED, expectedErrorMessage);
        verifyPackageHasBeenUploadedAndRemovedFromToscaO();
        verifySupportedOperationsHaveBeenDeleted(packageId);
        verify(objectStorageService, times(1)).deleteFile(any(String.class));

        // given
        createOnboardingDetails(packageId);

        // when
        packageService.asyncPackageUpload(FAKE_CSAR_ORIGINAL_FILE_NAME, packageArtifacts.csarPath, packageId, getTimeoutDate());
        waitForPackageOnboardingToComplete();

        // then
        verifyAppPackageFields(packageId, OnboardingPackageState.ONBOARDED_NOT_IN_USE_ENABLED, null);
        verifyAppPackageResponseFields(packageId, 1);
        verifyPackageHasBeenUploadedToToscaO(2);
        verifySupportedOperationsHaveBeenParsed();
        verify(objectStorageService, times(2)).deleteFile(any(String.class));

        // cleanup
        cleanupPackage(packageId);
    }

    @Test
    public void testEtsiAsyncOnboardingFromInputStreamAPI()
    throws IOException, InterruptedException, URISyntaxException {
        // given
        String packageId = createVnfPackage();
        PackageArtifactPaths packageArtifacts = buildPackageArtifacts();
        String filename = packageArtifacts.csarPath.toString();
        createOnboardingDetails(packageId);

        Map<String, Path> artifactsPaths = buildArtifactPaths(packageArtifacts);
        VnfDescriptorDetails descriptorDetails = buildVnfDescriptorDetails(packageArtifacts);
        mockServicesForSuccessfulOnboardingOfTosca1v2Package(
                packageId, descriptorDetails, packageArtifacts, artifactsPaths);

        when(fileService.create(any(), any())).thenReturn(packageArtifacts.csarPath);
        when(fileService.isFileExist(any())).thenReturn(false);
        // when
        packageService.asyncPackageUploadFromObjectStorage(filename, FAKE_CSAR_ORIGINAL_FILE_NAME,packageId, getTimeoutDate());
        waitForPackageOnboardingToComplete();

        // then
        verify(objectStorageService, times(1)).downloadFile(any(),eq(filename));
        verifyAppPackageFields(packageId, OnboardingPackageState.ONBOARDED_NOT_IN_USE_ENABLED, null);
        verifyAppPackageResponseFields(packageId, 1);
        verifyPackageHasNotBeenUploadedToToscaO();
        verifySupportedOperationsHaveBeenParsed();
        verify(objectStorageService, times(1)).deleteFile(any(String.class));

        // cleanup
        cleanupPackage(packageId);
    }

    @Test
    public void testAsyncOnboardingFromInputStreamAPIObjectStoreDownloadException() throws InterruptedException{
        // given
        String packageId = createVnfPackage();

        when(fileService.create(any(), any())).thenReturn(Path.of("dummy/dummy_filename"));
        doThrow(new ObjectStorageException("dummy exception"))
                .when(objectStorageService).downloadFile(any(), any());

        // when
        packageService.asyncPackageUploadFromObjectStorage("dummy/dummy_filename", FAKE_CSAR_ORIGINAL_FILE_NAME,
                                                           packageId, getTimeoutDate());
        waitForPackageOnboardingToComplete();
        // then
        verifyAppPackageFields(packageId, OnboardingPackageState.ERROR_NOT_IN_USE_DISABLED,
                               String.format(ERROR_DETAILS_WITH_EXCEPTION_MESSAGE_TEMPLATE, "package uploading", "dummy exception"));
        verify(objectStorageService, times(1)).deleteFile(any(String.class));

        // cleanup
        cleanupPackage(packageId);

        verify(asyncExecutionTrackerAspect, times(1)).beforeAdvice(any());
        verify(asyncExecutionTrackerAspect, times(1)).afterAdvice(any());
        assertThat(asyncExecutionTrackerAspect.getActiveAsyncThreads()).isEqualTo(0);
    }

    @Test
    public void testAsyncOnboardingFromInputStreamAPIRelativePathError() throws InterruptedException{
        // given
        String packageId = createVnfPackage();
        String filePath = "dummy" + File.separator + FAKE_CSAR_FILE;

        when(fileService.create(any(), any())).thenReturn(Path.of(filePath));
        doThrow(new IllegalArgumentException(Constants.ZIP_SLIP_ATTACK_ERROR_MESSAGE))
                .when(fileService).validateCsarForRelativePaths(eq(Path.of(filePath)), eq(9));
        // when
        packageService.asyncPackageUploadFromObjectStorage(filePath, FAKE_CSAR_ORIGINAL_FILE_NAME,
                                                           packageId, getTimeoutDate());
        waitForPackageOnboardingToComplete();
        // then
        verifyAppPackageFields(packageId, OnboardingPackageState.ERROR_NOT_IN_USE_DISABLED,
                               String.format(ERROR_DETAILS_WITH_EXCEPTION_MESSAGE_TEMPLATE, "package uploading",
                                       Constants.ZIP_SLIP_ATTACK_ERROR_MESSAGE));
        verify(objectStorageService, times(1)).deleteFile(any(String.class));
        verify(fileService).validateCsarForRelativePaths(eq(Path.of(filePath)), eq(9));
        // cleanup
        cleanupPackage(packageId);
    }

    private void verifyPackageHasNotBeenUploadedToToscaO() {
        verify(toscaoService, times(0)).uploadPackageToToscao(any(Path.class));
    }

    private void verifyPackageHasBeenUploadedToToscaO(int expectedNumberOfInvocations) {
        verify(toscaoService, times(expectedNumberOfInvocations)).uploadPackageToToscao(any(Path.class));
    }

    private void verifyPackageHasBeenUploadedAndRemovedFromToscaO() {
        verify(toscaoService, times(1)).uploadPackageToToscao(any(Path.class));
        verify(toscaoService, times(1)).deleteServiceModel(anyString());
    }

    private void verifyPackageHasNotBeenUploadedAndRemovedFromToscaO() {
        verify(toscaoService, never()).uploadPackageToToscao(any(Path.class));
        verify(toscaoService, never()).deleteServiceModel(anyString());
    }

    private void verifySupportedOperationsHaveBeenParsed() {
        verify(supportedOperationService, times(1)).parsePackageSupportedOperations(any(AppPackage.class));
    }

    private void verifySupportedOperationsHaveBeenDeleted(final String packageId) {
        verify(supportedOperationService, times(1)).deleteSupportedOperations(packageId);
    }

    private void verifyAppPackageFields(final String packageId, final OnboardingPackageState expectedPackageState,
                                        final String expectedErrorDetails) {
        AppPackage appPackage = appPackageRepository.findByPackageId(packageId).get();

        assertThat(appPackage.getOnboardingState()).isEqualTo(expectedPackageState.getOnboardingState());
        assertThat(appPackage.getUsageState()).isEqualTo(expectedPackageState.getUsageState());
        assertThat(appPackage.getOperationalState()).isEqualTo(expectedPackageState.getOperationalState());
        assertThat(appPackage.getErrorDetails()).isEqualTo(expectedErrorDetails);
        if (appPackage.getOnboardingState() == AppPackage.OnboardingStateEnum.ONBOARDED) {
            assertThat(appPackage.getChecksum()).isNotNull().isNotEmpty();
        } else {
            assertThat(appPackage.getChecksum()).isNull();
        }
    }

    private void verifyAppPackageResponseFields(final String packageId, final int expectedHelmPriority) {
        AppPackageResponse appPackageResponse = packageService.getPackage(packageId).get();

        assertThat(appPackageResponse.getAppDescriptorId()).isEqualTo(VNF_DESCRIPTOR_ID);
        assertThat(appPackageResponse.getDescriptorModel()).hasToString(
                "{node_types={Ericsson.SGSN-MME.1_20_CXS101289_R81E08.cxp9025898_4r81e08=tests}}");
        assertThat(appPackageResponse.getOnboardingState()).isEqualTo(AppPackage.OnboardingStateEnum.ONBOARDED.toString());
        assertThat(appPackageResponse.getAppSoftwareVersion()).isEqualTo(VNF_SOFTWARE_VERSION);
        assertThat(appPackageResponse.getDescriptorVersion()).isEqualTo(VNF_DESCRIPTOR_VERSION);

        HelmPackage helmPackage = appPackageResponse.getHelmPackageUrls().get(0);
        assertThat(helmPackage.getPriority()).isEqualTo(expectedHelmPriority);
        assertThat(helmPackage.getChartUrl()).isEqualTo(CHART_FILE_NAME);
        assertThat(helmPackage.getChartName()).isEqualTo(CHART_NAME);
        assertThat(helmPackage.getChartVersion()).isEqualTo(CHART_VERSION);
    }

    private String createVnfPackage() {
        CreateVnfPkgInfoRequest createVnfPkgInfoRequest = new CreateVnfPkgInfoRequest();
        return packageService.createVnfPackage(createVnfPkgInfoRequest).getId();
    }


    private void mockServicesForSuccessfulOnboardingOfTosca1v2Package(
            final String packageId, final VnfDescriptorDetails descriptorDetails,
            final PackageArtifactPaths packageArtifactPaths, final Map<String, Path> artifactPaths) throws IOException, URISyntaxException {
        mockServices(packageId, descriptorDetails, packageArtifactPaths, artifactPaths);
        JSONObject descriptor = new JSONObject(readDataFromFile("valid_vnfd.json", StandardCharsets.UTF_8));
        when(vnfdService.getVnfDescriptor(any())).thenReturn(descriptor);
        when(vnfdService.getDescriptorId(any())).thenReturn(VNF_DESCRIPTOR_ID);
        when(toscaoService.getServiceModelByDescriptorId(any())).thenReturn(Optional.of(buildServiceModel()));
        when(toscaoService.uploadPackageToToscao(any())).thenReturn(Optional.of(buildServiceModel()));
        when(toscaMetaService.getArtifactsMapFromToscaMetaFile(any(Path.class))).thenReturn(artifactPaths);
    }

    private void mockServicesForSuccessfulOnboardingOfTosca1v3Package(
            final String packageId, final VnfDescriptorDetails descriptorDetails,
            final PackageArtifactPaths packageArtifactPaths, final Map<String, Path> artifactPaths) throws IOException, URISyntaxException {
        mockServices(packageId, descriptorDetails, packageArtifactPaths, artifactPaths);
        JSONObject descriptor = new JSONObject(readDataFromFile("valid_vnfd_1_3.json", StandardCharsets.UTF_8));
        when(vnfdService.getVnfDescriptor(any())).thenReturn(descriptor);
        when(vnfdService.getDescriptorId(any())).thenReturn(VNF_DESCRIPTOR_ID);
        when(toscaoService.getServiceModelByDescriptorId(any())).thenReturn(Optional.empty());
        when(toscaoService.uploadPackageToToscao(any())).thenReturn(Optional.of(buildServiceModel()));
        when(toscaMetaService.getArtifactsMapFromToscaMetaFile(any(Path.class))).thenReturn(artifactPaths);
    }

    private void mockServicesForFailedAndThenSuccessfulOnboardingOfTosca1v2Package(
            final String packageId, final VnfDescriptorDetails descriptorDetails,
            final PackageArtifactPaths packageArtifactPaths, final Map<String, Path> artifactPaths) throws IOException, URISyntaxException {
        mockServices(packageId, descriptorDetails, packageArtifactPaths, artifactPaths);
        JSONObject descriptor = new JSONObject(readDataFromFile("valid_vnfd.json", StandardCharsets.UTF_8));
        when(vnfdService.getVnfDescriptor(any())).thenReturn(descriptor);
        when(vnfdService.getDescriptorId(any())).thenReturn(VNF_DESCRIPTOR_ID);
        when(toscaoService.getServiceModelByDescriptorId(any())).thenReturn(Optional.empty());
        when(toscaMetaService.getArtifactsMapFromToscaMetaFile(any(Path.class))).thenReturn(artifactPaths);
        when(helmService.uploadNewCharts(anyMap(), any(LocalDateTime.class)))
                .thenThrow(new ChartOnboardingException(CHART_ONBOARDING_ERROR_MESSAGE))
                .thenReturn(buildChartUploadResponseMap());
    }

    private void mockServicesForFailedOnboardingOfTosca1v2PackageByTimeout(
            final String packageId, final VnfDescriptorDetails descriptorDetails,
            final PackageArtifactPaths packageArtifactPaths, final Map<String, Path> artifactPaths) throws IOException, URISyntaxException {
        mockServices(packageId, descriptorDetails, packageArtifactPaths, artifactPaths);
        JSONObject descriptor = new JSONObject(readDataFromFile("valid_vnfd.json", StandardCharsets.UTF_8));
        when(vnfdService.getVnfDescriptor(any())).thenReturn(descriptor);
        when(vnfdService.getDescriptorId(any())).thenReturn(VNF_DESCRIPTOR_ID);
        when(toscaoService.getServiceModelByDescriptorId(any())).thenReturn(Optional.empty());
        when(toscaMetaService.getArtifactsMapFromToscaMetaFile(any(Path.class))).thenReturn(artifactPaths);
        Map<Path, ResponseEntity<String>> chartUploadResponseMap = Map.of(
                Path.of("/local/tmp/some_helm_chart.tgz"), new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
        when(helmService.uploadNewCharts(anyMap(), any(LocalDateTime.class)))
                .thenReturn(chartUploadResponseMap);
    }

    private void mockServicesForFailedAndThenSuccessfulOnboardingOfTosca1v3Package(
            final String packageId, final VnfDescriptorDetails descriptorDetails,
            final PackageArtifactPaths packageArtifactPaths, final Map<String, Path> artifactPaths) throws IOException {
        mockServices(packageId, descriptorDetails, packageArtifactPaths, artifactPaths);
        JSONObject descriptor = new JSONObject(readDataFromFile("valid_vnfd_1_3.json", StandardCharsets.UTF_8));

        when(vnfdService.getVnfDescriptor(any())).thenReturn(descriptor);
        when(vnfdService.getDescriptorId(any())).thenReturn(VNF_DESCRIPTOR_ID);
        when(toscaoService.getServiceModelByDescriptorId(any())).thenReturn(Optional.empty());
        when(toscaoService.uploadPackageToToscao(any())).thenReturn(Optional.of(buildServiceModel()));

        when(helmService.uploadNewCharts(anyMap(), any(LocalDateTime.class)))
                .thenThrow(new ChartOnboardingException(CHART_ONBOARDING_ERROR_MESSAGE))
                .thenReturn(buildChartUploadResponseMap());
    }


    private void mockServices(final String packageId,
                              final VnfDescriptorDetails descriptorDetails,
                              final PackageArtifactPaths packageArtifactPaths,
                              final Map<String, Path> artifactPaths) throws IOException {
        when(vnfdService.validateAndGetVnfDescriptorDetails(anyMap())).thenReturn(descriptorDetails);
        when(vnfdService.getVnfDescriptor(any(Path.class))).thenReturn(new JSONObject());

        when(helmService.checkChartPresent(any(Path.class))).thenReturn(HelmChartStatus.PRESENT);
        when(helmService.getChartInstallUrl(anyString())).thenReturn(CHART_FILE_NAME);
        when(helmService.checkChartPresent(anyCollection())).thenReturn(new HashMap<>());
        when(helmService.getChartInstallUrl(anyString())).thenReturn(CHART_FILE_NAME);

        when(fileService.createDirectory(anyString())).thenReturn(packageArtifactPaths.csarPath);
        when(fileService.storeFile(any(InputStream.class), any(Path.class), anyString())).thenReturn(packageArtifactPaths.csarPath);
        when(fileService.zipDirectory(any(Path.class))).thenReturn(packageArtifactPaths.vnfdArchivePath);
        when(fileService.readFile(any())).thenReturn(StringUtils.EMPTY);

        when(auditService.saveChart(any(Chart.class))).thenReturn(null);

        when(dockerService.onboardDockerTar(any(Path.class), any(LocalDateTime.class))).thenReturn(Collections.emptyList());
        doNothing().when(dockerService).removeDockerImagesByPackageId(anyString());

        when(toscaMetaService.getArtifactsMapFromToscaMetaFile(any(Path.class))).thenReturn(artifactPaths);
        when(toscaMetaService.getToscaMetaPath(any(Path.class))).thenReturn(packageArtifactPaths.toscaMetaPath);

        doNothing().when(toscaVersionIdentification).handle(any(PackageUploadRequestContext.class));

        when(toscaoService.uploadPackageToToscao(any(Path.class))).thenReturn(Optional.of(buildServiceModel()));
        when(toscaoService.deleteServiceModel(SERVICE_MODEL_ID)).thenReturn(true);

        when(supportedOperationService.parsePackageSupportedOperations(any(AppPackage.class)))
                .thenReturn(buildOperationDetails(appPackageRepository.findByPackageId(packageId).get()));
    }

    private List<OperationDetailEntity> buildOperationDetails(final AppPackage appPackage) {
        List<OperationDetailEntity> operationDetails = new ArrayList<>();
        for (LCMOperationsEnum operation : LCMOperationsEnum.values()) {
            if (operation != LCMOperationsEnum.CHANGE_CURRENT_PACKAGE) {
                operationDetails.add(buildOperationDetail(operation.getOperation(), appPackage));
            }
        }
        return operationDetails;
    }

    private OperationDetailEntity buildOperationDetail(final String operationName, final AppPackage appPackage) {
        OperationDetailEntity operationDetail = new OperationDetailEntity();
        operationDetail.setAppPackage(appPackage);
        operationDetail.setOperationName(operationName);
        operationDetail.setSupported(true);
        return operationDetail;
    }

    private VnfDescriptorDetails buildVnfDescriptorDetailsWithFlavour(final PackageArtifactPaths artifactsPath) {
        VnfDescriptorDetails vnfDescriptorDetails = buildVnfDescriptorDetails(artifactsPath);
        Map<String, Flavour> flavourMap = new HashMap<>();
        flavourMap.put("flavour-1", new Flavour());
        vnfDescriptorDetails.setFlavours(flavourMap);
        return vnfDescriptorDetails;
    }

    private VnfDescriptorDetails buildVnfDescriptorDetails(final PackageArtifactPaths artifactsPath) {
        List<HelmChart> helmCharts = new ArrayList<>();
        helmCharts.add(new HelmChart(artifactsPath.chartFile.toString(), HelmChartType.CNF, "helm_package1"));
        ImageDetails imageDetails = new ImageDetails(artifactsPath.dockerTarPath.toString());

        VnfDescriptorDetails vnfDescriptorDetails = new VnfDescriptorDetails();
        vnfDescriptorDetails.setHelmCharts(helmCharts);
        vnfDescriptorDetails.setVnfDescriptorId(VNF_DESCRIPTOR_ID);
        vnfDescriptorDetails.setVnfDescriptorVersion(VNF_DESCRIPTOR_VERSION);
        vnfDescriptorDetails.setVnfSoftwareVersion(VNF_SOFTWARE_VERSION);
        vnfDescriptorDetails.setDescriptorModel(
                "{\"node_types\": {\"Ericsson.SGSN-MME.1_20_CXS101289_R81E08.cxp9025898_4r81e08\": \"tests\"}}");
        vnfDescriptorDetails.setVnfProductName("product");
        vnfDescriptorDetails.setImagesDetails(Collections.singletonList(imageDetails));
        return vnfDescriptorDetails;
    }

    private Map<String, Path> buildArtifactPaths(final PackageArtifactPaths artifactsPath) {
        Map<String, Path> artifacts = new HashMap<>();
        artifacts.put(Constants.CSAR_DIRECTORY, tempFolder.getRoot());
        artifacts.put(Constants.PATH_TO_PACKAGE, artifactsPath.csarPath);
        artifacts.put(ENTRY_DEFINITIONS, artifactsPath.vnfdPath);
        artifacts.put(ENTRY_IMAGES, artifactsPath.dockerTarPath);
        return artifacts;
    }

    private PackageArtifactPaths buildPackageArtifacts() throws IOException, URISyntaxException {
        PackageArtifactPaths artifactsPath = new PackageArtifactPaths();

        artifactsPath.csarPath = Files.createFile(tempFolder.resolve(FAKE_CSAR_FILE));
        artifactsPath.chartFile = Files.createFile(tempFolder.resolve("helm-chart.tgz"));
        artifactsPath.vnfdPath = Files.createFile(tempFolder.resolve("vnfd.yaml"));
        artifactsPath.vnfdArchivePath = Files.createFile(tempFolder.resolve("vnfd.zip"));
        artifactsPath.toscaMetaPath = Files.createFile(tempFolder.resolve("TOSCA.meta"));
        artifactsPath.dockerTarPath = Files.createFile(tempFolder.resolve("docker.tar"));

        Files.copy(TestUtils.getResource(Constants.VALID_VNFD_FILE).toAbsolutePath(),
                   artifactsPath.vnfdPath, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(TestUtils.getResource(CHART_FILE_NAME).toAbsolutePath(),
                   artifactsPath.chartFile, StandardCopyOption.REPLACE_EXISTING);

        return artifactsPath;
    }

    private ServiceModel buildServiceModel() {
        ServiceModel serviceModel = new ServiceModel();
        serviceModel.setId(SERVICE_MODEL_ID);
        return serviceModel;
    }

    private Map<Path, ResponseEntity<String>> buildChartUploadResponseMap() {
        ResponseEntity<String> chartUploadResponse = new ResponseEntity<>("Chart uploaded successfully", OK);
        Map<Path, ResponseEntity<String>> responseMap = new HashMap<>();
        responseMap.put(Path.of("/local/tmp/some_helm_chart.tgz"), chartUploadResponse);
        return responseMap;
    }

    private void createOnboardingDetails(final String packageId) {
        final AppPackage appPackage = appPackageRepository.findByPackageId(packageId).orElseThrow();

        final OnboardingDetail onboardingDetail = new OnboardingDetail();
        onboardingDetail.setAppPackage(appPackage);
        onboardingDetail.setExpiredOnboardingTime(getTimeoutDate());

        appPackage.setOnboardingDetail(onboardingDetail);

        appPackageRepository.save(appPackage);
    }

    private void waitForPackageOnboardingToComplete() throws InterruptedException {
        TimeUnit.SECONDS.sleep(2);
    }

    private static LocalDateTime getTimeoutDate() {
        return LocalDateTime.now().plusMinutes(10);
    }

    private static class PackageArtifactPaths {
        Path csarPath;
        Path chartFile;
        Path vnfdPath;
        Path vnfdArchivePath;
        Path toscaMetaPath;
        Path dockerTarPath;
    }
}
