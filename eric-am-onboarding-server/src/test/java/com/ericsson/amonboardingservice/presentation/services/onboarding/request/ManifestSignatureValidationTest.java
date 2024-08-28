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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static com.ericsson.amonboardingservice.TestUtils.getResource;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_CERTIFICATE;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_MANIFEST;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.presentation.exceptions.SignatureValidationException;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.ericsson.signatureservice.SignatureService;
import com.ericsson.signatureservice.exception.SignatureVerificationException;

@SpringBootTest
@ActiveProfiles("test")
public class ManifestSignatureValidationTest extends AbstractDbSetupTest {

    private static final String VALID_SIGNED_MANIFEST = "option1/manifest_valid_signature.mf";
    private static final String VALID_MANIFEST_WITH_SIGNATURE_AND_CRL = "option1/manifest_valid_signature_and_crl.mf";
    private static final String VALID_SIGNED_EOCM_STYLE_MANIFEST = "option1/manifest_valid_signature_eocm_style.mf";
    private static final String VALID_MANIFEST_NO_SIGNATURE = "option1/manifest_no_signature.mf";
    private static final String VALID_CERTIFICATE = "option1/one_valid_cert.cert";
    private static final String VALID_CERTIFICATE_AND_CRL = "option1/one_valid_cert_and_crl.cert";
    private static final String VALID_MULTIPLE_CERTIFICATES = "option1/multiple_valid_certs.cert";
    private static final String SIGNATURE = "option1/signature.cms";
    private static final String SIGNATURE_AND_CRL = "option1/signature_and_crl.cms";

    @Autowired
    @InjectMocks
    private ManifestSignatureValidation manifestSignatureValidation;

    @MockBean
    private SignatureService signatureService;

    @Autowired
    private FileService fileService;

    @Test
    public void shouldVerifyOption1WhenCertificateIsSeparateFile() throws IOException, URISyntaxException {
        Path testDir = prepareDirectory();
        Path pathToManifest = prepareFile(VALID_SIGNED_MANIFEST, testDir);
        Path pathToCertificate = prepareFile(VALID_CERTIFICATE, testDir);
        PackageUploadRequestContext context = prepareContext(pathToManifest, pathToCertificate);
        var signatureCaptor = ArgumentCaptor.forClass(String.class);
        var certificateCaptor = ArgumentCaptor.forClass(String.class);

        manifestSignatureValidation.handle(context);
        verify(signatureService).verifyContentSignature(any(), signatureCaptor.capture(), certificateCaptor.capture());

        String savedManifest = Files.readString(pathToManifest);
        String manifestWithoutSignature = Files.readString(getResource(VALID_MANIFEST_NO_SIGNATURE));
        String signature = Files.readString(getResource(SIGNATURE));
        String certificate = Files.readString(getResource(VALID_CERTIFICATE));

        assertThat(savedManifest).isEqualToIgnoringNewLines(manifestWithoutSignature);
        assertThat(signatureCaptor.getValue()).isEqualToIgnoringNewLines(signature);
        assertThat(certificateCaptor.getValue()).isEqualToIgnoringNewLines(certificate);
        assertThat(pathToCertificate.toFile().exists()).isEqualTo(false);
        assertThat(context.getPackageSecurityOption()).isEqualTo(AppPackage.PackageSecurityOption.OPTION_1);
    }

    @Test
    public void shouldVerifyOption1WhenCertificateIsSeparateFileWithCrl() throws IOException, URISyntaxException {
        Path testDir = prepareDirectory();
        Path pathToManifest = prepareFile(VALID_SIGNED_MANIFEST, testDir);
        Path pathToCertificate = prepareFile(VALID_CERTIFICATE_AND_CRL, testDir);
        PackageUploadRequestContext context = prepareContext(pathToManifest, pathToCertificate);
        var signatureCaptor = ArgumentCaptor.forClass(String.class);
        var certificateCaptor = ArgumentCaptor.forClass(String.class);

        manifestSignatureValidation.handle(context);
        verify(signatureService).verifyContentSignature(any(), signatureCaptor.capture(), certificateCaptor.capture());

        String savedManifest = Files.readString(pathToManifest);
        String manifestWithoutSignature = Files.readString(getResource(VALID_MANIFEST_NO_SIGNATURE));
        String signature = Files.readString(getResource(SIGNATURE));
        String certificate = Files.readString(getResource(VALID_CERTIFICATE_AND_CRL));

        assertThat(savedManifest).isEqualToIgnoringNewLines(manifestWithoutSignature);
        assertThat(signatureCaptor.getValue()).isEqualToIgnoringNewLines(signature);
        assertThat(certificateCaptor.getValue()).isEqualToIgnoringNewLines(certificate);
        assertThat(pathToCertificate.toFile().exists()).isEqualTo(false);
        assertThat(context.getPackageSecurityOption()).isEqualTo(AppPackage.PackageSecurityOption.OPTION_1);
    }

    @Test
    public void shouldVerifyOption1WhenCertificateInsideSignature() throws IOException, URISyntaxException {
        Path testDir = prepareDirectory();
        Path pathToManifest = prepareFile(VALID_SIGNED_MANIFEST, testDir);
        PackageUploadRequestContext context = prepareContext(pathToManifest, null);
        var signatureCaptor = ArgumentCaptor.forClass(String.class);

        manifestSignatureValidation.handle(context);
        verify(signatureService).verifyContentSignature(any(), signatureCaptor.capture(), isNull());

        String savedManifest = Files.readString(pathToManifest);
        String manifestWithoutSignature = Files.readString(getResource(VALID_MANIFEST_NO_SIGNATURE));
        String signature = Files.readString(getResource(SIGNATURE));

        assertThat(savedManifest).isEqualToIgnoringNewLines(manifestWithoutSignature);
        assertThat(signatureCaptor.getValue()).isEqualToIgnoringNewLines(signature);
        assertThat(context.getPackageSecurityOption()).isEqualTo(AppPackage.PackageSecurityOption.OPTION_1);
    }

    @Test
    public void shouldVerifyOption1WhenSignatureAppendedInEocmStyle() throws IOException, URISyntaxException {
        Path testDir = prepareDirectory();
        Path pathToManifest = prepareFile(VALID_SIGNED_EOCM_STYLE_MANIFEST, testDir);
        PackageUploadRequestContext context = prepareContext(pathToManifest, null);

        doThrow(new SignatureVerificationException("Failed verification"))
                .doNothing()
                .when(signatureService).verifyContentSignature(any(), anyString(), isNull());

        manifestSignatureValidation.handle(context);

        var signatureCaptor = ArgumentCaptor.forClass(String.class);
        var manifestCaptor = ArgumentCaptor.forClass(InputStream.class);
        verify(signatureService, times(2)).verifyContentSignature(manifestCaptor.capture(), signatureCaptor.capture(), isNull());

        String savedManifest = Files.readString(pathToManifest);
        String manifestWithoutSignature = Files.readString(getResource(VALID_MANIFEST_NO_SIGNATURE));
        String signature = Files.readString(getResource(SIGNATURE));

        assertThat(savedManifest).isEqualToIgnoringNewLines(manifestWithoutSignature);

        final InputStream firstManifestPassed = manifestCaptor.getAllValues().get(0);
        firstManifestPassed.reset(); // has to be reset as it is written to a file as part of onboarding logic
        assertThat(firstManifestPassed).asString(StandardCharsets.UTF_8).endsWith("59");
        final InputStream secondManifestPassed = manifestCaptor.getAllValues().get(1);
        assertThat(secondManifestPassed).asString(StandardCharsets.UTF_8).endsWith("59\n");

        assertThat(signatureCaptor.getAllValues().get(0)).isEqualToIgnoringNewLines(signature);
        assertThat(signatureCaptor.getAllValues().get(1)).isEqualToIgnoringNewLines(signature);

        assertThat(context.getPackageSecurityOption()).isEqualTo(AppPackage.PackageSecurityOption.OPTION_1);
    }

    @Test
    public void shouldVerifyOption1WhenSignatureAppendedRightAfterContentEndingWithTwoNewlines() throws IOException, URISyntaxException {
        Path testDir = prepareDirectory();
        Path pathToManifest = prepareFile(VALID_SIGNED_MANIFEST, testDir);
        PackageUploadRequestContext context = prepareContext(pathToManifest, null);

        doThrow(new SignatureVerificationException("Failed verification"))
                .doNothing()
                .when(signatureService).verifyContentSignature(any(), anyString(), isNull());

        manifestSignatureValidation.handle(context);

        var signatureCaptor = ArgumentCaptor.forClass(String.class);
        var manifestCaptor = ArgumentCaptor.forClass(InputStream.class);
        verify(signatureService, times(2)).verifyContentSignature(manifestCaptor.capture(), signatureCaptor.capture(), isNull());

        String savedManifest = Files.readString(pathToManifest);
        String manifestWithoutSignature = Files.readString(getResource(VALID_MANIFEST_NO_SIGNATURE));
        String signature = Files.readString(getResource(SIGNATURE));

        assertThat(savedManifest).isEqualToIgnoringNewLines(manifestWithoutSignature);

        final InputStream firstManifestPassed = manifestCaptor.getAllValues().get(0);
        firstManifestPassed.reset(); // has to be reset as it is written to a file as part of onboarding logic
        assertThat(firstManifestPassed).asString(StandardCharsets.UTF_8).endsWith("59\n");
        final InputStream secondManifestPassed = manifestCaptor.getAllValues().get(1);
        assertThat(secondManifestPassed).asString(StandardCharsets.UTF_8).endsWith("59\n\n");

        assertThat(signatureCaptor.getAllValues().get(0)).isEqualToIgnoringNewLines(signature);
        assertThat(signatureCaptor.getAllValues().get(1)).isEqualToIgnoringNewLines(signature);

        assertThat(context.getPackageSecurityOption()).isEqualTo(AppPackage.PackageSecurityOption.OPTION_1);
    }

    @Test
    public void shouldVerifyOption1WhenManifestContainsSignatureAndCrl() throws IOException, URISyntaxException {
        Path testDir = prepareDirectory();
        Path pathToManifest = prepareFile(VALID_MANIFEST_WITH_SIGNATURE_AND_CRL, testDir);
        PackageUploadRequestContext context = prepareContext(pathToManifest, null);
        var signatureCaptor = ArgumentCaptor.forClass(String.class);

        manifestSignatureValidation.handle(context);
        verify(signatureService).verifyContentSignature(any(), signatureCaptor.capture(), isNull());

        String savedManifest = Files.readString(pathToManifest);
        String manifestWithoutSignature = Files.readString(getResource(VALID_MANIFEST_NO_SIGNATURE));
        String signature = Files.readString(getResource(SIGNATURE_AND_CRL));

        assertThat(savedManifest).isEqualToIgnoringNewLines(manifestWithoutSignature);
        assertThat(signatureCaptor.getValue()).isEqualToIgnoringNewLines(signature);
        assertThat(context.getPackageSecurityOption()).isEqualTo(AppPackage.PackageSecurityOption.OPTION_1);
    }

    @Test
    public void shouldVerifyOption1WhenMultipleCertificates() throws IOException, URISyntaxException {
        Path testDir = prepareDirectory();
        Path pathToManifest = prepareFile(VALID_SIGNED_MANIFEST, testDir);
        Path pathToCertificate = prepareFile(VALID_MULTIPLE_CERTIFICATES, testDir);
        PackageUploadRequestContext context = prepareContext(pathToManifest, pathToCertificate);
        var signatureCaptor = ArgumentCaptor.forClass(String.class);
        var certificateCaptor = ArgumentCaptor.forClass(String.class);

        manifestSignatureValidation.handle(context);
        verify(signatureService).verifyContentSignature(any(), signatureCaptor.capture(), certificateCaptor.capture());

        String savedManifest = Files.readString(pathToManifest);
        String manifestWithoutSignature = Files.readString(getResource(VALID_MANIFEST_NO_SIGNATURE));
        String signature = Files.readString(getResource(SIGNATURE));
        String certificate = Files.readString(getResource(VALID_MULTIPLE_CERTIFICATES));

        assertThat(savedManifest).isEqualToIgnoringNewLines(manifestWithoutSignature);
        assertThat(signatureCaptor.getValue()).isEqualToIgnoringNewLines(signature);
        assertThat(certificateCaptor.getValue()).isEqualToIgnoringNewLines(certificate);
        assertThat(pathToCertificate.toFile().exists()).isEqualTo(false);
        assertThat(context.getPackageSecurityOption()).isEqualTo(AppPackage.PackageSecurityOption.OPTION_1);
    }

    @Test
    public void shouldSkipVerifyOption1WhenNoSignatureAndAllowUnsignedPackages() throws IOException, URISyntaxException {
        Path testDir = prepareDirectory();
        Path pathToManifest = prepareFile(VALID_MANIFEST_NO_SIGNATURE, testDir);
        PackageUploadRequestContext context = prepareContext(pathToManifest, null);
        manifestSignatureValidation.setAllowUnsignedVNFPackageOnboarding(true);
        manifestSignatureValidation.handle(context);
        verify(signatureService, never()).verifyContentSignature(any(), any(), any());

        assertThat(context.getPackageSecurityOption()).isEqualTo(AppPackage.PackageSecurityOption.UNSIGNED);
    }

    @Test
    public void shouldSkipVerifyOption1WhenNoManifestAndAlreadySigned() {
        PackageUploadRequestContext context = prepareContext(null, null);
        context.setPackageSecurityOption(AppPackage.PackageSecurityOption.OPTION_2);

        manifestSignatureValidation.handle(context);
        verify(signatureService, never()).verifyContentSignature(any(), any(), any());

        assertThat(context.getPackageSecurityOption()).isEqualTo(AppPackage.PackageSecurityOption.OPTION_2);
    }

    @Test
    public void shouldSkipVerifyOption1WhenNoManifestAndAllowUnsignedPackagesOnboarding() {
        PackageUploadRequestContext context = prepareContext(null, null);
        manifestSignatureValidation.setAllowUnsignedVNFPackageOnboarding(true);

        manifestSignatureValidation.handle(context);
        verify(signatureService, never()).verifyContentSignature(any(), any(), any());

        assertThat(context.getPackageSecurityOption()).isEqualTo(AppPackage.PackageSecurityOption.UNSIGNED);
    }

    @Test
    public void shouldFailVerifyOption1WhenNoSignatureAndNotAllowUnsignedPackages() throws IOException, URISyntaxException {
        Path testDir = prepareDirectory();
        Path pathToManifest = prepareFile(VALID_MANIFEST_NO_SIGNATURE, testDir);
        PackageUploadRequestContext context = prepareContext(pathToManifest, null);
        manifestSignatureValidation.setAllowUnsignedVNFPackageOnboarding(false);

        SignatureValidationException signatureValidationException = assertThrows(SignatureValidationException.class,
                                                                                 () -> manifestSignatureValidation.handle(context));
        assertEquals("Package with id: signed-package-id does not contain signature. Unsigned packages are not allowed.",
                     signatureValidationException.getMessage());
    }

    @Test
    public void shouldFailVerifyOption1WhenNoManifestAndNotAllowUnsignedPackages() {
        PackageUploadRequestContext context = prepareContext(null, null);
        manifestSignatureValidation.setAllowUnsignedVNFPackageOnboarding(false);

        SignatureValidationException signatureValidationException = assertThrows(SignatureValidationException.class,
                                                                                 () -> manifestSignatureValidation.handle(context));
        assertEquals("Package with id: signed-package-id does not contain signature. Unsigned packages are not allowed.",
                     signatureValidationException.getMessage());
    }

    @Test
    public void shouldFailVerifyOption1WhenSignatureServiceThrowsException() throws IOException, URISyntaxException {
        Path testDir = prepareDirectory();
        Path pathToManifest = prepareFile(VALID_SIGNED_MANIFEST, testDir);
        Path pathToCertificate = prepareFile(VALID_CERTIFICATE, testDir);
        PackageUploadRequestContext context = prepareContext(pathToManifest, pathToCertificate);
        doThrow(new SignatureVerificationException("Content's digest is different from the digest in signature."))
                .when(signatureService).verifyContentSignature(any(), any(), any());

        SignatureVerificationException signatureVerificationException = assertThrows(SignatureVerificationException.class,
                                                                                     () -> manifestSignatureValidation.handle(context));
        assertEquals("Content's digest is different from the digest in signature.",
                     signatureVerificationException.getMessage());
        assertThat(context.getPackageSecurityOption()).isEqualTo(null);
    }

    @Test
    public void shouldVerifySignatureWhenRetryManifestValidationWithOption2() throws IOException, URISyntaxException {
        //given
        Path testDir = prepareDirectory();
        Path pathToManifest = prepareFile(VALID_SIGNED_MANIFEST, testDir);
        Path pathToCertificate = prepareFile(VALID_CERTIFICATE, testDir);
        PackageUploadRequestContext context = prepareContext(pathToManifest, pathToCertificate);
        context.setPackageSecurityOption(AppPackage.PackageSecurityOption.OPTION_2);
        var signatureCaptor = ArgumentCaptor.forClass(String.class);
        var certificateCaptor = ArgumentCaptor.forClass(String.class);

        //when
        manifestSignatureValidation.handle(context);
        verify(signatureService).verifyContentSignature(any(), signatureCaptor.capture(), certificateCaptor.capture());

        //then
        String savedManifest = Files.readString(pathToManifest);
        String manifestWithoutSignature = Files.readString(getResource(VALID_MANIFEST_NO_SIGNATURE));
        String signature = Files.readString(getResource(SIGNATURE));
        String certificate = Files.readString(getResource(VALID_CERTIFICATE));

        assertThat(savedManifest).isEqualTo(manifestWithoutSignature);
        assertThat(signatureCaptor.getValue()).isEqualToIgnoringNewLines(signature);
        assertThat(certificateCaptor.getValue()).isEqualToIgnoringNewLines(certificate);
        assertThat(pathToCertificate.toFile().exists()).isEqualTo(false);
        assertThat(context.getPackageSecurityOption()).isEqualTo(AppPackage.PackageSecurityOption.OPTION_2);
    }

    @Test
    public void shouldNotVerifyContentWhenRetryManifestValidationWithOption1() throws IOException, URISyntaxException {
        //given
        Path testDir = prepareDirectory();
        Path pathToManifest = prepareFile(VALID_SIGNED_MANIFEST, testDir);
        Path pathToCertificate = prepareFile(VALID_CERTIFICATE, testDir);
        PackageUploadRequestContext context = prepareContext(pathToManifest, pathToCertificate);
        context.setPackageSecurityOption(AppPackage.PackageSecurityOption.OPTION_1);

        //when
        manifestSignatureValidation.handle(context);
        verify(signatureService, never()).verifyContentSignature(any(), any(), any());

        //then
        String savedManifest = Files.readString(pathToManifest);
        String manifestWithoutSignature = Files.readString(getResource(VALID_MANIFEST_NO_SIGNATURE));

        assertThat(savedManifest).isEqualTo(manifestWithoutSignature);
        assertThat(pathToCertificate.toFile().exists()).isEqualTo(false);
        assertThat(context.getPackageSecurityOption()).isEqualTo(AppPackage.PackageSecurityOption.OPTION_1);
    }

    @Test
    public void shouldNotVerifyContentWhenRetryManifestValidationWithUnsigned() throws IOException, URISyntaxException {
        //given
        Path testDir = prepareDirectory();
        Path pathToManifest = prepareFile(VALID_MANIFEST_NO_SIGNATURE, testDir);
        PackageUploadRequestContext context = prepareContext(pathToManifest, null);
        context.setPackageSecurityOption(AppPackage.PackageSecurityOption.UNSIGNED);

        //when
        manifestSignatureValidation.setAllowUnsignedVNFPackageOnboarding(true);
        manifestSignatureValidation.handle(context);
        verify(signatureService, never()).verifyContentSignature(any(), any(), any());

        //then
        assertThat(context.getPackageSecurityOption()).isEqualTo(AppPackage.PackageSecurityOption.UNSIGNED);
    }

    private Path prepareDirectory() {
        String directoryName = UUID.randomUUID().toString();
        return fileService.createDirectory(directoryName);
    }

    private Path prepareFile(String filePath, Path directory) throws IOException, URISyntaxException {
        return fileService.storeFile(Files.newInputStream(getResource(filePath)), directory,
                                     new File(filePath).getName());
    }

    private PackageUploadRequestContext prepareContext(final Path pathToManifest, final Path pathToCertificate) {
        PackageUploadRequestContext context = new PackageUploadRequestContext("testCsar.csar", null, LocalDateTime.now(), "signed-package-id");
        Map<String, Path> artifactsPaths = new HashMap<>();
        if (pathToManifest != null) {
            artifactsPaths.put(ENTRY_MANIFEST, pathToManifest.toAbsolutePath());
        }
        if (pathToCertificate != null) {
            artifactsPaths.put(ENTRY_CERTIFICATE, pathToCertificate.toAbsolutePath());
        }
        context.setArtifactPaths(artifactsPaths);
        return context;
    }
}