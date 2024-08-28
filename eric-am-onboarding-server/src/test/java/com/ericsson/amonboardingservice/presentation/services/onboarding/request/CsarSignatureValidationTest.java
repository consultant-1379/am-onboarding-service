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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import static com.ericsson.amonboardingservice.TestUtils.getResource;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
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
public class CsarSignatureValidationTest extends AbstractDbSetupTest {

    private static final String VALID_CSAR = "option2/spider-app-a.csar";
    private static final String VALID_CSAR_WITH_CRL = "option2/spider-app-a-and-crl.csar";
    private static final String VALID_CERT = "option2/spider-app-a.cert";
    private static final String VALID_CERT_AND_CRL = "option2/spider-app-a-and-crl.cert";
    private static final String VALID_SECOND_CERT = "option2/spider-app-b.cert";
    private static final String VALID_SIGNATURE = "option2/spider-app-a.cms";
    private static final String VALID_SIGNATURE_AND_CRL = "option2/spider-app-a-and-crl.cms";
    private static final String INVALID_SIGNATURE_NAME = "option2/invalid_signature_name.cms";

    @Autowired
    @InjectMocks
    private CsarSignatureValidation csarSignatureValidation;

    @MockBean
    private SignatureService signatureService;

    @Autowired
    private FileService fileService;

    @Test
    public void shouldVerifyOption2WhenCertificateIsSeparateFile() throws IOException, URISyntaxException {
        Path testDir = prepareDirectory();
        Path pathToCsar = prepareFile(VALID_CSAR, testDir);
        Path pathToCertificate = prepareFile(VALID_CERT, testDir);
        Path pathToSignature = prepareFile(VALID_SIGNATURE, testDir);
        PackageUploadRequestContext context = prepareContext(pathToCsar);
        var signatureCaptor = ArgumentCaptor.forClass(String.class);
        var certificateCaptor = ArgumentCaptor.forClass(String.class);
        csarSignatureValidation.handle(context);
        verify(signatureService).verifyContentSignature(any(), signatureCaptor.capture(), certificateCaptor.capture());

        String signature = Files.readString(getResource(VALID_SIGNATURE));
        String certificate = Files.readString(getResource(VALID_CERT));
        assertThat(signatureCaptor.getValue()).isEqualToIgnoringNewLines(signature);
        assertThat(certificateCaptor.getValue()).isEqualToIgnoringNewLines(certificate);
        assertThat(pathToCertificate.toFile().exists()).isEqualTo(false);
        assertThat(pathToSignature.toFile().exists()).isEqualTo(false);
        assertThat(context.getPackageSecurityOption()).isEqualTo(AppPackage.PackageSecurityOption.OPTION_2);
    }

    @Test
    public void shouldVerifyOption2WhenSignatureAndCertificateContainCrl() throws IOException, URISyntaxException {
        Path testDir = prepareDirectory();
        Path pathToCsar = prepareFile(VALID_CSAR_WITH_CRL, testDir);
        Path pathToCertificate = prepareFile(VALID_CERT_AND_CRL, testDir);
        Path pathToSignature = prepareFile(VALID_SIGNATURE_AND_CRL, testDir);
        PackageUploadRequestContext context = prepareContext(pathToCsar);
        var signatureCaptor = ArgumentCaptor.forClass(String.class);
        var certificateCaptor = ArgumentCaptor.forClass(String.class);
        csarSignatureValidation.handle(context);
        verify(signatureService).verifyContentSignature(any(), signatureCaptor.capture(), certificateCaptor.capture());

        String signature = Files.readString(getResource(VALID_SIGNATURE_AND_CRL));
        String certificate = Files.readString(getResource(VALID_CERT_AND_CRL));
        assertThat(signatureCaptor.getValue()).isEqualToIgnoringNewLines(signature);
        assertThat(certificateCaptor.getValue()).isEqualToIgnoringNewLines(certificate);
        assertThat(pathToCertificate.toFile().exists()).isEqualTo(false);
        assertThat(pathToSignature.toFile().exists()).isEqualTo(false);
        assertThat(context.getPackageSecurityOption()).isEqualTo(AppPackage.PackageSecurityOption.OPTION_2);
    }

    @Test
    public void shouldVerifyOption2WhenCertificateIsInsideSignature() throws IOException, URISyntaxException {
        Path testDir = prepareDirectory();
        Path pathToCsar = prepareFile(VALID_CSAR, testDir);
        Path pathToSignature = prepareFile(VALID_SIGNATURE, testDir);
        PackageUploadRequestContext context = prepareContext(pathToCsar);
        var signatureCaptor = ArgumentCaptor.forClass(String.class);
        csarSignatureValidation.handle(context);
        verify(signatureService).verifyContentSignature(any(), signatureCaptor.capture(), isNull());

        String signature = Files.readString(getResource(VALID_SIGNATURE));
        assertThat(signatureCaptor.getValue()).isEqualToIgnoringNewLines(signature);
        assertThat(pathToSignature.toFile().exists()).isEqualTo(false);
        assertThat(context.getPackageSecurityOption()).isEqualTo(AppPackage.PackageSecurityOption.OPTION_2);
    }

    @Test
    public void shouldSkipVerifyOption2WhenSignatureNotPresent() throws IOException, URISyntaxException {
        Path testDir = prepareDirectory();
        Path pathToCsar = prepareFile(VALID_CSAR, testDir);
        PackageUploadRequestContext context = prepareContext(pathToCsar);
        csarSignatureValidation.handle(context);
        verify(signatureService, never()).verifyContentSignature(any(), any(), any());

        assertThat(context.getPackageSecurityOption()).isEqualTo(null);
    }

    @Test
    public void shouldFailVerifyOption2WhenMultipleCertificateFiles() throws IOException, URISyntaxException {
        Path testDir = prepareDirectory();
        Path pathToCsar = prepareFile(VALID_CSAR, testDir);
        prepareFile(VALID_CERT, testDir);
        prepareFile(VALID_SECOND_CERT, testDir);
        prepareFile(VALID_SIGNATURE, testDir);
        PackageUploadRequestContext context = prepareContext(pathToCsar);

        SignatureValidationException signatureValidationException = assertThrows(SignatureValidationException.class,
                                                                                 () -> csarSignatureValidation.handle(context));
        assertEquals(
                "For Option 2 signature verification Zip archive should only contain Csar, one Signature file and optionally one Certificate file.",
                signatureValidationException.getMessage());
    }

    @Test
    public void shouldFailVerifyOption2WhenFileNamesAreNotIdentical() throws IOException, URISyntaxException {
        Path testDir = prepareDirectory();
        Path pathToCsar = prepareFile(VALID_CSAR, testDir);
        prepareFile(VALID_CERT, testDir);
        prepareFile(INVALID_SIGNATURE_NAME, testDir);
        PackageUploadRequestContext context = prepareContext(pathToCsar);

        SignatureValidationException signatureValidationException = assertThrows(SignatureValidationException.class,
                                                                                 () -> csarSignatureValidation.handle(context));
        assertEquals("Csar, Certificate and Signature file names must be identical for Option 2 signature verification.",
                     signatureValidationException.getMessage());
    }

    @Test
    public void shouldFailVerifyOption2WhenSignatureServiceThrowsException() throws IOException, URISyntaxException {
        Path testDir = prepareDirectory();
        Path pathToCsar = prepareFile(VALID_CSAR, testDir);
        prepareFile(VALID_CERT, testDir);
        prepareFile(VALID_SIGNATURE, testDir);
        doThrow(new SignatureVerificationException("Content's digest is different from the digest in signature."))
                .when(signatureService).verifyContentSignature(any(), any(), any());
        PackageUploadRequestContext context = prepareContext(pathToCsar);

        SignatureVerificationException signatureVerificationException = assertThrows(SignatureVerificationException.class,
                                                                                     () -> csarSignatureValidation.handle(context));
        assertEquals("Content's digest is different from the digest in signature.",
                     signatureVerificationException.getMessage());
        assertThat(context.getPackageSecurityOption()).isEqualTo(null);
    }

    @Test
    public void shouldNotVerifyOption2WhenRetryCsarValidation() throws IOException, URISyntaxException {
        //given
        Path testDir = prepareDirectory();
        Path pathToCsar = prepareFile(VALID_CSAR, testDir);
        Path pathToCertificate = prepareFile(VALID_CERT, testDir);
        Path pathToSignature = prepareFile(VALID_SIGNATURE, testDir);
        PackageUploadRequestContext context = prepareContext(pathToCsar);
        context.setPackageSecurityOption(AppPackage.PackageSecurityOption.OPTION_1);

        //when
        csarSignatureValidation.handle(context);

        //then
        verifyNoInteractions(signatureService);
        assertThat(pathToCertificate.toFile().exists()).isEqualTo(false);
        assertThat(pathToSignature.toFile().exists()).isEqualTo(false);
        assertThat(context.getPackageSecurityOption()).isEqualTo(AppPackage.PackageSecurityOption.OPTION_2);
    }

    private PackageUploadRequestContext prepareContext(final Path pathToPackageContent) {
        return new PackageUploadRequestContext("testCsar.csar", pathToPackageContent, LocalDateTime.now(), "signed-package-id");
    }

    private Path prepareDirectory() {
        String directoryName = UUID.randomUUID().toString();
        return fileService.createDirectory(directoryName);
    }

    private Path prepareFile(String filePath, Path directory) throws IOException, URISyntaxException {
        return fileService.storeFile(Files.newInputStream(getResource(filePath)), directory,
                                     new File(filePath).getName());
    }
}