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

import static com.ericsson.amonboardingservice.TestUtils.getResource;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.presentation.exceptions.UnsupportedMediaTypeException;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;

@SpringBootTest
@ActiveProfiles("test")
public class UnpackZIPTest extends AbstractDbSetupTest {
    private static final String VALID_ZIP_OPTION_2 = "option2/spider-app-a.zip";
    private static final String VALID_ZIP_NO_CSAR = "option2/spider-app-a-no-csar.zip";
    private static final String VALID_ZIP_NO_SIGNATURE = "option2/spider-app-a-no-signature.zip";

    @Autowired
    private UnpackZIP unpackZIP;

    @Autowired
    private FileService fileService;

    @Test
    public void shouldUnpackZipWhenOption2SignatureValidation() throws IOException, URISyntaxException {
        Path testDir = prepareDirectory();
        Path pathToPackage = prepareFile(VALID_ZIP_OPTION_2, testDir);
        PackageUploadRequestContext context = prepareContext("spider-app-a.zip", pathToPackage);
        unpackZIP.handle(context);
        assertThat(Objects.requireNonNull(pathToPackage.getParent().toFile().listFiles()).length).isEqualTo(3);
        assertThat(context.isPackageSigned()).isTrue();
    }

    @Test
    public void shouldSkipUnpackZipWhenCsarArchive() throws IOException, URISyntaxException {
        Path testDir = prepareDirectory();
        Path pathToPackage = prepareFile(VALID_ZIP_OPTION_2, testDir);
        PackageUploadRequestContext context = prepareContext("spider-app-a.csar", pathToPackage);
        unpackZIP.handle(context);
        assertThat(Objects.requireNonNull(pathToPackage.getParent().toFile().listFiles()).length).isEqualTo(1);
    }

    @Test
    public void shouldFailUnpackZipWhenNoCsarFile() throws IOException, URISyntaxException {
        Path testDir = prepareDirectory();
        Path pathToPackage = prepareFile(VALID_ZIP_NO_CSAR, testDir);
        PackageUploadRequestContext context = prepareContext("spider-app-a-no-csar.zip", pathToPackage);

        UnsupportedMediaTypeException unsupportedMediaTypeException = assertThrows(UnsupportedMediaTypeException.class,
                                                                                   () -> unpackZIP.handle(context));
        assertEquals("Cannot locate csar archive in spider-app-a-no-csar.zip.",
                     unsupportedMediaTypeException.getMessage());
    }

    @Test
    public void shouldFailUnpackZipWhenNoSignatureFile() throws IOException, URISyntaxException {
        Path testDir = prepareDirectory();
        Path pathToPackage = prepareFile(VALID_ZIP_NO_SIGNATURE, testDir);
        PackageUploadRequestContext context = prepareContext("spider-app-a-no-signature.zip", pathToPackage);

        UnsupportedMediaTypeException unsupportedMediaTypeException = assertThrows(UnsupportedMediaTypeException.class,
                                                                                   () -> unpackZIP.handle(context));
        assertEquals("Cannot locate signature in spider-app-a-no-signature.zip.",
                     unsupportedMediaTypeException.getMessage());
    }

    private PackageUploadRequestContext prepareContext(String originalFileName, final Path pathToPackageContent) {
        return new PackageUploadRequestContext(originalFileName, pathToPackageContent, LocalDateTime.now().plusMinutes(5), "signed-package-id");
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