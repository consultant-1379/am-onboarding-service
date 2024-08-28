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
package com.ericsson.amonboardingservice.utils.docker;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.infrastructure.client.RestClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DockerRegistryRestApiV2Helper implements DockerRegistryHelper {
    private static final String MANIFEST_API = "https://%s/v2/%s/manifests/%s";

    private static final List<String> MANIFEST_CONTENT_TYPE_HEADER =
            Collections.singletonList("application/vnd.docker.distribution.manifest.v2+json");
    private static final String DOCKER_CONTENT_DIGEST_HEADER = "Docker-Content-Digest";
    private static final String UNKNOWN_ERROR_MESSAGE = "Unknown error";

    @Autowired
    private RestClient restClient;

    @Value("${docker.registry.address}")
    private String privateDockerRegistry;

    @Value("${docker.registry.user.name}")
    private String registryUser;

    @Value("${docker.registry.user.password}")
    private String registryPassword;

    public List<String> getManifestDigestsByTag(String repository, String imageTag) {
        String url = String.format(MANIFEST_API, privateDockerRegistry, repository, imageTag);
        ResponseEntity<String> manifestResponse =
                restClient.exchange(url, GET, getManifestContentTypeHeaders(), registryUser, registryPassword);
        if (manifestResponse.getStatusCode() == HttpStatus.OK) {
            return manifestResponse.getHeaders().get(DOCKER_CONTENT_DIGEST_HEADER);
        } else if (manifestResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
            LOGGER.debug("No manifest found for tag specified: {}", imageTag);
            return Collections.emptyList();
        } else {
            throw new InternalRuntimeException("Unable to get manifest digest list: " + getErrorDetails(manifestResponse));
        }
    }

    public void deleteManifestsByTag(String repository, String imageTag) {
        LOGGER.info("Removing image with tag {} from repository {}", imageTag, repository);
        getManifestDigestsByTag(repository, imageTag)
                .forEach(digest -> deleteManifestByDigest(repository, digest));
    }

    public void deleteManifestByDigest(String repository, String digest) {
        String url = String.format(MANIFEST_API, privateDockerRegistry, repository, digest);
        ResponseEntity<String> manifestResponse =
                restClient.exchange(url, DELETE, getManifestContentTypeHeaders(), registryUser, registryPassword);

        if (manifestResponse.getStatusCode() == HttpStatus.ACCEPTED) {
            LOGGER.info("Manifest {} has been removed from repository {}", digest, repository);
        } else if (manifestResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
            LOGGER.info("Manifest {} in repository {} had already been deleted or did not exist", digest, repository);
        } else {
            throw new InternalRuntimeException("Unable to delete manifest: " + getErrorDetails(manifestResponse));
        }
    }

    private static String getErrorDetails(ResponseEntity<String> errorResponse) {
        String errorMessage = UNKNOWN_ERROR_MESSAGE;
        try {
            JsonNode responseBodyJson = new ObjectMapper().readTree(errorResponse.getBody());
            if (responseBodyJson.hasNonNull("errors")) {
                JsonNode errorDetails = responseBodyJson.get("errors").get(0);
                if (responseBodyJson.hasNonNull("message")) {
                    errorMessage = errorDetails.get("message").asText(UNKNOWN_ERROR_MESSAGE);
                }
            }
        } catch (JsonProcessingException exception) {
            LOGGER.debug("Registry API response with status {} received, but can not be parsed: {}",
                         errorResponse.getStatusCode(), exception);
        }
        return errorResponse.getStatusCode() + ": " + errorMessage;
    }

    private static HttpHeaders getManifestContentTypeHeaders() {
        HttpHeaders manifestHeaders = new HttpHeaders();
        manifestHeaders.put(HttpHeaders.ACCEPT, MANIFEST_CONTENT_TYPE_HEADER);
        return manifestHeaders;
    }
}
