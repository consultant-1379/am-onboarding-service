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
import com.ericsson.am.shared.vnfd.model.servicemodel.ServiceModel;
import com.ericsson.am.shared.vnfd.service.ToscaoService;
import com.ericsson.amonboardingservice.presentation.exceptions.FailedOnboardingValidationException;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.services.ToscaHelper;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.ericsson.amonboardingservice.presentation.services.filter.VnfPackageQuery;
import com.ericsson.amonboardingservice.presentation.services.packageservice.OnboardingPackageState;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageDatabaseService;
import com.ericsson.amonboardingservice.presentation.services.vnfdservice.VnfdService;
import com.ericsson.amonboardingservice.presentation.services.vnfdservice.VnfdServiceImpl;
import com.ericsson.amonboardingservice.utils.Constants;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_DEFINITIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
    VnfdValidation.class,
    VnfdServiceImpl.class,
    ToscaValidation.class,
    ToscaHelper.class,
        MockitoExtension.class
})
@MockBean(classes = {
    AppPackageRepository.class,
    VnfPackageQuery.class,
    FileService.class
})
@TestPropertySource(
    properties = {
        "onboarding.skipToscaoValidation = false",
    })
public class ToscaValidationTest {

    private static final String TEST_PACKAGE_ID = "test_packageID";
    private static final String DESCRIPTOR_ID = "descriptor-id";
    private static final String TOSCAO_ERROR_MESSAGE = "Validation failed";
    @TempDir
    public Path tempFolder;
    @Autowired
    private ToscaValidation toscaValidation;
    @MockBean
    private ToscaHelper toscaHelper;
    @MockBean
    private VnfdService vnfdService;
    @MockBean
    private ToscaoService toscaoService;
    @MockBean
    private PackageDatabaseService databaseService;
    @MockBean
    private AppPackageRepository appPackageRepository;
    @MockBean
    private FileService fileService;
    @Value("${onboarding.skipToscaoValidation}")
    private boolean isToscaoDisabled;
    private PackageUploadRequestContext context;
    private VnfDescriptorDetails vnfdDescriptorDetails;
    private Path packageContent;

    @BeforeEach
    public void setUp() throws Exception {
        vnfdDescriptorDetails = new VnfDescriptorDetails();
        vnfdDescriptorDetails.setVnfDescriptorId(DESCRIPTOR_ID);
        vnfdDescriptorDetails.setDescriptorModel("{}");
        packageContent = tempFolder.resolve("fakeVnfPackage.zip");
        context = new PackageUploadRequestContext("testCsar.csar", packageContent, LocalDateTime.now(), TEST_PACKAGE_ID);
        context.setVnfd(vnfdDescriptorDetails);
        var artifactPaths = Map.of(ENTRY_DEFINITIONS, Path.of(""), Constants.PATH_TO_PACKAGE, packageContent);
        context.setArtifactPaths(artifactPaths);
    }

    @Test
    public void testNeedToValidateWithToscaAndServiceIsDisabledCase() throws IOException {
        var appPackage = new AppPackage();
        when(databaseService.getAppPackageById(TEST_PACKAGE_ID)).thenReturn(appPackage);
        when(appPackageRepository.save(OnboardingPackageState.PROCESSING_NOT_IN_USE_DISABLED.setPackageState(appPackage))).thenReturn(appPackage);
        when(fileService.zipDirectory(any())).thenReturn(Files.createFile(Path.of(
                tempFolder.toAbsolutePath().toString(),
                tempFolder.getFileName().toString())));

        when(toscaHelper.isTosca1Dot2(any())).thenReturn(false);
        when(toscaHelper.isEtsiPackage(any())).thenReturn(true);

        toscaValidation.handle(context);

        verify(toscaHelper).uploadPackageToToscao(eq(context));
    }

    @Test
    public void testPackageIdNotEtsiSoValidationIsNotPerformed() {
        var appPackage = new AppPackage();
        when(databaseService.getAppPackageById(TEST_PACKAGE_ID)).thenReturn(appPackage);
        when(appPackageRepository.save(OnboardingPackageState.PROCESSING_NOT_IN_USE_DISABLED.setPackageState(appPackage))).thenReturn(appPackage);

        when(toscaHelper.isEtsiPackage(any())).thenReturn(false);
        toscaValidation.handle(context);

        verify(vnfdService, never()).getVnfDescriptor(any());
        verify(toscaHelper, never()).isTosca1Dot2(any());
        verify(vnfdService, never()).getDescriptorId(any());
        verify(toscaoService, never()).getServiceModelByDescriptorId(any());
        verify(toscaoService, never()).getServiceModelByDescriptorId(any());
        verify(toscaHelper, never()).uploadPackageToToscao(eq(context));
    }

    @Test
    public void testContextHasServiceModel() {
        ServiceModel model = new ServiceModel();
        context.setServiceModel(model);
        when(toscaHelper.isEtsiPackage(any())).thenReturn(true);

        verify(vnfdService, never()).getVnfDescriptor(any());
        verify(vnfdService, never()).getDescriptorId(any());
        verify(toscaHelper, never()).isTosca1Dot2(any());
        verify(toscaHelper, never()).uploadPackageToToscao(any());
    }
    @Test
    public void testSuccessfullHandleWhenServiceModelIsPresent() throws IOException {
        var appPackage = new AppPackage();
        when(databaseService.getAppPackageById(TEST_PACKAGE_ID)).thenReturn(appPackage);
        when(appPackageRepository.save(OnboardingPackageState.PROCESSING_NOT_IN_USE_DISABLED.setPackageState(appPackage))).thenReturn(appPackage);
        when(fileService.zipDirectory(any())).thenReturn(Files.createFile(Path.of(
                tempFolder.toAbsolutePath().toString(),
                tempFolder.getFileName().toString())));

        ServiceModel model = new ServiceModel();
        when(toscaHelper.isEtsiPackage(any())).thenReturn(true);
        when(toscaHelper.isTosca1Dot2(any())).thenReturn(false);
        when(vnfdService.getDescriptorId(any())).thenReturn(DESCRIPTOR_ID);
        when(toscaoService.getServiceModelByDescriptorId(anyString())).thenReturn(Optional.of(model));

        toscaValidation.handle(context);

        assertThat(context.getServiceModel()).isEqualTo(model);
        assertThat(context.getErrors()).isNullOrEmpty();
        verify(vnfdService).getVnfDescriptor(any());
        verify(toscaHelper, never()).uploadPackageToToscao(context);
        verify(toscaHelper).isTosca1Dot2(any());
    }

    @Test
    public void testNoServiceModelValidationWithToscao() throws IOException {
        var appPackage = new AppPackage();
        when(databaseService.getAppPackageById(TEST_PACKAGE_ID)).thenReturn(appPackage);
        when(appPackageRepository.save(OnboardingPackageState.PROCESSING_NOT_IN_USE_DISABLED.setPackageState(appPackage))).thenReturn(appPackage);
        when(fileService.zipDirectory(any())).thenReturn(Files.createFile(Path.of(
                tempFolder.toAbsolutePath().toString(),
                tempFolder.getFileName().toString())));

        when(toscaHelper.isEtsiPackage(any())).thenReturn(true);
        when(vnfdService.getDescriptorId(any())).thenReturn(DESCRIPTOR_ID);
        when(toscaoService.getServiceModelByDescriptorId(anyString())).thenReturn(Optional.empty());
        when(toscaHelper.isTosca1Dot2(any())).thenReturn(false);

        toscaValidation.handle(context);
        assertThat(context.getErrors()).isNullOrEmpty();
        verify(toscaHelper).uploadPackageToToscao(context);
    }

    @Test
    public void testNoServiceModelAndNoNeedToValidateWithTosca() throws IOException {
        var appPackage = new AppPackage();
        when(databaseService.getAppPackageById(TEST_PACKAGE_ID)).thenReturn(appPackage);
        when(appPackageRepository.save(OnboardingPackageState.PROCESSING_NOT_IN_USE_DISABLED.setPackageState(appPackage))).thenReturn(appPackage);
        when(fileService.zipDirectory(any())).thenReturn(Files.createFile(Path.of(
                tempFolder.toAbsolutePath().toString(),
                tempFolder.getFileName().toString())));

        when(toscaHelper.isEtsiPackage(any())).thenReturn(true);
        when(toscaoService.getServiceModelByDescriptorId(anyString())).thenReturn(Optional.empty());
        when(vnfdService.getVnfDescriptor(any())).thenReturn(new JSONObject());
        when(toscaHelper.isTosca1Dot2(any())).thenReturn(false);

        toscaValidation.handle(context);

        assertThat(context.getErrors()).isNullOrEmpty();
        verify(toscaoService).getServiceModelByDescriptorId(any());
        verify(vnfdService).getVnfDescriptor(any());
        verify(toscaHelper).isTosca1Dot2(any());
        verify(toscaHelper).uploadPackageToToscao(context);
    }

    @Test
    public void testThrowsOnboardingValidationException() throws IOException {
        var appPackage = new AppPackage();
        when(databaseService.getAppPackageById(TEST_PACKAGE_ID)).thenReturn(appPackage);
        when(appPackageRepository.save(OnboardingPackageState.PROCESSING_NOT_IN_USE_DISABLED.setPackageState(appPackage))).thenReturn(appPackage);
        when(fileService.zipDirectory(any())).thenReturn(Files.createFile(Path.of(
                tempFolder.toAbsolutePath().toString(),
                tempFolder.getFileName().toString())));

        when(toscaHelper.isEtsiPackage(any())).thenReturn(true);
        when(vnfdService.getDescriptorId(any())).thenReturn(DESCRIPTOR_ID);
        when(toscaoService.getServiceModelByDescriptorId(anyString())).thenReturn(Optional.empty());
        when(toscaHelper.isTosca1Dot2(any())).thenReturn(false);
        doThrow(new FailedOnboardingValidationException(TOSCAO_ERROR_MESSAGE)).when(toscaHelper).uploadPackageToToscao(context);

        assertThatExceptionOfType(FailedOnboardingValidationException.class)
            .isThrownBy(() -> toscaValidation.handle(context))
            .withMessage(TOSCAO_ERROR_MESSAGE);
    }
}