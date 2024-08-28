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

import com.ericsson.amonboardingservice.presentation.exceptions.DockerServiceException;
import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.presentation.exceptions.PushLayerException;
import com.ericsson.amonboardingservice.presentation.models.containerregistry.ImageLayerContent;
import com.ericsson.amonboardingservice.utils.Constants;
import com.ericsson.amonboardingservice.infrastructure.client.RestClient;
import com.ericsson.amonboardingservice.utils.UrlConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ericsson.amonboardingservice.utils.UrlConstants.MANIFEST_IN_PRIVATE_DOCKER_REGISTRY_URL;
import static com.ericsson.amonboardingservice.utils.UrlConstants.UPLOAD_URL;

@Slf4j
@Service
public class ContainerRegistryServiceImpl implements ContainerRegistryService {

    public static final int CAPACITY = 20971520;

    private static final Pattern REGISTRY_NO_SPACE_LEFT_ERROR_PATTERN = Pattern.compile("\"Err\":28");

    @Value("${docker.registry.address}")
    private String privateDockerRegistry;

    @Value("${docker.registry.user.name}")
    private String registryUser;

    @Value("${docker.registry.user.password}")
    private String registryPassword;

    @Autowired
    private RestClient restClient;

    @Autowired
    private ObjectMapper mapper;

    @Override
    public String getDockerRegistry() {
        return privateDockerRegistry;
    }

    @Override
    public boolean isLayerExists(String repo, String digest) {
        String url = String.format(UrlConstants.LAYERS_IN_PRIVATE_DOCKER_REGISTRY_URL, privateDockerRegistry, repo, digest);
        LOGGER.info("Checking if URL exists: {}", url);
        return restClient.head(url, registryUser, registryPassword).equals(HttpStatus.OK);
    }

    @Override
    public void processManifest(Collection<LayerObject> layers, String repo, String tag, LayerObject config) {
        LOGGER.info("Generating and pushing manifest for {}:{}", repo, tag);
        String manifestJson;
        try {
            manifestJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(new Manifest(config, layers));
        } catch (IOException e) {
            throw new InternalRuntimeException(String.format("Failed to generate manifest for %s:%s due to: %s", repo, tag, e.getMessage()), e);
        }
        String url = String.format(MANIFEST_IN_PRIVATE_DOCKER_REGISTRY_URL, privateDockerRegistry, repo, tag);
        HttpStatusCode httpStatus = restClient.put(url, manifestJson, registryUser, registryPassword, Constants.DOCKER_LAYER_CONTENT_TYPE)
                .getStatusCode();
        if (HttpStatus.CREATED.value() != httpStatus.value()) {
            throw new InternalRuntimeException("Failed to upload manifest with Http Status " + httpStatus.value());
        }
    }

    @Override
    public void uploadLayer(String repo, String layer, Path layerPath, final String layerDigest) {
        LOGGER.info("Uploading {} to docker registry {}", layer, privateDockerRegistry);
        String uploadUrl = String.format(UPLOAD_URL, privateDockerRegistry, repo);
        LOGGER.info("Start upload url: {}", uploadUrl);

        try {
            ResponseEntity<String> responseEntity = restClient.post(uploadUrl, registryUser, registryPassword);
            uploadUrl = Objects.requireNonNull(responseEntity.getHeaders().getLocation()).toString();
            LOGGER.info("Upload url: {}", uploadUrl);

            this.pushLayer(layerPath, uploadUrl, layerDigest);
        } catch (Exception e) {
            String details = isNoSpaceLeftOnDeviceException(e) ? "no space left on docker registry" : e.getMessage();
            String errorMessage = String.format("%s due to %s",
                    String.format(Constants.DOCKER_REGISTRY_PUSH_FAIL_MESSAGE, layer, layerPath.toFile().length(), repo),
                    details);

            throw new DockerServiceException(errorMessage);
        }
    }

    private void pushLayer(Path layerPath, String uploadUrl, final String layerDigest) throws PushLayerException {
        File targetFile = layerPath.toFile();
        ImageLayerContent layerContent = new ImageLayerContent(targetFile);
        try (RandomAccessFile reader = new RandomAccessFile(layerContent.getImageContent(), "r");
                FileChannel fis = reader.getChannel()) {
            URI uploadUri = new URI(uploadUrl);
            ByteBuffer buf = ByteBuffer.allocate(CAPACITY);
            int i;
            long start = 0L;
            long offset;

            do {
                i = fis.read(buf);
                offset = start + i;
                long fileSize = Files.size(layerPath);
                LOGGER.info("file size {} pushed payload {}", fileSize, offset);

                if (offset == fileSize) {
                    byte[] lastChunk = Arrays.copyOf(buf.array(), i);
                    final URI buildUrl = new URI(uploadUri + "&digest=sha256:" + layerDigest);
                    RequestEntity<byte[]> request = buildHeaders(RequestEntity.put(buildUrl), lastChunk, start, offset);
                    restClient.pushLayerToContainerRegistry(request, registryUser, registryPassword);
                    break;
                } else {
                    RequestEntity<byte[]> request = buildHeaders(RequestEntity.patch(Objects.requireNonNull(uploadUri)), buf.array(), start, offset);
                    ResponseEntity<Void> response = restClient.pushLayerToContainerRegistry(request, registryUser, registryPassword);
                    uploadUri = response.getHeaders().getLocation();
                }

                start = offset;
                buf.clear();
            } while (i != -1);
        } catch (IOException e) {
            final String errMsg = String.format("There is no any file for path %s", layerPath);
            LOGGER.error(errMsg);
            throw new PushLayerException(errMsg, e);
        } catch (URISyntaxException e) {
            final String errMsg = String.format("Exception. uploadUrl %s is wrong", uploadUrl);
            LOGGER.error(errMsg);
            throw new PushLayerException(errMsg, e);
        }
    }

    private static RequestEntity<byte[]> buildHeaders(final RequestEntity.BodyBuilder bodyBuilder, byte[] content,
                                                      final long startAt, final long offset) {
        return bodyBuilder.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(content.length))
                .header(HttpHeaders.CONTENT_RANGE, HttpRange.toString(Collections.singletonList(HttpRange.createByteRange(startAt, offset))))
                .body(content);
    }



    private static <E extends Exception> boolean isNoSpaceLeftOnDeviceException(E e) {
        if (e instanceof HttpServerErrorException) {
            HttpServerErrorException exception = (HttpServerErrorException) e;
            String body = exception.getResponseBodyAsString();
            Matcher matcher = REGISTRY_NO_SPACE_LEFT_ERROR_PATTERN.matcher(body);
            return exception.getStatusCode().is5xxServerError() && matcher.find();
        }
        if (e instanceof InternalRuntimeException) {
            return e.getMessage().contains("no space left on device");
        }
        return false;
    }
}
