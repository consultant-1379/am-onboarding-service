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
package com.ericsson.amonboardingservice.presentation.services.dockerservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import static com.ericsson.amonboardingservice.TestUtils.createInputStream;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.TestUtils;
import com.ericsson.amonboardingservice.model.ImageResponse;
import com.ericsson.amonboardingservice.presentation.exceptions.CommandTimedOutException;
import com.ericsson.amonboardingservice.presentation.exceptions.DockerServiceException;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileSystemService;
import com.ericsson.amonboardingservice.presentation.services.imageservice.ImageService;
import com.ericsson.amonboardingservice.utils.Constants;
import com.ericsson.amonboardingservice.infrastructure.client.RestClient;
import com.ericsson.amonboardingservice.utils.executor.ProcessExecutor;
import com.ericsson.amonboardingservice.utils.executor.ProcessExecutorResponse;

@SpringBootTest(properties = "onboarding.upload.concurrency=high")
public final class DockerServiceConcurrencyTest extends AbstractDbSetupTest {

    private static final String LAYER_TAR = "layer.tar";
    private static final String CONFIG_JSON_1 = "219ee5171f8006d1462fa76c12b9b01ab672dbc8b283f186841bf2c3ca8e3c94.json";
    private static final String CONFIG_JSON_2 = "319ee5171f8006d1462fa76c12b9b01ab672dbc8b283f186841bf2c3ca8e3c94.json";
    private static final String CONCURRENT_MANIFEST_PATH = "docker/concurrent/" + Constants.MANIFEST_JSON;
    private static final String LINK_LAYER_FOLDER = "6b4ef804af565ff504bf7693239e5f26b4782484f068226b7244117760603d0d";
    private static final String TARGET_LAYER_FOLDER = "78c89d488790185b06835cc26e8b0ff4b8c09dbca2803a96c456f951c37a42b7";
    private static final String FOLDER_PREFIX = "5b4ef804af565ff504bf7693239e5f26b4782484f068226b7244117760603d05";
    public static final String UPLOAD_LAYER_URL = "url";
    private static final String DOCKER_TAR = "docker.tar";
    private static final String DOCKER_TAR_PATH = "docker/concurrent/docker.tar";

    @Autowired
    private DockerService dockerService;

    @MockBean
    private RestClient restClient;

    @MockBean
    private ImageService imageService;

    @SpyBean
    private ProcessExecutor processExecutor;

    @SpyBean
    private ContainerRegistryService containerRegistryService;

    @SpyBean
    private FileSystemService fileSystemService;

    @BeforeEach
    public void setup() {
        ImageResponse emptyRegistryResponse = new ImageResponse();
        emptyRegistryResponse.setProjects(new ArrayList<>());
        when(imageService.getAllImages()).thenReturn(emptyRegistryResponse);
    }

    @Test
    public void testOnboardDockerTarSuccessfully() throws CommandTimedOutException, IOException, URISyntaxException {
        // given
        Path manifest = createDirectoryWithDockerTar();

        final ProcessExecutorResponse extractFileCommandResponse = new ProcessExecutorResponse();
        final ResponseEntity<String> uploadLayerResponse = ResponseEntity.ok().header(HttpHeaders.LOCATION, UPLOAD_LAYER_URL).build();
        doReturn(extractFileCommandResponse).when(processExecutor).executeProcessBuilder(any(String.class), any(Integer.class));
        doReturn(false).when(containerRegistryService).isLayerExists(anyString(), anyString());
        doNothing().when(containerRegistryService).uploadLayer(anyString(), anyString(), any(Path.class), anyString());
        when(restClient.post(anyString(), anyString(), anyString())).thenReturn(uploadLayerResponse);
        final ResponseEntity<String> processManifestResponse = ResponseEntity.created(URI.create("")).build();
        when(restClient.put(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(processManifestResponse);
        doReturn(UUID.randomUUID().toString()).when(fileSystemService).generateHash(any(), any());

        // when
        List<String> onboardedImages = dockerService.onboardDockerTar(manifest, LocalDateTime.now().plusMinutes(5));

        // then
        assertThat(onboardedImages).hasSize(2);
    }

    @Test
    public void testOnboardDockerTarOnUploadLayerNoSpaceError() throws CommandTimedOutException, URISyntaxException, IOException {
        // given
        Path manifest = createDirectoryWithDockerTar();

        final ProcessExecutorResponse extractFileCommandResponse = new ProcessExecutorResponse();
        doReturn(extractFileCommandResponse).when(processExecutor).executeProcessBuilder(any(String.class), any(Integer.class));
        doReturn(false).when(containerRegistryService).isLayerExists(anyString(), anyString());
        doReturn(UUID.randomUUID().toString()).when(fileSystemService).generateHash(any(), any());

        final Map<String, Integer> exceptionBody = Map.of("Err", 28);
        HttpServerErrorException noSpaceLeftOnDeviceException = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR,
                "",
                TestUtils.convertToBytes(exceptionBody),
                Charset.defaultCharset());
        doThrow(noSpaceLeftOnDeviceException).when(restClient).post(anyString(), anyString(), anyString());

        // when/then
        assertThatThrownBy(() -> dockerService.onboardDockerTar(manifest, LocalDateTime.now().plusMinutes(5)))
                .isInstanceOf(DockerServiceException.class)
                .hasMessageContaining("no space left on docker registry");
    }

    private Path createDirectoryWithDockerTar() throws IOException, URISyntaxException {
        Path tempDirectory = Files.createTempDirectory(FOLDER_PREFIX);
        tempDirectory.toFile().deleteOnExit();
        String tempDirectoryPath = tempDirectory.toString();
        final Path linkLayerFolder = Files.createDirectory(Path.of(tempDirectoryPath, LINK_LAYER_FOLDER));
        final Path targetLayerFolder = Files.createDirectory(Path.of(tempDirectoryPath, TARGET_LAYER_FOLDER));
        final Path layerTarFile = Files.createFile(Path.of(tempDirectoryPath, LAYER_TAR));
        Path targetLayer = Files.move(layerTarFile, Paths.get(targetLayerFolder.toString(), LAYER_TAR), StandardCopyOption.REPLACE_EXISTING);
        final Path layerTarSymbolicLink = Paths.get(linkLayerFolder.toString(), LAYER_TAR);
        final Path relativeTarPath = layerTarSymbolicLink.relativize(targetLayer);
        Files.createSymbolicLink(layerTarSymbolicLink, relativeTarPath);
        Files.createFile(Path.of(tempDirectoryPath, CONFIG_JSON_1));
        Files.createFile(Path.of(tempDirectoryPath, CONFIG_JSON_2));
        final Path manifest = Files.createFile(Path.of(tempDirectoryPath, Constants.MANIFEST_JSON));
        Files.copy(createInputStream(CONCURRENT_MANIFEST_PATH), manifest, StandardCopyOption.REPLACE_EXISTING);
        return manifest;
    }

    @Test
    public void testOnboardDockerTarSuccessfullyWithNotNestedLayerDirectory() throws IOException, URISyntaxException {
        // given
        Path tempDirectory = Files.createTempDirectory(FOLDER_PREFIX);
        tempDirectory.toFile().deleteOnExit();
        String tempDirectoryPath = tempDirectory.toString();
        final Path dockerTar = Files.createFile(Path.of(tempDirectoryPath, DOCKER_TAR));
        Files.copy(createInputStream(DOCKER_TAR_PATH), dockerTar, StandardCopyOption.REPLACE_EXISTING);
        final ResponseEntity<String> uploadLayerResponse = ResponseEntity.ok().header(HttpHeaders.LOCATION, UPLOAD_LAYER_URL).build();
        final ResponseEntity<String> processManifestResponse = ResponseEntity.created(URI.create("")).build();

        doReturn(false).when(containerRegistryService).isLayerExists(anyString(), anyString());
        doNothing().when(containerRegistryService).uploadLayer(anyString(), anyString(), any(Path.class), anyString());
        doReturn(UUID.randomUUID().toString()).when(fileSystemService).generateHash(any(), any());
        when(restClient.post(anyString(), anyString(), anyString())).thenReturn(uploadLayerResponse);
        when(restClient.put(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(processManifestResponse);

        // when
        List<String> onboardedImages = dockerService.onboardDockerTar(dockerTar, LocalDateTime.now().plusMinutes(5));

        // then
        assertThat(onboardedImages).hasSize(1);
    }
}
