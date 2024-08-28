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
package com.ericsson.amonboardingservice.presentation.services;

import com.ericsson.amonboardingservice.TestUtils;
import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileSystemService;
import com.ericsson.amonboardingservice.utils.Constants;
import com.ericsson.amonboardingservice.utils.executor.ProcessExecutor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import static com.ericsson.amonboardingservice.TestUtils.createInputStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@SpringBootTest(classes = {
        FileSystemService.class,
        ProcessExecutor.class })
@ActiveProfiles("test")
public final class FileSystemServiceTest {

    @Autowired
    private FileSystemService fileSystemService;
    private static final String TEST_CHART_TGZ = "eso-1.2.469.tgz";
    private static final String CMD_PROCESS_INPUT_MESSAGE = "due to: Archive:";
    private static final String CMD_PROCESS_ERROR_OUTPUT_INCORRECT_FILE_EXTENSION = "error details: End-of-central-directory signature not found.";
    @TempDir
    public Path folder;

    @BeforeEach
    public void setUp() {
        String rootDirectory = folder.toString();
        setField(fileSystemService, "rootDirectory", rootDirectory);
    }

    @Test
    public void unpackFileWhichExists() throws IOException, URISyntaxException {
        Path zip = Files.createFile(folder.resolve("test.zip"));
        Files.copy(createInputStream("zip_contents"), zip, StandardCopyOption.REPLACE_EXISTING);

        fileSystemService.unpack(zip, 15);

        assertThat(folder.toFile().listFiles())
                .hasSize(1)
                .extracting(File::getName)
                .contains("sample");
    }

    @Test
    public void unpackFileWhichIsNotAZip() throws IOException, URISyntaxException {
        Path zip = Files.createFile(folder.resolve("test.zip"));
        Files.copy(createInputStream("logback-test.xml"), zip, StandardCopyOption.REPLACE_EXISTING);

        assertThatThrownBy(() -> fileSystemService.unpack(zip, 15))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(Constants.FAILED_TO_EXECUTE_COMMAND_MESSAGE.split(":")[0])
                .hasMessageContaining(CMD_PROCESS_INPUT_MESSAGE)
                .hasMessageContaining(CMD_PROCESS_ERROR_OUTPUT_INCORRECT_FILE_EXTENSION);
    }

    @Test
    public void createDirectory() {
        fileSystemService.createDirectory("myPackage");
        assertThat(folder.toFile().listFiles())
                .extracting(File::getName)
                .containsOnly("myPackage");
    }

    @Test
    public void storePackage() throws IOException, URISyntaxException {
        Path packagePath = fileSystemService.createDirectory("myPackage");
        fileSystemService.storeFile(createInputStream("zip_contents"), packagePath,
                                    Constants.VNF_PACKAGE_ZIP);
        assertThat(packagePath.toFile().listFiles())
                .hasSize(1)
                .extracting(File::getName)
                .containsOnly(Constants.VNF_PACKAGE_ZIP);
    }

    @Test
    public void getFilePath() throws IOException, URISyntaxException {
        Path packagePath = fileSystemService.createDirectory("myPackage");
        Path csarPath = fileSystemService.storeFile(createInputStream("csar.zip"), packagePath,
                                                    Constants.VNF_PACKAGE_ZIP);
        fileSystemService.unpack(csarPath, 15);
        Path chartPath = FileSystemService.getFilePathWithinExtractedPath(packagePath,
                                                                          "/Definitions/OtherTemplates", "tgz").get();
        assertThat(chartPath).hasFileName(TEST_CHART_TGZ);
    }

    @Test
    public void getFilePathNotFound() throws IOException, URISyntaxException {
        Path packagePath = fileSystemService.createDirectory("myPackage");
        Path csarPath = fileSystemService.storeFile(createInputStream("csar.zip"), packagePath,
                                                    Constants.VNF_PACKAGE_ZIP);
        fileSystemService.unpack(csarPath, 15);
        assertThat(FileSystemService.getFilePathWithinExtractedPath(packagePath,
                                                                    "/Definitions/OtherTemplates", "nofile")).isEmpty();
    }

    @Test
    public void testCreateTempPath() {
        Path temporaryPath = fileSystemService.createTempPath("chart.tgz");
        assertThat(temporaryPath).hasFileName("chart.tgz");
        int directoryDepth = temporaryPath.getNameCount();
        assertThat(temporaryPath.getName(directoryDepth - 3)).hasFileName("temporary");
    }

    @Test
    public void testCreateZip() throws IOException {
        try {
            Path yamlFile = createVnfdYamlFile();
            Path zipFile = fileSystemService.zipDirectory(yamlFile.getParent());
            assertThat(zipFile).isNotNull();
            assertThat(zipFile.getFileName()).isNotNull();
            fileSystemService.deleteDirectory(zipFile.getParent().toString());
            fileSystemService.deleteDirectory(yamlFile.getParent().toString());
        } catch (Exception ex) {
            fail(ex.getMessage());
            throw ex;
        }
    }

    @Test
    public void shouldGetContentFromFile() throws IOException, URISyntaxException {
        Path directory = fileSystemService.createDirectory("test-file-content");
        Path path = fileSystemService.storeFile(createInputStream("bootstrap.yml"), directory, "bootstrap.yml");
        byte[] expectedResult = "logging:".getBytes();

        assertThat(fileSystemService.getFileContent(path)).contains(expectedResult);
        fileSystemService.deleteDirectory(directory.toString());
    }

    @Test
    public void shouldFailGetContentFromFileWhenFileIsNotPresent() {
        Path file = Paths.get("");
        assertThrows(InternalRuntimeException.class, () -> fileSystemService.getFileContent(file));
    }

    @Test
    public void shouldDeleteRootDirectoryContent() throws IOException, URISyntaxException {
        // given
        File tmpFolder = Files.createDirectory(folder.resolve(UUID.randomUUID().toString())).toFile();
        Path zip = tmpFolder.toPath().resolve("test.zip");
        Files.copy(createInputStream("zip_contents"), zip, StandardCopyOption.REPLACE_EXISTING);

        fileSystemService.unpack(zip, 15);

        // when
        fileSystemService.deleteTempDirectories();

        // then
        assertThat(folder.getRoot()).isDirectoryNotContaining(file -> file.equals(tmpFolder));
    }

    @Test
    public void testValidateCsarFileForZipSlipAttack() throws URISyntaxException {
        Path csarFile = TestUtils.getResource("sampledescriptor.csar").toAbsolutePath();

        assertThatNoException()
                .isThrownBy(() -> fileSystemService.validateCsarForRelativePaths(csarFile, 15));
    }


    @Test
    public void testValidateCsarFileForZipSlipAttackWithVulnerableFile(@TempDir Path temporaryFolder) throws IOException, URISyntaxException {
        File file = Files.createFile(temporaryFolder.resolve("zip-slip.zip")).toFile();
        Path csarFile = TestUtils.getResource("zip-slip.zip").toAbsolutePath();
        Files.copy(csarFile, file.toPath(), StandardCopyOption.REPLACE_EXISTING);

        assertThatThrownBy(() -> fileSystemService.validateCsarForRelativePaths(file.toPath(), 15))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(Constants.ZIP_SLIP_ATTACK_ERROR_MESSAGE);
    }

    @Test
    public void testIsEnoughSpaceInRootDirectory() {
        assertThatNoException().isThrownBy(() -> {
            fileSystemService.isEnoughSpaceInRootDirectory(
                    new MockMultipartFile("any", "any-dummy-content".getBytes()).getSize());
        });
    }

    private Path createVnfdYamlFile() throws JsonProcessingException {
        JsonNode jsonNodeTree = new ObjectMapper().readTree("{\"test\": \"dummy_string\"}");
        String jsonAsYaml = new YAMLMapper().writeValueAsString(jsonNodeTree);
        String directoryName = UUID.randomUUID().toString();
        Path directory = fileSystemService.createDirectory(directoryName);
        return fileSystemService.storeFile(
                new ByteArrayInputStream(jsonAsYaml.getBytes(StandardCharsets.UTF_8)), directory, Constants.VNFD_FILE_NAME);
    }
}