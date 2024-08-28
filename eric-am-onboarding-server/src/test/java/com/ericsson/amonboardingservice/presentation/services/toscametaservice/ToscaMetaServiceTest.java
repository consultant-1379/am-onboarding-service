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
package com.ericsson.amonboardingservice.presentation.services.toscametaservice;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.presentation.exceptions.UnsupportedMediaTypeException;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileSystemService;
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
import java.util.Map;
import java.util.function.Predicate;

import static com.ericsson.amonboardingservice.TestUtils.createInputStream;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_DEFINITIONS;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_HELM_DEFINITIONS;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_IMAGES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
public class ToscaMetaServiceTest extends AbstractDbSetupTest {

    @TempDir
    public Path folder;

    @Autowired
    private ToscaMetaService toscaMetaService;

    @Autowired
    private FileSystemService fileSystemService;

    private File unpackedDirectory;

    @BeforeEach
    public void setupUnpackedDirectory() throws IOException, URISyntaxException {
        File structure = Files.createDirectory(folder.resolve("package")).toFile();
        Path zip = new File(structure, "zip").toPath();
        Files.copy(createInputStream("csar.zip"), zip);
        fileSystemService.unpack(zip, 15);
        unpackedDirectory = structure;
    }

    @Test
    public void shouldParseCorrectly() {
        Map<String, Path> paths = toscaMetaService.getArtifactsMapFromToscaMetaFile(unpackedDirectory.toPath());
        assertThat(paths)
                .isNotNull()
                .hasSize(3);
    }

    @Test
    public void shouldIncludeMissingFiles() throws IOException, URISyntaxException {
        File toscaMeta = new File(unpackedDirectory, "TOSCA-Metadata" + File.separator + "TOSCA.meta");
        toscaMeta.delete();
        Files.copy(createInputStream("TOSCA-Metadata" + File.separator + "TOSCA-missing-artifacts.meta"), toscaMeta.toPath());

        Map<String, Path> paths = toscaMetaService.getArtifactsMapFromToscaMetaFile(unpackedDirectory.toPath());

        assertThat(paths)
                .isNotNull()
                .hasSize(3)
                .containsKeys(ENTRY_DEFINITIONS, ENTRY_HELM_DEFINITIONS, ENTRY_IMAGES);
        for (Path path : paths.values()) {
            assertThat(path.toString()).matches(endsWithOneOf());
        }
    }

    private Predicate<String> endsWithOneOf() {
        return s -> s.endsWith("Definitions" + File.separator + "TOSCA.yaml")
                || s.endsWith("OtherTemplates" + File.separator + "eso-1.2.469.tgz")
                || s.endsWith("Files" + File.separator + "images" + File.separator + "docker.tar");
    }

    @Test
    public void shouldThrowExceptionForEmptyToscaMetaFile() throws IOException, URISyntaxException {
        File toscaMeta = new File(unpackedDirectory, "TOSCA-Metadata" + File.separator + "TOSCA.meta");
        toscaMeta.delete();
        Files.copy(createInputStream("TOSCA-Metadata" + File.separator + "TOSCA-empty.meta"), toscaMeta.toPath());

        assertThatThrownBy(() -> toscaMetaService.getArtifactsMapFromToscaMetaFile(unpackedDirectory.toPath()))
                .isInstanceOf(UnsupportedMediaTypeException.class)
                .hasMessageStartingWith("The TOSCA.meta file %s was found to be empty", toscaMeta.toPath());
    }

    @Test
    public void shouldThrowExceptionForNonExistentToscaMetaFile() throws IOException, URISyntaxException {
        File toscaMeta = new File(unpackedDirectory, "TOSCA-Metadata" + File.separator + "TOSCA.meta");
        toscaMeta.delete();
        assertThatThrownBy(() -> toscaMetaService.getArtifactsMapFromToscaMetaFile(unpackedDirectory.toPath()))
                .isInstanceOf(UnsupportedMediaTypeException.class)
                .hasMessageStartingWith("Only CSAR packages of type A1 are supported. The TOSCA.meta %s file was not found in the TOSCA-Metadata directory.",
                                        toscaMeta.toPath());
    }

    @Test
    public void shouldNotFailWhenLinesAreInvalid() throws IOException, URISyntaxException {
        File toscaMeta = new File(unpackedDirectory, "TOSCA-Metadata" + File.separator + "TOSCA.meta");
        toscaMeta.delete();
        Files.copy(createInputStream("TOSCA-Metadata" + File.separator + "TOSCA-invalid-lines.meta"), toscaMeta.toPath());
        Map<String, Path> paths = toscaMetaService.getArtifactsMapFromToscaMetaFile(unpackedDirectory.toPath());
        assertThat(paths)
                .isNotNull()
                .hasSize(3)
                .containsKeys(ENTRY_DEFINITIONS, ENTRY_HELM_DEFINITIONS, ENTRY_IMAGES);
    }

    @Test
    public void shouldThrownExceptionWhenToscaMetaFileIsBinary() throws IOException, URISyntaxException {
        File toscaMeta = new File(unpackedDirectory, "TOSCA-Metadata" + File.separator + "TOSCA.meta");
        toscaMeta.delete();
        Files.copy(createInputStream("TOSCA-Metadata" + File.separator + "TOSCA-binary.meta"), toscaMeta.toPath());
        assertThatThrownBy(() -> toscaMetaService.getArtifactsMapFromToscaMetaFile(unpackedDirectory.toPath()))
                .isInstanceOf(UnsupportedMediaTypeException.class)
                .hasMessageStartingWith("The TOSCA.meta file %s type is incorrect, text/plain is all that is supported.", toscaMeta.toPath());
    }
}