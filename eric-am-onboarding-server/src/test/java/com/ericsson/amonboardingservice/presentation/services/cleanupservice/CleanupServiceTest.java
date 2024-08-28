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
package com.ericsson.amonboardingservice.presentation.services.cleanupservice;

import static java.util.concurrent.TimeUnit.SECONDS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import static com.ericsson.amonboardingservice.TestUtils.getResource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.am.shared.vnfd.service.ToscaoService;
import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.presentation.models.ChartUrlsEntity;
import com.ericsson.amonboardingservice.presentation.models.OnboardingDetail;
import com.ericsson.amonboardingservice.presentation.models.OnboardingHeartbeat;
import com.ericsson.amonboardingservice.presentation.models.ServiceModelRecordEntity;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackageArtifacts;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackageDockerImage;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppUserDefinedData;
import com.ericsson.amonboardingservice.presentation.models.vnfd.OperationDetailEntity;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageArtifactsRepository;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageDockerImageRepository;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.repositories.ChartUrlsRepository;
import com.ericsson.amonboardingservice.presentation.repositories.OnboardingDetailRepository;
import com.ericsson.amonboardingservice.presentation.repositories.OnboardingHeartbeatRepository;
import com.ericsson.amonboardingservice.presentation.repositories.OperationDetailEntityRepository;
import com.ericsson.amonboardingservice.presentation.repositories.ServiceModelRecordRepository;
import com.ericsson.amonboardingservice.presentation.services.dockerservice.DockerService;
import com.ericsson.amonboardingservice.presentation.services.onboarding.request.PackageUploadRequestContext;
import com.ericsson.amonboardingservice.presentation.services.packageservice.OnboardingPackageState;
import com.ericsson.amonboardingservice.utils.Constants;
import com.ericsson.amonboardingservice.infrastructure.client.RestClient;

@SpringBootTest
@ActiveProfiles("test")
public class CleanupServiceTest extends AbstractDbSetupTest {

    @Autowired
    private CleanupService cleanupService;

    @Autowired
    private AppPackageRepository appPackageRepository;

    @Autowired
    private ServiceModelRecordRepository serviceModelRecordRepository;

    @Autowired
    private ChartUrlsRepository chartUrlsRepository;

    @Autowired
    private OperationDetailEntityRepository operationDetailEntityRepository;

    @Autowired
    private AppPackageArtifactsRepository appPackageArtifactsRepository;

    @Autowired
    private AppPackageDockerImageRepository appPackageDockerImageRepository;

    @Autowired
    private OnboardingDetailRepository onboardingDetailRepository;

    @Autowired
    private OnboardingHeartbeatRepository onboardingHeartbeatRepository;

    @MockBean
    private ToscaoService toscaoService;

    @MockBean
    private DockerService dockerService;

    @MockBean
    private RestClient restClient;

    private static Path helmChart;

    @BeforeEach
    public void setUp() throws URISyntaxException, IOException {
        await().atMost(10, SECONDS).until(this::heartbeatIsCreated);
        Path sampleDescriptorHelmChart = getResource("sampledescriptor-0.0.1-223.tgz");
        Path chart = Files.createTempFile("helmChart", ".tgz");
        Files.copy(sampleDescriptorHelmChart, chart, StandardCopyOption.REPLACE_EXISTING);
        helmChart = chart;
    }

    @Test
    public void shouldCleanupPackagesStuckInRecovery() {
        // given
        OnboardingDetail onboardingDetail = new OnboardingDetail();
        onboardingDetail.setExpiredOnboardingTime(LocalDateTime.now());

        AppPackage testAppPackage = new AppPackage();
        testAppPackage.setProvider("New");
        testAppPackage.setProductName("package");
        testAppPackage.setSoftwareVersion("110496ed-96fd-4d7a-8371-52db698e038f");
        testAppPackage.setOnboardingState(AppPackage.OnboardingStateEnum.UPLOADING);
        testAppPackage.setUsageState(AppPackage.AppUsageStateEnum.NOT_IN_USE);
        testAppPackage.setOperationalState(AppPackage.OperationalStateEnum.DISABLED);
        testAppPackage.setMultipleVnfd(false);
        testAppPackage.setForDeletion(false);
        testAppPackage.setOnboardingDetail(onboardingDetail);
        onboardingDetail.setAppPackage(testAppPackage);

        final String packageId = appPackageRepository.saveAndFlush(testAppPackage).getPackageId();

        String error = String.format("Can not recover package with id: %s due to %s",
                                     testAppPackage.getPackageId(),
                                     "Package context doesn't exist");

        // when 
        cleanupService.cleanupStuckPackage(packageId, error);

        // then
        // package stuck during recovery should be reset
        final Optional<AppPackage> optionalModifiedAppPackageStuckInRecovery = appPackageRepository.findByPackageId(packageId);

        assertThat(optionalModifiedAppPackageStuckInRecovery.isPresent()).isTrue();

        AppPackage modifiedAppPackageStuckInRecovery = optionalModifiedAppPackageStuckInRecovery.get();
        assertThat(modifiedAppPackageStuckInRecovery.getOnboardingState()).isEqualTo(AppPackage.OnboardingStateEnum.ERROR);
        assertThat(modifiedAppPackageStuckInRecovery.getUsageState()).isEqualTo(AppPackage.AppUsageStateEnum.NOT_IN_USE);
        assertThat(modifiedAppPackageStuckInRecovery.getOperationalState()).isEqualTo(AppPackage.OperationalStateEnum.DISABLED);

        assertThat(serviceModelRecordRepository.findByAppPackage(testAppPackage)).isEmpty();
        assertThat(chartUrlsRepository.findByAppPackage(testAppPackage)).isEmpty();
        assertThat(appPackageDockerImageRepository.findByAppPackage(testAppPackage)).isEmpty();
        assertThat(operationDetailEntityRepository.findByAppPackagePackageId(testAppPackage.getPackageId())).isEmpty();
        assertThat(appPackageArtifactsRepository.findByAppPackage(testAppPackage)).isEmpty();
        assertThat(onboardingDetailRepository.findByAppPackage(testAppPackage)).isEmpty();

        assertThat(modifiedAppPackageStuckInRecovery.getDescriptorId()).isNull();
        assertThat(modifiedAppPackageStuckInRecovery.getDescriptorVersion()).isEqualTo(Constants.DEFAULT_PACKAGE_DESCRIPTOR_VERSION);
        assertThat(modifiedAppPackageStuckInRecovery.getDescriptorModel()).isNull();
        assertThat(modifiedAppPackageStuckInRecovery.getProvider()).isEqualTo(Constants.DEFAULT_PACKAGE_PROVIDER_NAME);
        assertThat(modifiedAppPackageStuckInRecovery.getProductName()).isEqualTo(Constants.DEFAULT_PACKAGE_PRODUCT_NAME);
        assertThat(modifiedAppPackageStuckInRecovery.getSoftwareVersion()).isEqualTo(testAppPackage.getPackageId());
        assertThat(modifiedAppPackageStuckInRecovery.getChecksum()).isNull();
        assertThat(modifiedAppPackageStuckInRecovery.getFiles()).isNull();
        assertThat(modifiedAppPackageStuckInRecovery.getHelmfile()).isNull();
        assertThat(modifiedAppPackageStuckInRecovery.getErrorDetails()).isEqualTo(error);
        assertThat(modifiedAppPackageStuckInRecovery.getVnfdZip()).isNull();
        assertThat(modifiedAppPackageStuckInRecovery.isMultipleVnfd()).isFalse();
        assertThat(modifiedAppPackageStuckInRecovery.getPackageSecurityOption()).isNull();

    }

    @Test
    @Transactional
    public void shouldDeletePackagesWithFlagSetForDeletionWhenNoResources() {
        // given
        AppPackage beforeMethod = createPackageSetForDeletion();

        final AppPackage appPackage = appPackageRepository.save(beforeMethod);

        final AppPackage appPackageFromRepoBeforeMethod = appPackageRepository.findByPackageId(appPackage.getPackageId()).get();

        assertThat(appPackageFromRepoBeforeMethod.isSetForDeletion()).isTrue();
        assertThat(appPackageFromRepoBeforeMethod.getAppPackageDockerImages()).isNull();
        assertThat(appPackageFromRepoBeforeMethod.getChartsRegistryUrl()).isNull();
        assertThat(appPackageFromRepoBeforeMethod.getServiceModelRecordEntity()).isNull();
        assertThat(appPackageRepository.findAllByIsSetForDeletionIsTrue().size()).isEqualTo(1);

        cleanupService.cleanupPackagesSetForDeletion();

        // then
        await().atMost(10, SECONDS).untilAsserted(() -> {
            final Optional<AppPackage> appPackageShouldAppearDeleted=
                    appPackageRepository.findByPackageId(appPackage.getPackageId());
            assertThat(appPackageShouldAppearDeleted).isEmpty();
            assertThat(serviceModelRecordRepository.findByAppPackage(appPackage)).isEmpty();
            assertThat(chartUrlsRepository.findByAppPackage(appPackage)).isEmpty();
            assertThat(appPackageDockerImageRepository.findByAppPackage(appPackage)).isEmpty();
            assertThat(operationDetailEntityRepository.findByAppPackagePackageId(appPackage.getPackageId())).isEmpty();
            assertThat(appPackageArtifactsRepository.findByAppPackage(appPackage)).isEmpty();
            assertThat(onboardingDetailRepository.findByAppPackage(appPackage)).isEmpty();
        });
    }

    @Test
    public void shouldDeletePackagesWithFlagSetForDeletionWithResources() {
        // given
        AppPackage beforeMethod = createPackageWithServiceModelChartsAndRegistry();

        final AppPackage appPackage = appPackageRepository.save(beforeMethod);
        assertThat(appPackageRepository.findByPackageId(appPackage.getPackageId()).get().isSetForDeletion()).isTrue();

        assertThat(appPackageRepository.findAllByIsSetForDeletionIsTrue()).isNotEmpty();

        when(restClient.getFile(anyString(), anyString(), anyString(), anyString())).thenReturn(Optional.of(helmChart.toFile()));

        when(restClient.buildUrl(any(), any())).thenReturn("someString");
        doNothing().when(restClient).delete(any(), any(), any());
        when(toscaoService.deleteServiceModel(anyString())).thenReturn(true);
        doNothing().when(dockerService).removeDockerImagesByPackageId(anyString());
        // when
        cleanupService.cleanupPackagesSetForDeletion();

        // then
        // package stuck in uploading should be deleted
        await().atMost(10, SECONDS).untilAsserted(() -> {
            final Optional<AppPackage> appPackageShouldAppearDeleted=
                    appPackageRepository.findByPackageId(appPackage.getPackageId());
            assertThat(appPackageShouldAppearDeleted).isEmpty();
            assertThat(serviceModelRecordRepository.findByAppPackage(appPackage)).isEmpty();
            assertThat(chartUrlsRepository.findByAppPackage(appPackage)).isEmpty();
            assertThat(appPackageDockerImageRepository.findByAppPackage(appPackage)).isEmpty();
            assertThat(operationDetailEntityRepository.findByAppPackagePackageId(appPackage.getPackageId())).isEmpty();
            assertThat(appPackageArtifactsRepository.findByAppPackage(appPackage)).isEmpty();
            assertThat(onboardingDetailRepository.findByAppPackage(appPackage)).isEmpty();
        });
    }

    @Test
    public void shouldCleanupOnlyPackagesStuckDuringOnboarding() {
        // given
        final OnboardingHeartbeat heartbeat = onboardingHeartbeatRepository.findAll().get(0);

        final AppPackage appPackageStuckInUploading = appPackageRepository.save(createPackageStuckInUploading(heartbeat));
        final AppPackage appPackageStuckInProcessing = appPackageRepository.save(createPackageStuckInProcessing(heartbeat));
        final AppPackage appPackageOnboardingInProgress = appPackageRepository.save(createPackageOnboardingInProgress(heartbeat));
        final AppPackage appPackageOnboardingCompleted = appPackageRepository.save(createPackageOnboardingCompleted());

        // when
        cleanupService.cleanupStuckPackages();

        // then
        // package stuck in uploading should be reset
        final AppPackage modifiedAppPackageStuckInUploading = appPackageRepository.findByPackageId(appPackageStuckInUploading.getPackageId()).get();

        assertThat(modifiedAppPackageStuckInUploading.getOnboardingState()).isEqualTo(AppPackage.OnboardingStateEnum.ERROR);
        assertThat(modifiedAppPackageStuckInUploading.getUsageState()).isEqualTo(AppPackage.AppUsageStateEnum.NOT_IN_USE);
        assertThat(modifiedAppPackageStuckInUploading.getOperationalState()).isEqualTo(AppPackage.OperationalStateEnum.DISABLED);
        assertThat(modifiedAppPackageStuckInUploading.getUserDefinedData()).hasSize(1);

        assertThat(serviceModelRecordRepository.findByAppPackage(appPackageStuckInUploading)).isEmpty();
        assertThat(chartUrlsRepository.findByAppPackage(appPackageStuckInUploading)).isEmpty();
        assertThat(appPackageDockerImageRepository.findByAppPackage(appPackageStuckInUploading)).isEmpty();
        assertThat(operationDetailEntityRepository.findByAppPackagePackageId(appPackageStuckInUploading.getPackageId())).isEmpty();
        assertThat(appPackageArtifactsRepository.findByAppPackage(appPackageStuckInUploading)).isEmpty();
        assertThat(onboardingDetailRepository.findByAppPackage(appPackageStuckInUploading)).isEmpty();

        assertThat(modifiedAppPackageStuckInUploading.getDescriptorId()).isNull();
        assertThat(modifiedAppPackageStuckInUploading.getDescriptorVersion()).isEqualTo(Constants.DEFAULT_PACKAGE_DESCRIPTOR_VERSION);
        assertThat(modifiedAppPackageStuckInUploading.getDescriptorModel()).isNull();
        assertThat(modifiedAppPackageStuckInUploading.getProvider()).isEqualTo(Constants.DEFAULT_PACKAGE_PROVIDER_NAME);
        assertThat(modifiedAppPackageStuckInUploading.getProductName()).isEqualTo(Constants.DEFAULT_PACKAGE_PRODUCT_NAME);
        assertThat(modifiedAppPackageStuckInUploading.getSoftwareVersion()).isEqualTo(appPackageStuckInUploading.getPackageId());
        assertThat(modifiedAppPackageStuckInUploading.getChecksum()).isNull();
        assertThat(modifiedAppPackageStuckInUploading.getFiles()).isNull();
        assertThat(modifiedAppPackageStuckInUploading.getHelmfile()).isNull();
        assertThat(modifiedAppPackageStuckInUploading.getErrorDetails()).isEqualTo("Can not onboard package: timeout has been exceeded");
        assertThat(modifiedAppPackageStuckInUploading.getVnfdZip()).isNull();
        assertThat(modifiedAppPackageStuckInUploading.isMultipleVnfd()).isFalse();
        assertThat(modifiedAppPackageStuckInUploading.getPackageSecurityOption()).isNull();

        // package stuck in processing should be reset
        final AppPackage modifiedAppPackageStuckInProcessing = appPackageRepository.findByPackageId(appPackageStuckInProcessing.getPackageId()).get();
        assertThat(modifiedAppPackageStuckInProcessing.getOnboardingState()).isEqualTo(AppPackage.OnboardingStateEnum.ERROR);

        // package that is currently onboarded and timeout not expired should not be reset
        final AppPackage modifiedAppPackageOnboardingInProgress =
                appPackageRepository.findByPackageId(appPackageOnboardingInProgress.getPackageId()).get();
        assertThat(modifiedAppPackageOnboardingInProgress.getOnboardingState()).isEqualTo(AppPackage.OnboardingStateEnum.UPLOADING);

        // package that is already onboarded should not be reset
        final AppPackage modifiedAppPackageOnboardingCompleted =
                appPackageRepository.findByPackageId(appPackageOnboardingCompleted.getPackageId()).get();
        assertThat(modifiedAppPackageOnboardingCompleted.getOnboardingState()).isEqualTo(AppPackage.OnboardingStateEnum.ONBOARDED);
    }

    private boolean heartbeatIsCreated() {
        final List<OnboardingHeartbeat> existingHeartbeats = onboardingHeartbeatRepository.findAll();

        return !existingHeartbeats.isEmpty();
    }

    private static AppPackage createPackageStuckInUploading(final OnboardingHeartbeat heartbeat) {
        final AppPackage appPackage = new AppPackage();

        appPackage.setServiceModelRecordEntity(createServiceModelRecord(appPackage));
        appPackage.setChartsRegistryUrl(createChartUrls(appPackage));
        appPackage.setAppPackageDockerImages(createImages(appPackage));
        appPackage.setOperationDetails(createOperationDetails(appPackage));
        appPackage.setAppPackageArtifacts(createArtifacts(appPackage));
        appPackage.setOnboardingDetail(createOnboardingDetail(appPackage, LocalDateTime.now().minusHours(12), heartbeat));

        OnboardingPackageState.UPLOADING_NOT_IN_USE_DISABLED.setPackageState(appPackage);
        appPackage.setUserDefinedData(createUserData(appPackage));

        appPackage.setDescriptorId("descriptor-id-for-expired-onboarding");
        appPackage.setDescriptorVersion("descriptor-version");
        appPackage.setDescriptorModel("{}");
        appPackage.setProvider("Ericsson");
        appPackage.setProductName("Network function");
        appPackage.setSoftwareVersion("1.2.3");
        appPackage.setChecksum("ha$h");
        appPackage.setFiles(new byte[10]);
        appPackage.setHelmfile(new byte[10]);
        appPackage.setVnfdZip(new byte[10]);
        appPackage.setMultipleVnfd(true);
        appPackage.setPackageSecurityOption(AppPackage.PackageSecurityOption.UNSIGNED);
        appPackage.setForDeletion(false);

        return appPackage;
    }

    private static AppPackage createPackageStuckInProcessing(final OnboardingHeartbeat heartbeat) {
        final AppPackage appPackage = new AppPackage();
        appPackage.setOnboardingDetail(createOnboardingDetail(appPackage, LocalDateTime.now().minusMinutes(1), heartbeat));
        OnboardingPackageState.PROCESSING_NOT_IN_USE_DISABLED.setPackageState(appPackage);
        appPackage.setUserDefinedData(createUserData(appPackage));
        appPackage.setForDeletion(false);

        return appPackage;
    }

    private static AppPackage createPackageOnboardingInProgress(final OnboardingHeartbeat heartbeat) {
        final AppPackage appPackage = new AppPackage();

        appPackage.setOnboardingDetail(createOnboardingDetail(appPackage, LocalDateTime.now().plusHours(1), heartbeat));

        OnboardingPackageState.UPLOADING_NOT_IN_USE_DISABLED.setPackageState(appPackage);
        appPackage.setUserDefinedData(createUserData(appPackage));
        appPackage.setForDeletion(false);
        return appPackage;
    }

    private static AppPackage createPackageWithServiceModelChartsAndRegistry() {
        final AppPackage appPackage = new AppPackage();

        OnboardingPackageState.ONBOARDED_NOT_IN_USE_ENABLED.setPackageState(appPackage);
        appPackage.setUserDefinedData(createUserData(appPackage));
        appPackage.setChartsRegistryUrl(createChartUrls(appPackage));
        appPackage.setForDeletion(true);
        appPackage.setServiceModelRecordEntity(createServiceModelRecord(appPackage));

        return appPackage;
    }

    private static AppPackage createPackageOnboardingCompleted() {
        final AppPackage appPackage = new AppPackage();

        OnboardingPackageState.ONBOARDED_NOT_IN_USE_ENABLED.setPackageState(appPackage);
        appPackage.setUserDefinedData(createUserData(appPackage));
        appPackage.setForDeletion(false);

        return appPackage;
    }

    private static AppPackage createPackageSetForDeletion() {
        final AppPackage appPackage = new AppPackage();

        OnboardingPackageState.ONBOARDED_NOT_IN_USE_ENABLED.setPackageState(appPackage);
        appPackage.setUserDefinedData(createUserData(appPackage));
        appPackage.setForDeletion(true);

        return appPackage;
    }
    private static ServiceModelRecordEntity createServiceModelRecord(final AppPackage appPackage) {
        final ServiceModelRecordEntity record = new ServiceModelRecordEntity();
        record.setAppPackage(appPackage);
        record.setServiceModelId("service-model-id");
        record.setServiceModelName("service-model-name");
        record.setDescriptorId("descriptor-id-for-expired-onboarding");

        return record;
    }

    private static OnboardingDetail createOnboardingDetail(final AppPackage appPackage,
                                                           final LocalDateTime expiredOnboardingTime,
                                                           final OnboardingHeartbeat heartbeat) {
        final OnboardingDetail onboardingDetail = new OnboardingDetail();
        onboardingDetail.setAppPackage(appPackage);
        onboardingDetail.setExpiredOnboardingTime(expiredOnboardingTime);
        onboardingDetail.setOnboardingHeartbeat(heartbeat);
        onboardingDetail.setOnboardingPhase("Generate Cheksum");
        onboardingDetail.setPackageUploadContext(createPackageUploadContext());

        return onboardingDetail;
    }

    public static PackageUploadRequestContext createPackageUploadContext() {
        PackageUploadRequestContext packageUploadRequestContext =
                new PackageUploadRequestContext("someFileName",
                                                Paths.get("some/path"),
                                                LocalDateTime.of(2022, 3, 15, 11, 23),
                                                "somePackageId");
        packageUploadRequestContext.setHelmChartPaths(Set.of(Paths.get("dummy/path")));

        return packageUploadRequestContext;
    }

    private static List<ChartUrlsEntity> createChartUrls(final AppPackage appPackage) {
        final ChartUrlsEntity chartUrl = new ChartUrlsEntity();
        chartUrl.setAppPackage(appPackage);
        chartUrl.setChartsRegistryUrl("registry");
        chartUrl.setChartName("chart-name");
        chartUrl.setChartType(ChartUrlsEntity.ChartTypeEnum.CNF);
        chartUrl.setChartVersion("1.4.6");
        chartUrl.setChartArtifactKey("chart-artifact-key");
        chartUrl.setPriority(1);

        return new ArrayList<>(List.of(chartUrl));
    }

    private static List<AppPackageDockerImage> createImages(final AppPackage appPackage) {
        final AppPackageDockerImage image = new AppPackageDockerImage();
        image.setAppPackage(appPackage);
        image.setImageId("image-id");

        return new ArrayList<>(List.of(image));
    }

    private static List<OperationDetailEntity> createOperationDetails(final AppPackage appPackage) {
        final OperationDetailEntity operationDetail = new OperationDetailEntity();
        operationDetail.setAppPackage(appPackage);
        operationDetail.setOperationName("INSTANTIATE");
        operationDetail.setSupported(true);

        return new ArrayList<>(List.of(operationDetail));
    }

    private static List<AppPackageArtifacts> createArtifacts(final AppPackage appPackage) {
        final AppPackageArtifacts artifact = new AppPackageArtifacts();
        artifact.setAppPackage(appPackage);
        artifact.setArtifactPath("artifact-path");

        return new ArrayList<>(List.of(artifact));
    }

    private static List<AppUserDefinedData> createUserData(final AppPackage appPackage) {
        final AppUserDefinedData userDefinedData = new AppUserDefinedData();
        userDefinedData.setAppPackages(appPackage);
        userDefinedData.setKey("key");
        userDefinedData.setValue("value");

        return new ArrayList<>(List.of(userDefinedData));
    }
}
