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

import com.ericsson.am.shared.vnfd.model.VnfDescriptorDetails;
import com.ericsson.amonboardingservice.TestUtils;
import com.ericsson.amonboardingservice.presentation.exceptions.FailedOnboardingValidationException;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.services.filter.VnfPackageQuery;
import com.ericsson.amonboardingservice.presentation.services.packageservice.OnboardingPackageState;
import com.ericsson.amonboardingservice.presentation.services.vnfdservice.VnfdService;
import com.ericsson.amonboardingservice.presentation.services.vnfdservice.VnfdServiceImpl;
import com.ericsson.amonboardingservice.utils.Constants;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_DEFINITIONS;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_HELM_DEFINITIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { VnfdValidation.class, VnfdServiceImpl.class })
@MockBean(classes = {
    VnfPackageQuery.class
})
public class VnfdValidationTest {

    private static final String TEST_PACKAGE_ID = "test_packageID";

    @Autowired
    private VnfdValidation vnfdValidation;

    @MockBean
    private VnfdService vnfdService;

    @MockBean
    private AppPackageRepository appPackageRepository;

    @TempDir
    public Path tempFolder;

    private PackageUploadRequestContext context;
    private PackageUploadRequestContext contextWithEntryHelmDefinitions;
    private PackageUploadRequestContext contextWithEntryHelmDefinitionsWithScalingMapping;
    private PackageUploadRequestContext contextWithEntryHelmDefinitionsOnlyScalingMapping;
    private PackageUploadRequestContext contextWithEntryHelmDefinitionsWithoutChart;
    private PackageUploadRequestContext contextWithEntryHelmDefinitionsWithoutAnyChart;

    private Path validVnfd;
    private Path validEntryHelmDefinitions;
    private Path validEntryHelmDefinitionsWithScalingMapping;
    private Path validEntryHelmDefinitionsOnlyScalingMapping;
    private Path entryHelmDefinitionsWithoutChart;
    private Path entryHelmDefinitionsWithoutAnyChart;

    @BeforeEach
    public void setUp() throws Exception {
        var packageContent = Files.createFile(tempFolder.resolve("fakeVnfPackage.zip"));
        validVnfd = TestUtils.getResource("vnfd/valid_vnfd.yaml");
        context = new PackageUploadRequestContext("testCsar.csar", packageContent, LocalDateTime.now(), TEST_PACKAGE_ID);
        context.setVnfd(new VnfDescriptorDetails());
        var artifactPaths = Map.of(ENTRY_DEFINITIONS, validVnfd, Constants.PATH_TO_PACKAGE, packageContent);
        context.setArtifactPaths(artifactPaths);

        //contextWithEntryHelmDefinitions
        validEntryHelmDefinitions = TestUtils.getResource("package-artifacts/Definitions/OtherTemplates");
        contextWithEntryHelmDefinitions = new PackageUploadRequestContext("testCsar.csar", packageContent, LocalDateTime.now(), TEST_PACKAGE_ID);
        contextWithEntryHelmDefinitions.setVnfd(new VnfDescriptorDetails());
        var artifactPathsWithEntryHelmDefinitions = Map.of(
            ENTRY_DEFINITIONS, validVnfd,
            Constants.PATH_TO_PACKAGE, packageContent,
            ENTRY_HELM_DEFINITIONS, validEntryHelmDefinitions
        );
        contextWithEntryHelmDefinitions.setArtifactPaths(artifactPathsWithEntryHelmDefinitions);

        //contextWithEntryHelmDefinitionsWithScalingMapping
        validEntryHelmDefinitionsWithScalingMapping = TestUtils.getResource("package-artifacts-with-scaling-mapping/Definitions/OtherTemplates");
        contextWithEntryHelmDefinitionsWithScalingMapping = new PackageUploadRequestContext("testCsar.csar", packageContent, LocalDateTime.now(), TEST_PACKAGE_ID);
        contextWithEntryHelmDefinitionsWithScalingMapping.setVnfd(new VnfDescriptorDetails());
        var artifactPathsWithEntryHelmDefinitionsWithScalingMapping = Map.of(
            ENTRY_DEFINITIONS, validVnfd,
            Constants.PATH_TO_PACKAGE, packageContent,
            ENTRY_HELM_DEFINITIONS, validEntryHelmDefinitionsWithScalingMapping
        );
        contextWithEntryHelmDefinitionsWithScalingMapping.setArtifactPaths(artifactPathsWithEntryHelmDefinitionsWithScalingMapping);

        //contextWithEntryHelmDefinitionsOnlyScalingMapping
        validEntryHelmDefinitionsOnlyScalingMapping = TestUtils.getResource("package-artifacts-only-scaling-mapping/Definitions/OtherTemplates");
        contextWithEntryHelmDefinitionsOnlyScalingMapping = new PackageUploadRequestContext("testCsar.csar", packageContent, LocalDateTime.now(), TEST_PACKAGE_ID);
        contextWithEntryHelmDefinitionsOnlyScalingMapping.setVnfd(new VnfDescriptorDetails());
        var artifactPathsWithEntryHelmDefinitionsOnlyScalingMapping = Map.of(
            ENTRY_DEFINITIONS, validVnfd,
            Constants.PATH_TO_PACKAGE, packageContent,
            ENTRY_HELM_DEFINITIONS, validEntryHelmDefinitionsOnlyScalingMapping
        );
        contextWithEntryHelmDefinitionsOnlyScalingMapping.setArtifactPaths(artifactPathsWithEntryHelmDefinitionsOnlyScalingMapping);

        //contextWithEntryHelmDefinitionsWithoutChart
        entryHelmDefinitionsWithoutChart = TestUtils.getResource("package-artifacts-without-chart/Definitions/OtherTemplates");
        contextWithEntryHelmDefinitionsWithoutChart = new PackageUploadRequestContext("testCsar.csar", packageContent, LocalDateTime.now(), TEST_PACKAGE_ID);
        contextWithEntryHelmDefinitionsWithoutChart.setVnfd(new VnfDescriptorDetails());
        var artifactPathsWithEntryHelmDefinitionsWithoutChart = Map.of(
            ENTRY_DEFINITIONS, validVnfd,
            Constants.PATH_TO_PACKAGE, packageContent,
            ENTRY_HELM_DEFINITIONS, entryHelmDefinitionsWithoutChart
        );
        contextWithEntryHelmDefinitionsWithoutChart.setArtifactPaths(artifactPathsWithEntryHelmDefinitionsWithoutChart);

        //contextWithEntryHelmDefinitionsWithoutAnyChart
        entryHelmDefinitionsWithoutAnyChart = TestUtils.getResource("package-artifacts-without-any-chart/Definitions/OtherTemplates");
        contextWithEntryHelmDefinitionsWithoutAnyChart = new PackageUploadRequestContext("testCsar.csar", packageContent, LocalDateTime.now(), TEST_PACKAGE_ID);
        contextWithEntryHelmDefinitionsWithoutAnyChart.setVnfd(new VnfDescriptorDetails());
        var artifactPathsWithEntryHelmDefinitionsWithoutAnyChart = Map.of(
            ENTRY_DEFINITIONS, validVnfd,
            Constants.PATH_TO_PACKAGE, packageContent,
            ENTRY_HELM_DEFINITIONS, entryHelmDefinitionsWithoutAnyChart
        );
        contextWithEntryHelmDefinitionsWithoutAnyChart.setArtifactPaths(artifactPathsWithEntryHelmDefinitionsWithoutAnyChart);
    }

    @Test
    public void testHandleShouldPassForValidVnfd() {
        // given
        var vnfDescriptor = new JSONObject(Map.of(com.ericsson.am.shared.vnfd.utils.Constants.NODE_TYPES_KEY, new JSONObject(Map.of("Sample_VNF", "test"))));
        when(vnfdService.getVnfDescriptor(validVnfd)).thenReturn(vnfDescriptor);
        when(vnfdService.validateAndGetVnfDescriptorDetails(any())).thenReturn(new VnfDescriptorDetails());

        // when/then
        assertThatNoException().isThrownBy(() -> vnfdValidation.handle(context));
    }

    @Test
    public void testHandleValidateEntryHelmDefinitionsValues_shouldHaveYamlExtensionChartAndBeParsable() throws IOException {
        // given
        var appPackage = new AppPackage();
        when(appPackageRepository.findByPackageId(TEST_PACKAGE_ID)).thenReturn(Optional.of(appPackage));
        when(appPackageRepository.save(OnboardingPackageState.PROCESSING_NOT_IN_USE_DISABLED.setPackageState(appPackage))).thenReturn(appPackage);
        var vnfDescriptor = new JSONObject(Map.of(com.ericsson.am.shared.vnfd.utils.Constants.NODE_TYPES_KEY, new JSONObject(Map.of("Sample_VNF", "test"))));
        when(vnfdService.getVnfDescriptor(validVnfd)).thenReturn(vnfDescriptor);
        when(vnfdService.validateAndGetVnfDescriptorDetails(any())).thenReturn(new VnfDescriptorDetails());

        // when/then
        assertDoesNotThrow(() -> vnfdValidation.handle(contextWithEntryHelmDefinitions));
    }

    @Test
    public void testHandleValidateEntryHelmDefinitionsValues_shouldIgnoreScalingMapping() throws IOException {
        // given
        var appPackage = new AppPackage();
        when(appPackageRepository.findByPackageId(TEST_PACKAGE_ID)).thenReturn(Optional.of(appPackage));
        when(appPackageRepository.save(OnboardingPackageState.PROCESSING_NOT_IN_USE_DISABLED.setPackageState(appPackage))).thenReturn(appPackage);
        var vnfDescriptor = new JSONObject(Map.of(com.ericsson.am.shared.vnfd.utils.Constants.NODE_TYPES_KEY, new JSONObject(Map.of("Sample_VNF", "test"))));
        when(vnfdService.getVnfDescriptor(validVnfd)).thenReturn(vnfDescriptor);
        when(vnfdService.validateAndGetVnfDescriptorDetails(any())).thenReturn(new VnfDescriptorDetails());

        // when/then
        assertDoesNotThrow(() -> vnfdValidation.handle(contextWithEntryHelmDefinitionsWithScalingMapping));
    }

    @Test
    public void testHandleValidateEntryHelmDefinitionsValues_onlyScalingMapping() throws IOException {
        // given
        var appPackage = new AppPackage();
        when(appPackageRepository.findByPackageId(TEST_PACKAGE_ID)).thenReturn(Optional.of(appPackage));
        when(appPackageRepository.save(OnboardingPackageState.PROCESSING_NOT_IN_USE_DISABLED.setPackageState(appPackage))).thenReturn(appPackage);
        var vnfDescriptor = new JSONObject(Map.of(com.ericsson.am.shared.vnfd.utils.Constants.NODE_TYPES_KEY, new JSONObject(Map.of("Sample_VNF", "test"))));
        when(vnfdService.getVnfDescriptor(validVnfd)).thenReturn(vnfDescriptor);
        when(vnfdService.validateAndGetVnfDescriptorDetails(any())).thenReturn(new VnfDescriptorDetails());

        // when/then
        assertDoesNotThrow(() -> vnfdValidation.handle(contextWithEntryHelmDefinitionsOnlyScalingMapping));
    }

    @Test
    public void testHandleValidateEntryHelmDefinitionsValues_withoutChart_shouldAddError() throws IOException {
        // given
        contextWithEntryHelmDefinitionsWithoutChart.setEtsiPackage(true);
        var appPackage = new AppPackage();
        when(appPackageRepository.findByPackageId(TEST_PACKAGE_ID)).thenReturn(Optional.of(appPackage));
        when(appPackageRepository.save(OnboardingPackageState.PROCESSING_NOT_IN_USE_DISABLED.setPackageState(appPackage))).thenReturn(appPackage);
        var vnfDescriptor = new JSONObject(Map.of(com.ericsson.am.shared.vnfd.utils.Constants.NODE_TYPES_KEY, new JSONObject(Map.of("Sample_VNF", "test"))));
        when(vnfdService.getVnfDescriptor(validVnfd)).thenReturn(vnfDescriptor);
        when(vnfdService.validateAndGetVnfDescriptorDetails(any())).thenReturn(new VnfDescriptorDetails());

        // when/then
        assertThrows(FailedOnboardingValidationException.class, () -> vnfdValidation.handle(contextWithEntryHelmDefinitionsWithoutChart));
        assertThat(contextWithEntryHelmDefinitionsWithoutChart.getErrors()).hasSize(1);
        var errorMessage = contextWithEntryHelmDefinitionsWithoutChart.getErrors().get(0);
        assertThat(errorMessage.getMessage()).isEqualTo("Unable to Onboard: Following Entry-Helm-Definitions value file(s) does not contain chart(s): [valid_helm_definitions_values.yaml]");
    }

    @Test
    public void testHandleValidateEntryHelmDefinitionsValues_withoutAnyChart_shouldAddError() throws IOException {
        // given
        contextWithEntryHelmDefinitionsWithoutAnyChart.setEtsiPackage(true);
        var appPackage = new AppPackage();
        when(appPackageRepository.findByPackageId(TEST_PACKAGE_ID)).thenReturn(Optional.of(appPackage));
        when(appPackageRepository.save(OnboardingPackageState.PROCESSING_NOT_IN_USE_DISABLED.setPackageState(appPackage))).thenReturn(appPackage);
        var vnfDescriptor = new JSONObject(Map.of(com.ericsson.am.shared.vnfd.utils.Constants.NODE_TYPES_KEY, new JSONObject(Map.of("Sample_VNF", "test"))));
        when(vnfdService.getVnfDescriptor(validVnfd)).thenReturn(vnfDescriptor);
        when(vnfdService.validateAndGetVnfDescriptorDetails(any())).thenReturn(new VnfDescriptorDetails());

        // when/then
        assertThrows(FailedOnboardingValidationException.class, () -> vnfdValidation.handle(contextWithEntryHelmDefinitionsWithoutAnyChart));
        assertThat(contextWithEntryHelmDefinitionsWithoutAnyChart.getErrors()).hasSize(1);
        var errorMessage = contextWithEntryHelmDefinitionsWithoutAnyChart.getErrors().get(0);
        assertThat(errorMessage.getMessage()).isEqualTo("Unable to Onboard: Following Entry-Helm-Definitions value file(s) does not contain chart(s): [valid_helm_definitions_values.yaml]");

    }
}