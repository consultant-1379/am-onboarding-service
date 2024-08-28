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
package com.ericsson.amonboardingservice.presentation.services.manifestservice;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.presentation.exceptions.ErrorMessage;
import com.ericsson.amonboardingservice.presentation.exceptions.FailedOnboardingValidationException;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileSystemService;
import com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ericsson.amonboardingservice.TestUtils.createInputStream;
import static com.ericsson.amonboardingservice.TestUtils.getResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest
@ActiveProfiles("test")
public class ManifestServiceTest extends AbstractDbSetupTest {

    @TempDir
    public Path folder;

    @Autowired
    private ManifestServiceImpl manifestService;

    @Autowired
    private ToscaMetaService toscaMetaService;

    @Autowired
    private FileSystemService fileSystemService;

    private File unpackedDirectory;

    @BeforeEach
    public void setupUnpackedDirectory() throws IOException, URISyntaxException {
        File structure = Files.createDirectory(folder.resolve("package")).toFile();
        Path zip = new File(structure, "zip").toPath();
        Files.copy(createInputStream("sampledescriptor.csar"), zip);
        fileSystemService.unpack(zip, 15);
        unpackedDirectory = structure;
    }

    @Test
    public void shouldFindManifestFile(){
        final Map<String, Path> artifacts = toscaMetaService.getArtifactsMapFromToscaMetaFile(unpackedDirectory.toPath());
        Optional<Path> manifest = manifestService.getPathToManifestFile(artifacts);
        assertThat(manifest).isNotNull().isNotEmpty();
    }

    @Test
    public void shouldNotFindManifestFile() throws IOException, URISyntaxException {
        File structure = Files.createDirectory(folder.resolve("package-no-manifest")).toFile();
        Path zip = new File(structure, "zip").toPath();
        Files.copy(createInputStream("csar.zip"), zip);
        fileSystemService.unpack(zip, 15);
        final Map<String, Path> artifacts = toscaMetaService.getArtifactsMapFromToscaMetaFile(structure.toPath());
        Optional<Path> manifest = manifestService.getPathToManifestFile(artifacts);
        assertThat(manifest).isNotNull().isEmpty();
    }

    @Test
    public void shouldNotFailWhenNoManifestFilePresent(){
        File manifestFile = new File(unpackedDirectory, "sample_descriptor_v4.mf");
        manifestFile.delete();
        final Map<String, Path> artifacts = toscaMetaService.getArtifactsMapFromToscaMetaFile(unpackedDirectory.toPath());
        try {
            manifestService.validateDigests(unpackedDirectory.toPath(), artifacts);
        } catch (Exception e){
            fail("An exception should not have been thrown");
        }
    }

    @Test
    public void shouldNotFailForManifestFileWithNoDigests() throws IOException, URISyntaxException {
        File manifestFile = new File(unpackedDirectory, "sample_descriptor_v4.mf");
        manifestFile.delete();
        Files.copy(createInputStream("manifest" + File.separator + "no_digests.mf"), manifestFile.toPath());
        final Map<String, Path> artifacts = toscaMetaService.getArtifactsMapFromToscaMetaFile(unpackedDirectory.toPath());
        try {
            manifestService.validateDigests(unpackedDirectory.toPath(), artifacts);
        } catch (Exception e){
            fail("An exception should not have been thrown");
        }
    }

    @Test
    public void shouldThrowExceptionForEmptyManifestFile() throws IOException, URISyntaxException {
        File manifestFile = new File(unpackedDirectory, "sample_descriptor_v4.mf");
        manifestFile.delete();
        Files.copy(createInputStream("manifest" + File.separator + "empty.mf"), manifestFile.toPath());
        final Map<String, Path> artifacts = toscaMetaService.getArtifactsMapFromToscaMetaFile(unpackedDirectory.toPath());
        assertThatThrownBy(() -> manifestService.validateDigests(unpackedDirectory.toPath(), artifacts))
                .isInstanceOf(FailedOnboardingValidationException.class)
                .hasMessageStartingWith("The Manifest file %s was found to be empty", manifestFile);
    }

    @Test
    public void shouldFilterOutIrrelevantLines(){
        final Map<String, Path> artifacts = toscaMetaService.getArtifactsMapFromToscaMetaFile(unpackedDirectory.toPath());
        Optional<Path> manifest = manifestService.getPathToManifestFile(artifacts);
        List<String> relevantLines = manifestService.getManifestLinesWithHashes(manifest.get());
        assertThat(relevantLines).hasSize(18);
    }

    @Test
    public void createListOfPojosFromLines(){
        final Map<String, Path> artifacts = toscaMetaService.getArtifactsMapFromToscaMetaFile(unpackedDirectory.toPath());
        Optional<Path> manifest = manifestService.getPathToManifestFile(artifacts);
        List<String> relevantLines = manifestService.getManifestLinesWithHashes(manifest.get());
        List<Digest> digests = manifestService.createDigests(relevantLines, unpackedDirectory.toPath());
        assertThat(digests).hasSize(6);
    }

    @Test
    public void createListOfPojosFromLinesInDifferentOrder() throws IOException, URISyntaxException {
        File manifestFile = new File(unpackedDirectory, "sample_descriptor_v4.mf");
        manifestFile.delete();
        Files.copy(createInputStream("manifest" + File.separator + "different_order.mf"), manifestFile.toPath());
        final Map<String, Path> artifacts = toscaMetaService.getArtifactsMapFromToscaMetaFile(unpackedDirectory.toPath());
        Optional<Path> manifest = manifestService.getPathToManifestFile(artifacts);
        List<String> relevantLines = manifestService.getManifestLinesWithHashes(manifest.get());
        List<Digest> digests = manifestService.createDigests(relevantLines, unpackedDirectory.toPath());
        assertThat(digests).hasSize(6);
    }

    @Test
    public void shouldSuccessfullyValidateMatchingDigests(){
        final Map<String, Path> artifacts = toscaMetaService.getArtifactsMapFromToscaMetaFile(unpackedDirectory.toPath());
        Optional<Path> manifest = manifestService.getPathToManifestFile(artifacts);
        List<String> relevantLines = manifestService.getManifestLinesWithHashes(manifest.get());
        List<Digest> digests = manifestService.createDigests(relevantLines, unpackedDirectory.toPath());
        List<ErrorMessage> errors = manifestService.validateDigests(digests);
        assertThat(errors).isEmpty();
    }

    @Test
    public void shouldThrowExceptionWhenOneDigestDoesntMatch() throws IOException, URISyntaxException {
        File manifestFile = new File(unpackedDirectory, "sample_descriptor_v4.mf");
        manifestFile.delete();
        Files.copy(createInputStream("manifest" + File.separator + "one_incorrect.mf"), manifestFile.toPath());
        final Map<String, Path> artifacts = toscaMetaService.getArtifactsMapFromToscaMetaFile(unpackedDirectory.toPath());
        assertThatThrownBy(() -> manifestService.validateDigests(unpackedDirectory.toPath(), artifacts))
            .isInstanceOf(FailedOnboardingValidationException.class).matches(o -> {
            FailedOnboardingValidationException e = (FailedOnboardingValidationException) o;
            return e.getErrors().size() == 1;
        });
    }

    @Test
    public void shouldThrowExceptionWhenMultipleDigestsDontMatch() throws IOException, URISyntaxException {
        File manifestFile = new File(unpackedDirectory, "sample_descriptor_v4.mf");
        manifestFile.delete();
        Files.copy(createInputStream("manifest" + File.separator + "multiple_incorrect.mf"), manifestFile.toPath());
        final Map<String, Path> artifacts = toscaMetaService.getArtifactsMapFromToscaMetaFile(unpackedDirectory.toPath());
        assertThatThrownBy(() -> manifestService.validateDigests(unpackedDirectory.toPath(), artifacts))
                .isInstanceOf(FailedOnboardingValidationException.class).matches(o -> {
            FailedOnboardingValidationException e = (FailedOnboardingValidationException) o;
            return e.getErrors().size() == 3;
        });
    }

    @Test
    public void shouldThrowExceptionWhenManifestPointsToNonExistentFile() throws IOException, URISyntaxException {
        File manifestFile = new File(unpackedDirectory, "sample_descriptor_v4.mf");
        manifestFile.delete();
        Files.copy(createInputStream("manifest" + File.separator + "missing_file.mf"), manifestFile.toPath());
        final Map<String, Path> artifacts = toscaMetaService.getArtifactsMapFromToscaMetaFile(unpackedDirectory.toPath());
        assertThatThrownBy(() -> manifestService.validateDigests(unpackedDirectory.toPath(), artifacts))
                .isInstanceOf(FailedOnboardingValidationException.class).matches(o -> {
            FailedOnboardingValidationException e = (FailedOnboardingValidationException) o;
            return e.getErrors().size() == 1;
        });
    }

    @Test
    public void shouldThrowExceptionWhenFieldIsMissingForOneFileInManifest() throws IOException, URISyntaxException {
        File manifestFile = new File(unpackedDirectory, "sample_descriptor_v4.mf");
        manifestFile.delete();
        Files.copy(createInputStream("manifest" + File.separator + "missing_field.mf"), manifestFile.toPath());
        final Map<String, Path> artifacts = toscaMetaService.getArtifactsMapFromToscaMetaFile(unpackedDirectory.toPath());
        assertThatThrownBy(() -> manifestService.validateDigests(unpackedDirectory.toPath(), artifacts))
                .isInstanceOf(FailedOnboardingValidationException.class).hasMessageStartingWith("An entry in the Manifest file is missing a required field");
    }

    @Test
    public void shouldThrowExceptionWhenLineIsMalformed() throws IOException, URISyntaxException {
        File manifestFile = new File(unpackedDirectory, "sample_descriptor_v4.mf");
        manifestFile.delete();
        Files.copy(createInputStream("manifest" + File.separator + "malformed_entry.mf"), manifestFile.toPath());
        final Map<String, Path> artifacts = toscaMetaService.getArtifactsMapFromToscaMetaFile(unpackedDirectory.toPath());
        assertThatThrownBy(() -> manifestService.validateDigests(unpackedDirectory.toPath(), artifacts))
                .isInstanceOf(FailedOnboardingValidationException.class).hasMessageStartingWith("An entry in the Manifest file has a malformed field");
    }

    @Test
    public void shouldWorkForSol004ComplaintSHA() throws IOException, URISyntaxException {
        File manifestFile = new File(unpackedDirectory, "sample_descriptor_v4.mf");
        manifestFile.delete();
        Files.copy(createInputStream("manifest" + File.separator + "sol004_sha.mf"), manifestFile.toPath());
        final Map<String, Path> artifacts = toscaMetaService.getArtifactsMapFromToscaMetaFile(unpackedDirectory.toPath());

        Optional<Path> manifest = manifestService.getPathToManifestFile(artifacts);
        List<String> relevantLines = manifestService.getManifestLinesWithHashes(manifest.get());
        List<Digest> digests = manifestService.createDigests(relevantLines, unpackedDirectory.toPath());
        List<ErrorMessage> errors = manifestService.validateDigests(digests);
        assertThat(digests).hasSize(6);
        assertThat(errors).isEmpty();
    }

    @Test
    public void compareMatchingFiles() throws URISyntaxException {
        assertThat(manifestService.compareArtifacts(getResource("sampledescriptor-0.0.1-223.tgz"), getResource("sampledescriptor-0.0.1-223.tgz"))).isTrue();
    }

    @Test
    public void compareDifferentFiles() throws URISyntaxException {
        assertThat(manifestService.compareArtifacts(getResource("sampledescriptor-0.0.1-223.tgz"), getResource("get_catalog_response.json"))).isFalse();
    }

    @Test
    public void compareNonExistentFile() throws URISyntaxException {
        assertThat(manifestService.compareArtifacts(getResource("sampledescriptor-0.0.1-223.tgz"), Paths.get("non-existent.tgz"))).isFalse();
    }
}
