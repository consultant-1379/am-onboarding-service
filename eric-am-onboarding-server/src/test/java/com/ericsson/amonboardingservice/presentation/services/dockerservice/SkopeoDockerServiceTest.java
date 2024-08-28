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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageDockerImageRepository;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileSystemService;
import com.ericsson.amonboardingservice.utils.Constants;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = { "onboarding.upload.concurrency=none", "skopeo.enabled=true" })
@ActiveProfiles("test")
public class SkopeoDockerServiceTest extends AbstractDbSetupTest {

    private static final Path MANIFEST_PATH = Path.of("docker/" + Constants.MANIFEST_JSON);
    private static final Path IVLALID_IMAGE_TAG_FORMAT_MANIFEST_PATH = Path.of("docker/" + "invalid_image_tag_format_manifest.json");
    private static final int TIMEOUT_VALUE = 5;

    @Autowired
    private DockerService dockerService;
    @MockBean
    private SkopeoService skopeoService;
    @MockBean
    private FileSystemService fileSystemService;
    @MockBean
    private AppPackageDockerImageRepository dockerImageRepository;
    @SpyBean
    private ObjectMapper mapper;

    @Test
    public void testOnboardDockerTarSuccessfully_shouldExecuteSkopeoServicePushMethod() throws IOException {
        // given
        File manifestJsonAbsolutePath = new File("src/test/resources/" + MANIFEST_PATH).getAbsoluteFile();
        String manifestJsonContent = Files.readAllLines(Path.of(manifestJsonAbsolutePath.toString())).get(0);
        Path manifestPath = mock(Path.class);
        File mockFile = mock(File.class);

        when(fileSystemService.extractFileFromTar(any(Path.class), any(Path.class), anyString(), anyInt())).thenReturn(manifestPath);
        when(fileSystemService.readFile(eq(manifestPath))).thenReturn(manifestJsonContent);
        when(manifestPath.toFile()).thenReturn(mockFile);
        when(mockFile.exists()).thenReturn(true);

        // when
        List<String> onboardedImages = dockerService.onboardDockerTar(MANIFEST_PATH, LocalDateTime.now().plusMinutes(TIMEOUT_VALUE));

        assertThat(onboardedImages.size() == 1);
        // then
        verify(skopeoService, times(1)).pushImageFromDockerTar(anyString(), anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    public void testOnboardDockerTar_shouldThrowIllegalArgumentException_whenManifestFileNotExists()
    throws IOException {
        // given


        File manifestJsonAbsolutePath = new File("src/test/resources/" + MANIFEST_PATH).getAbsoluteFile();
        String manifestJsonContent = Files.readAllLines(Path.of(manifestJsonAbsolutePath.toString())).get(0);

        File mockFile = mock(File.class);
        Path manifestPath = mock(Path.class);



        when(fileSystemService.extractFileFromTar(any(Path.class), any(Path.class), anyString(), anyInt())).thenReturn(manifestPath);
        when(fileSystemService.readFile(eq(manifestPath))).thenReturn(manifestJsonContent);
        when(manifestPath.toFile()).thenReturn(mockFile);
        when(mockFile.exists()).thenReturn(false);

        // when
        assertThrows(IllegalArgumentException.class, () -> dockerService.onboardDockerTar(MANIFEST_PATH, LocalDateTime.now().plusMinutes(TIMEOUT_VALUE)));

//        // then
        verify(skopeoService, times(0)).pushImageFromDockerTar(anyString(), anyString(), anyString(), anyString(), anyInt());
    }
    @Test
    public void testOnboardDockerTar_shouldThrowIllegalArgumentException_whenImageRepoPathIsInInvalidFormat()  throws IOException {
        // given

        File manifestJsonAbsolutePath = new File("src/test/resources/" + IVLALID_IMAGE_TAG_FORMAT_MANIFEST_PATH).getAbsoluteFile();
        String manifestJsonContent = Files.readAllLines(Path.of(manifestJsonAbsolutePath.toString())).get(0);

        File mockFile = mock(File.class);
        Path manifestPath = mock(Path.class);



        when(fileSystemService.extractFileFromTar(any(Path.class), any(Path.class), anyString(), anyInt())).thenReturn(manifestPath);
        when(fileSystemService.readFile(eq(manifestPath))).thenReturn(manifestJsonContent);
        when(manifestPath.toFile()).thenReturn(mockFile);
        when(mockFile.exists()).thenReturn(true);

        // when
        assertThrows(IllegalArgumentException.class, () -> dockerService.onboardDockerTar(IVLALID_IMAGE_TAG_FORMAT_MANIFEST_PATH, LocalDateTime.now().plusMinutes(TIMEOUT_VALUE)));

        //        // then
        verify(skopeoService, times(0)).pushImageFromDockerTar(anyString(), anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    public void removeDockerImagesByPackageId_shouldExecuteSkopeoServiceRemoveMethod() {
        String packageId = "sdfsdsd322mrf2d2";
        String imageName = "busybox";
        String imageTag = "1.0.0";
        String fullImageName = String.format("%s:%s", imageName, imageTag);
        when(dockerImageRepository.findAllRemovableImagesByPackageId(eq(packageId))).thenReturn(new ArrayList<>(Arrays.asList(fullImageName)));

        dockerService.removeDockerImagesByPackageId(packageId);

        verify(skopeoService, times(1)).deleteImageFromRegistry(eq(imageName), eq(imageTag));
    }
}
