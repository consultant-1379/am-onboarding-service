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


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.presentation.exceptions.DockerServiceException;
import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.presentation.exceptions.PushLayerException;
import com.ericsson.amonboardingservice.presentation.services.dockerservice.ContainerRegistryServiceImpl;
import com.ericsson.amonboardingservice.presentation.services.dockerservice.LayerObject;
import com.ericsson.amonboardingservice.infrastructure.client.RestClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.web.client.HttpServerErrorException;

@SpringBootTest()
@ActiveProfiles("test")
public class ContainerRegistryServiceTest extends AbstractDbSetupTest {

    private static final String UPLOAD_URL = "https://docker-registry.zname.claster.rnd.gic.ericsson.se/v2/pathToRepo/blobs/uploads/digest";
    private static final String INVALID_URL = "invalidUrl";
    private static final String DIGEST = "sha256:f5600c6330da7bb112776ba067a32a9c20842d6ecc8ee3289f1a713b644092f8";
    private static final String SOME_DATA = "some data";
    private static final String IMAGE_REPO = "armdocker.rnd.ericsson.se/dockerhub-ericsson-remote/busybox";
    private static final String IMAGE_TAG = "1.323.0-1";
    private static final String LAYER = "5b4ef804af565ff504bf7693239e5f26b4782484f068226b7244117760603d0d";

    @Autowired
    private ContainerRegistryServiceImpl containerRegistryService;

    @MockBean
    private RestClient restClient;

    @SpyBean
    private ObjectMapper objectMapper;

    @Test
    public void testPushLayerSuccessfully() throws Exception {
        // given
        File layer = File.createTempFile("layer", "tar");
        Path layerPath = layer.toPath();
        Files.writeString(layerPath, SOME_DATA);
        given(restClient.pushLayerToContainerRegistry(any(), anyString(), anyString())).willReturn(ResponseEntity.ok().build());
        given(restClient.post(any(), any(), any())).willReturn(ResponseEntity.ok().header(HttpHeaders.LOCATION, UPLOAD_URL).build());
        // when
        containerRegistryService.uploadLayer(IMAGE_REPO, LAYER, layerPath, DIGEST);
        // then
        verify(restClient, times(1)).pushLayerToContainerRegistry(any(), anyString(), anyString());
    }

    @Test
    public void testPushLayerToContainerRegistryFailed() throws Exception {
        // given
        File layer = File.createTempFile("layer", "tar");
        Path layerPath = layer.toPath();
        Files.writeString(layerPath, SOME_DATA);
        given(restClient.post(any(), any(), any())).willReturn(ResponseEntity.ok().header(HttpHeaders.LOCATION, UPLOAD_URL).build());
        given(restClient.pushLayerToContainerRegistry(any(), anyString(), anyString())).willThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        // when/then
        assertThatThrownBy(() -> containerRegistryService.uploadLayer(IMAGE_REPO, LAYER, layerPath, DIGEST))
                .isInstanceOf(DockerServiceException.class);
    }

    @Test
    public void testPushLayerToContainerRegistryFailedWithNoSpaceLeftOnDeviceException() throws Exception {
        // given
        File layer = File.createTempFile("layer", "tar");
        Path layerPath = layer.toPath();
        Files.writeString(layerPath, SOME_DATA);
        given(restClient.post(any(), any(), any())).willReturn(ResponseEntity.ok().header(HttpHeaders.LOCATION, UPLOAD_URL).build());
        given(restClient.pushLayerToContainerRegistry(any(), anyString(), anyString())).willThrow(new InternalRuntimeException("no space left on device"));
        // when/then
        assertThatThrownBy(() -> containerRegistryService.uploadLayer(IMAGE_REPO, LAYER, layerPath, DIGEST))
                .isInstanceOf(DockerServiceException.class)
                .hasMessageContaining("no space left on docker registry");
    }

    @Test
    public void testWrongPathForLayer() throws PushLayerException {
        // given
        Path layerPath = Path.of("path/to/fileNotExist");
        given(restClient.pushLayerToContainerRegistry(any(), anyString(), anyString())).willReturn(ResponseEntity.ok().build());
        given(restClient.post(any(), any(), any())).willReturn(ResponseEntity.ok().header(HttpHeaders.LOCATION, UPLOAD_URL).build());

        // when/then
        assertThatThrownBy(() -> containerRegistryService.uploadLayer(IMAGE_REPO, LAYER, layerPath, DIGEST))
                .isInstanceOf(DockerServiceException.class);
    }

    @Test
    public void testWrongUploadUrl() throws Exception {
        // given
        File layer = File.createTempFile("layer", "tar");
        Path layerPath = layer.toPath();
        Files.writeString(layerPath, SOME_DATA);
        given(restClient.pushLayerToContainerRegistry(any(), anyString(), anyString())).willReturn(ResponseEntity.ok().build());
        given(restClient.post(any(), any(), any())).willReturn(ResponseEntity.ok().header(HttpHeaders.LOCATION, INVALID_URL).build());

        // when/then
        assertThatThrownBy(() -> containerRegistryService.uploadLayer(IMAGE_REPO, LAYER, layerPath, DIGEST))
                .isInstanceOf(DockerServiceException.class);
    }

    @Test
    public void testLayerExists() {
        // given
        given(restClient.head(any(), any(), any())).willReturn(HttpStatus.OK);

        // when/then
        assertThat(containerRegistryService.isLayerExists(IMAGE_REPO, DIGEST)).isTrue();
    }

    @Test
    public void testProcessManifestSuccessfully() {
        // given
        given(restClient.put(any(), any(), any(), any(), any())).willReturn(ResponseEntity.status(HttpStatus.CREATED).build());

        // when
        containerRegistryService.processManifest(Collections.emptyList(), IMAGE_REPO, IMAGE_TAG, new LayerObject(0, DIGEST));

        // then
        verify(restClient, times(1)).put(any(), any(), any(), any(), any());
    }

    @Test
    public void testProcessManifestFailedWhenIOException() {
        // given
        given(objectMapper.writerWithDefaultPrettyPrinter()).willAnswer( invocation -> { throw new IOException("exception"); });

        // when/then
        assertThatThrownBy(() -> containerRegistryService.processManifest(Collections.emptyList(), IMAGE_REPO, IMAGE_TAG, new LayerObject(0, DIGEST)))
                .isInstanceOf(InternalRuntimeException.class)
                .hasMessageContaining("Failed to generate manifest for");
    }

    @Test
    public void testProcessManifestFailedWhenNotCREATEDResponseStatus() throws Exception {
        // given
        given(restClient.put(any(), any(), any(), any(), any())).willReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());

        // when/then
        assertThatThrownBy(() -> containerRegistryService.processManifest(Collections.emptyList(), IMAGE_REPO, IMAGE_TAG, new LayerObject(0, DIGEST)))
                .isInstanceOf(InternalRuntimeException.class)
                .hasMessageContaining("Failed to upload manifest with Http Status");
    }
}
