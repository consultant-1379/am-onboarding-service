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
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.presentation.exceptions.UnsupportedMediaTypeException;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.ericsson.amonboardingservice.utils.Constants;

@SpringBootTest
@ActiveProfiles("test")
public class UnpackCSARTest extends AbstractDbSetupTest {
    private static final String CSAR_WITH_INVALID_EXTENSION = "option2/spider-app-a-invalid-extension.vip";
    private static final String VALID_CSAR = "option2/spider-app-a.csar";

    @Autowired
    private UnpackCSAR unpackCSAR;

    @Autowired
    private FileService fileService;

    @Test
    public void shouldFailUnpackWhenCsarHasNonCsarExtension() throws IOException, URISyntaxException {
        Path testDir = prepareDirectory();
        Path testPackage = prepareFile(CSAR_WITH_INVALID_EXTENSION, testDir, Constants.VNF_PACKAGE_ZIP);
        Path pathToPackage = resolvePathToPackage(testPackage);
        PackageUploadRequestContext context = prepareContext("spider-app-a-invalid-extension.vip", pathToPackage);

        UnsupportedMediaTypeException unsupportedMediaTypeException = assertThrows(UnsupportedMediaTypeException.class,
                                                                                   () -> unpackCSAR.handle(context));
        assertEquals("Unsupported CSAR extension: .vip. Only .csar extension is supported.",
                     unsupportedMediaTypeException.getMessage());
    }

    @Test
    public void shouldUnpackWhenCsarIsSignedAndHasZipExtension() throws IOException, URISyntaxException {
        Path testDir = prepareDirectory();
        Path testPackage = prepareFile(VALID_CSAR, testDir);
        Path pathToPackage = resolvePathToPackage(testPackage);
        PackageUploadRequestContext context = prepareContext("spider-app-a.zip", pathToPackage);
        context.setPackageSigned(true);

        unpackCSAR.handle(context);
        Map<String, Path> artifactPaths = context.getArtifactPaths();
        Path csarDir = artifactPaths.get(Constants.CSAR_DIRECTORY);

        assertThat(artifactPaths.get(Constants.PATH_TO_PACKAGE)).isEqualTo(testPackage);
        assertThat(csarDir).isEqualTo(testDir);
        assertThat(Objects.requireNonNull(csarDir.toFile().listFiles()).length).isEqualTo(4);
    }

    @Test
    public void shouldUnpackWhenCsarIsNotSignedAndHasCsarExtension() throws IOException, URISyntaxException {
        Path testDir = prepareDirectory();
        Path testPackage = prepareFile(VALID_CSAR, testDir, Constants.VNF_PACKAGE_ZIP);
        Path pathToPackage = resolvePathToPackage(testPackage);
        PackageUploadRequestContext context = prepareContext("spider-app-a.csar", pathToPackage);

        unpackCSAR.handle(context);
        Map<String, Path> artifactPaths = context.getArtifactPaths();
        Path csarDir = artifactPaths.get(Constants.CSAR_DIRECTORY);

        assertThat(artifactPaths.get(Constants.PATH_TO_PACKAGE)).isEqualTo(pathToPackage);
        assertThat(csarDir).isEqualTo(testDir);
        assertThat(Objects.requireNonNull(csarDir.toFile().listFiles()).length).isEqualTo(4);
    }

    private PackageUploadRequestContext prepareContext(String originalFileName, final Path pathToPackageContent) {
        return new PackageUploadRequestContext(originalFileName, pathToPackageContent, LocalDateTime.now().plusMinutes(5), "signed-package-id");
    }

    private Path prepareDirectory() {
        String directoryName = UUID.randomUUID().toString();
        return fileService.createDirectory(directoryName);
    }

    private Path prepareFile(String filePath, Path directory, String fileName) throws IOException, URISyntaxException {
        return fileService.storeFile(Files.newInputStream(getResource(filePath)), directory, fileName);
    }

    private Path prepareFile(String filePath, Path directory) throws IOException, URISyntaxException {
        return prepareFile(filePath, directory, new File(filePath).getName());
    }

    private static Path resolvePathToPackage(final Path testPackage) {
        return testPackage.resolveSibling(Constants.VNF_PACKAGE_ZIP);
    }
}