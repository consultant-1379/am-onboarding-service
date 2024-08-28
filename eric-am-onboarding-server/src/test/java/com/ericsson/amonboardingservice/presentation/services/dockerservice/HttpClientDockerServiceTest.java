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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import static com.ericsson.amonboardingservice.TestUtils.createInputStream;
import static com.ericsson.amonboardingservice.utils.Constants.FAILED_TO_EXECUTE_COMMAND_MESSAGE;
import static com.ericsson.amonboardingservice.utils.Constants.FAILED_TO_EXTRACT_LAYER_FROM_DOCKER_TAR_FILE_MESSAGE;

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

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpServerErrorException;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.TestUtils;
import com.ericsson.amonboardingservice.model.ImageResponse;
import com.ericsson.amonboardingservice.model.ImageResponseImages;
import com.ericsson.amonboardingservice.model.ImageResponseProjects;
import com.ericsson.amonboardingservice.presentation.exceptions.CommandTimedOutException;
import com.ericsson.amonboardingservice.presentation.exceptions.DataNotFoundException;
import com.ericsson.amonboardingservice.presentation.exceptions.DockerServiceException;
import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileSystemService;
import com.ericsson.amonboardingservice.presentation.services.imageservice.ImageService;
import com.ericsson.amonboardingservice.utils.Constants;
import com.ericsson.amonboardingservice.infrastructure.client.RestClient;
import com.ericsson.amonboardingservice.utils.docker.DockerRegistryHelper;
import com.ericsson.amonboardingservice.utils.executor.ProcessExecutor;
import com.ericsson.amonboardingservice.utils.executor.ProcessExecutorResponse;

@SpringBootTest(properties = {"onboarding.upload.concurrency=none", "skopeo.enabled=false"})
@ActiveProfiles("test")
public final class HttpClientDockerServiceTest extends AbstractDbSetupTest {

    private static final String PACKAGE_ID = "1";
    private static final String LAYER_TAR = "layer.tar";
    private static final String CONFIG_JSON = "219ee5171f8006d1462fa76c12b9b01ab672dbc8b283f186841bf2c3ca8e3c93.json";
    private static final String MANIFEST_PATH = "docker/" + Constants.MANIFEST_JSON;
    private static final String LAYER_FOLDER = "5b4ef804af565ff504bf7693239e5f26b4782484f068226b7244117760603d0d";
    private static final String LAYER_PATH = LAYER_FOLDER + "/" + LAYER_TAR;
    private static final String UPLOAD_LAYER_URL = "url";
    private static final int TIMEOUT_VALUE = 5;
    private static final int CMD_NORMAL_EXIT_VALUE = 0;
    private static final int CMD_ERROR_EXIT_VALUE = 1;
    private static final String CMD_OUTPUT = "some cmd output";
    private static final String CMD_ERROR_OUTPUT = "some cmd error output";

    @Autowired
    private DockerService dockerService;

    @MockBean
    private RestClient restClient;

    @MockBean
    private ProcessExecutor processExecutorMock;

    @MockBean
    private DockerRegistryHelper dockerRegistryHelper;

    @MockBean
    private ImageService imageService;

    @SpyBean
    private ContainerRegistryService containerRegistryService;

    @SpyBean
    private FileSystemService fileSystemService;

    @Value("${docker.registry.address}")
    private String testingRegistry;

    @TempDir
    public Path folder;

    @Test
    public void testOnboardDockerTarSuccessfully() throws CommandTimedOutException, IOException, URISyntaxException {
        // given
        Path manifest = prepareMocksForOnboardTar();
        mockEmptyRegistryCatalogResponse();
        // when
        assertThat(dockerService.onboardDockerTar(manifest, LocalDateTime.now().plusMinutes(TIMEOUT_VALUE))).hasSize(1);
        // then
        verify(containerRegistryService, times(1)).uploadLayer(anyString(), anyString(), any(Path.class), anyString());
    }

    @Test
    public void testOnboardDockerTarSuccessfullySkipImageUpload() throws CommandTimedOutException, IOException, URISyntaxException {
        // given
        Path manifest = prepareMocksForOnboardTar();
        mockRegistryCatalogResponse();
        // when
        assertThat(dockerService.onboardDockerTar(manifest, LocalDateTime.now().plusMinutes(TIMEOUT_VALUE))).hasSize(1);
        // then
        verify(containerRegistryService, never()).uploadLayer(anyString(), anyString(), any(Path.class), anyString());
    }

    @Test
    public void testOnboardDockerTarSuccessfullyWithDuplicatedImage() throws CommandTimedOutException, IOException, URISyntaxException {
        // given
        final Path layerFolder = Files.createDirectory(folder.resolve(LAYER_FOLDER));
        final Path layerTarPath = Files.createFile(folder.resolve(LAYER_TAR));
        final Path layerTarSymbolicLink = Paths.get(layerFolder.toString(), LAYER_TAR);
        final Path relativeTarPath = layerTarSymbolicLink.relativize(layerTarPath);
        Files.createSymbolicLink(layerTarSymbolicLink, relativeTarPath);
        Files.createFile(folder.resolve(CONFIG_JSON));
        final Path manifest = Files.createFile(folder.resolve(Constants.MANIFEST_JSON));
        Files.copy(createInputStream(MANIFEST_PATH), manifest, StandardCopyOption.REPLACE_EXISTING);

        final ProcessExecutorResponse response = buildNormalProcessExecutorResponse();

        final ResponseEntity<String> uploadLayerResponse = ResponseEntity.ok().header(HttpHeaders.LOCATION, UPLOAD_LAYER_URL).build();
        when(processExecutorMock.executeProcessBuilder(any(String.class), any(Integer.class))).thenReturn(response);
        doReturn(false, true).when(containerRegistryService).isLayerExists(anyString(), anyString());
        doNothing().when(containerRegistryService).uploadLayer(anyString(), anyString(), any(Path.class), anyString());
        when(restClient.post(anyString(), anyString(), anyString())).thenReturn(uploadLayerResponse);
        final ResponseEntity<String> processManifestResponse = ResponseEntity.created(URI.create("")).build();
        when(restClient.put(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(processManifestResponse);
        doReturn(UUID.randomUUID().toString()).when(fileSystemService).generateHash(any(), any());
        mockRegistryWithImage("dockerhub-ericsson-remote/busybox", "1.32.0");

        // when
        dockerService.onboardDockerTar(manifest, LocalDateTime.now().plusMinutes(TIMEOUT_VALUE));

        // then
        verify(containerRegistryService, times(0)).uploadLayer(anyString(), anyString(), any(Path.class), anyString());
    }

    @Test
    public void testOnboardDockerTarSuccessfullyGetAllImagesThrowsError() throws CommandTimedOutException, IOException, URISyntaxException {
        // given
        final Path layerFolder = Files.createDirectory(folder.resolve(LAYER_FOLDER));
        final Path layerTarPath = Files.createFile(folder.resolve(LAYER_TAR));
        final Path layerTarSymbolicLink = Paths.get(layerFolder.toString(), LAYER_TAR);
        final Path relativeTarPath = layerTarSymbolicLink.relativize(layerTarPath);
        Files.createSymbolicLink(layerTarSymbolicLink, relativeTarPath);
        Files.createFile(folder.resolve(CONFIG_JSON));
        final Path manifest = Files.createFile(folder.resolve(Constants.MANIFEST_JSON));
        Files.copy(createInputStream(MANIFEST_PATH), manifest, StandardCopyOption.REPLACE_EXISTING);

        final ProcessExecutorResponse response = buildNormalProcessExecutorResponse();

        final ResponseEntity<String> uploadLayerResponse = ResponseEntity.ok().header(HttpHeaders.LOCATION, UPLOAD_LAYER_URL).build();
        when(processExecutorMock.executeProcessBuilder(any(String.class), any(Integer.class))).thenReturn(response);
        doReturn(false, true).when(containerRegistryService).isLayerExists(anyString(), anyString());
        doNothing().when(containerRegistryService).uploadLayer(anyString(), anyString(), any(Path.class), anyString());
        when(restClient.post(anyString(), anyString(), anyString())).thenReturn(uploadLayerResponse);
        final ResponseEntity<String> processManifestResponse = ResponseEntity.created(URI.create("")).build();
        when(restClient.put(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(processManifestResponse);
        doReturn(UUID.randomUUID().toString()).when(fileSystemService).generateHash(any(), any());
        when(imageService.getAllImages()).thenThrow(new DataNotFoundException(Constants.NO_DATA_FOUND_ERROR_MESSAGE));

        // when
        dockerService.onboardDockerTar(manifest, LocalDateTime.now().plusMinutes(TIMEOUT_VALUE));

        // then
        verify(containerRegistryService, times(1)).uploadLayer(anyString(), anyString(), any(Path.class), anyString());
    }

    @Test
    public void testOnboardDockerTarOnUploadLayerNoSpaceError() throws CommandTimedOutException, URISyntaxException, IOException {
        // given
        Files.createDirectory(folder.resolve(LAYER_FOLDER));
        Files.createFile(folder.resolve(LAYER_PATH));
        Files.createFile(folder.resolve(CONFIG_JSON));
        final Path manifest = Files.createFile(folder.resolve(Constants.MANIFEST_JSON));
        Files.copy(createInputStream(MANIFEST_PATH), manifest, StandardCopyOption.REPLACE_EXISTING);

        ProcessExecutorResponse extractFileCommandResponse = new ProcessExecutorResponse();
        when(processExecutorMock.executeProcessBuilder(any(String.class), any(Integer.class))).thenReturn(extractFileCommandResponse);
        doReturn(false).when(containerRegistryService).isLayerExists(anyString(), anyString());
        doReturn(UUID.randomUUID().toString()).when(fileSystemService).generateHash(any(), any());
        mockEmptyRegistryCatalogResponse();

        final Map<String, Integer> exceptionBody = Map.of("Err", 28);
        HttpServerErrorException noSpaceLeftOnDeviceException = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR,
                                                                                             "",
                                                                                             TestUtils.convertToBytes(exceptionBody),
                                                                                             Charset.defaultCharset());
        doThrow(noSpaceLeftOnDeviceException).when(restClient).post(anyString(), anyString(), anyString());

        // when/then
        assertThatThrownBy(() -> dockerService.onboardDockerTar(manifest, LocalDateTime.now().plusMinutes(TIMEOUT_VALUE)))
                .isInstanceOf(DockerServiceException.class)
                .hasMessageContaining("no space left on docker registry");
    }

    @Test
    public void getDockerRegistry() {
        assertThat(containerRegistryService.getDockerRegistry()).isEqualTo(testingRegistry);
    }

    @Test
    public void shouldRemoveOnlyImagesOfPackageNotRelatedToOtherPackages() {
        String[] expectedImageToRemove = "docker.io/unique/non-shareable-image:1.0.0".split(":");

        dockerService.removeDockerImagesByPackageId(PACKAGE_ID);

        verify(dockerRegistryHelper, atLeastOnce())
                .deleteManifestsByTag(expectedImageToRemove[0], expectedImageToRemove[1]);
    }

    @Test
    public void testExecuteProcessBuilderFail() throws CommandTimedOutException, IOException {
        Path pathToTar = folder;
        when(processExecutorMock.executeProcessBuilder(any(String.class), any(Integer.class))).thenThrow(CommandTimedOutException.class);
        mockEmptyRegistryCatalogResponse();

        assertThatThrownBy(() -> dockerService.onboardDockerTar(pathToTar, LocalDateTime.now().plusMinutes(TIMEOUT_VALUE)))
                .isInstanceOf(InternalRuntimeException.class)
                .hasMessageContaining(String.format(FAILED_TO_EXTRACT_LAYER_FROM_DOCKER_TAR_FILE_MESSAGE, TIMEOUT_VALUE));
    }

    @Test
    public void whenResponseFromProcessBuilderNotZero_ThenExceptionThrows() throws CommandTimedOutException, IOException {
        //given
        Path pathToTar = folder;
        ProcessExecutorResponse response = buildErrorProcessExecutorResponse();
        //when
        mockEmptyRegistryCatalogResponse();
        when(processExecutorMock.executeProcessBuilder(any(String.class), any(Integer.class))).thenReturn(response);
        //then

        assertThatThrownBy(() -> dockerService.onboardDockerTar(pathToTar, LocalDateTime.now().plusMinutes(TIMEOUT_VALUE)))
                .isInstanceOf(InternalRuntimeException.class)
                .hasMessageContaining(FAILED_TO_EXECUTE_COMMAND_MESSAGE.split(":")[0]);
    }

    /**
     * Preparation of mocks for Onboard Tar tests in HttpDockerClient class
     *
     * @return Path to temporary image manifest file
     */
    private Path prepareMocksForOnboardTar() throws CommandTimedOutException, IOException, URISyntaxException{
        final Path layerFolder = Files.createDirectory(folder.resolve(LAYER_FOLDER));
        final Path layerTarPath = Files.createFile(folder.resolve(LAYER_TAR));
        final Path layerTarSymbolicLink = Paths.get(layerFolder.toString(), LAYER_TAR);
        final Path relativeTarPath = layerTarSymbolicLink.relativize(layerTarPath);
        Files.createSymbolicLink(layerTarSymbolicLink, relativeTarPath);
        Files.createFile(folder.resolve(CONFIG_JSON));
        final Path manifest = Files.createFile(folder.resolve(Constants.MANIFEST_JSON));
        Files.copy(createInputStream(MANIFEST_PATH), manifest, StandardCopyOption.REPLACE_EXISTING);

        final ProcessExecutorResponse response = buildNormalProcessExecutorResponse();

        final ResponseEntity<String> uploadLayerResponse = ResponseEntity.ok().header(HttpHeaders.LOCATION, UPLOAD_LAYER_URL).build();
        when(processExecutorMock.executeProcessBuilder(any(String.class), any(Integer.class))).thenReturn(response);
        doReturn(false, true).when(containerRegistryService).isLayerExists(anyString(), anyString());
        doNothing().when(containerRegistryService).uploadLayer(anyString(), anyString(), any(Path.class), anyString());
        when(restClient.post(anyString(), anyString(), anyString())).thenReturn(uploadLayerResponse);
        final ResponseEntity<String> processManifestResponse = ResponseEntity.created(URI.create("")).build();
        when(restClient.put(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(processManifestResponse);
        doReturn(UUID.randomUUID().toString()).when(fileSystemService).generateHash(any(), any());

        return manifest;
    }

    @NotNull
    private ProcessExecutorResponse buildNormalProcessExecutorResponse() {
        final ProcessExecutorResponse response = new ProcessExecutorResponse();
        response.setExitValue(CMD_NORMAL_EXIT_VALUE);
        response.setCmdResult(CMD_OUTPUT);
        return response;
    }

    @NotNull
    private ProcessExecutorResponse buildErrorProcessExecutorResponse() {
        final ProcessExecutorResponse response = new ProcessExecutorResponse();
        response.setExitValue(CMD_ERROR_EXIT_VALUE);
        response.setCmdResult(CMD_OUTPUT);
        response.setCmdErrorResult(CMD_ERROR_OUTPUT);
        return response;
    }

    private void mockEmptyRegistryCatalogResponse() {
        ImageResponse emptyRegistryResponse = new ImageResponse();
        emptyRegistryResponse.setProjects(new ArrayList<>());
        when(imageService.getAllImages()).thenReturn(emptyRegistryResponse);
    }

    private void mockRegistryCatalogResponse() {
        ImageResponseImages image = new ImageResponseImages();
        image.setName("busybox");
        image.setTags(List.of("1.32.0"));
        image.setRepository("dockerhub-ericsson-remote/busybox");

        ImageResponseProjects projects = new ImageResponseProjects();
        projects.setName("dockerhub-ericsson-remote");
        projects.addImagesItem(image);

        ImageResponse emptyRegistryResponse = new ImageResponse();
        emptyRegistryResponse.addProjectsItem(projects);
        when(imageService.getAllImages()).thenReturn(emptyRegistryResponse);
    }

    private void mockRegistryWithImage(String repo, String tag) {
        ImageResponse imageResponse = new ImageResponse();
        ImageResponseProjects imageResponseProjects = new ImageResponseProjects();
        ImageResponseImages images = new ImageResponseImages();
        images.setRepository(repo);
        images.setTags(List.of(tag));
        imageResponseProjects.setName("dummy name");
        imageResponseProjects.setImages(List.of(images));

        imageResponse.setProjects(List.of(imageResponseProjects));
        when(imageService.getAllImages()).thenReturn(imageResponse);
    }
}
