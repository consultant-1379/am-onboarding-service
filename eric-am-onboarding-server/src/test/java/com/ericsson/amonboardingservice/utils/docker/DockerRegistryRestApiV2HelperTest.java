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

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.TestUtils;
import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.infrastructure.client.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;

@SpringBootTest
@ActiveProfiles("test")
public class DockerRegistryRestApiV2HelperTest extends AbstractDbSetupTest {

    private static final String VALID_REPOSITORY = "image-repository";
    private static final String VALID_TAG = "1.0.0";
    private static final String VALID_DIGEST = "sha256:g00d-d1ge27";
    private static final String NON_EXISTENT_TAG = "bad-tag";
    private static final String NON_EXISTENT_DIGEST = "sha256:bad-d1ge27";
    private static final String INVALID_DIGEST = "ugly-digest";
    private static final String INVALID_TAG = "invalid tag";
    private static final String DOCKER_REGISTRY_MANIFEST_API = "https://%s/v2/%s/manifests/%s";

    @Autowired
    private DockerRegistryRestApiV2Helper dockerRegistryHelper;

    @MockBean
    private RestClient restClient;

    @Value("${docker.registry.address}")
    private String testingRegistry;

    @BeforeEach
    public void setUp() throws IOException {
        HttpHeaders validManifestResponseHeaders = new HttpHeaders();
        validManifestResponseHeaders.add("Docker-Content-Digest", VALID_DIGEST);

        String getExistentManifestRequest = String.format(DOCKER_REGISTRY_MANIFEST_API, testingRegistry, VALID_REPOSITORY, VALID_TAG);
        when(restClient.exchange(eq(getExistentManifestRequest), eq(GET), any(HttpHeaders.class), anyString(), anyString()))
                .thenReturn(new ResponseEntity<>("{}", validManifestResponseHeaders, HttpStatus.OK));

        String getNonExistentManifestResponseBody =
                TestUtils.readDataFromFile("docker/get_non_existent_manifest_response.json", StandardCharsets.UTF_8);
        String getNonExistentManifestRequest = String.format(DOCKER_REGISTRY_MANIFEST_API, testingRegistry, VALID_REPOSITORY, NON_EXISTENT_TAG);
        when(restClient.exchange(eq(getNonExistentManifestRequest), eq(GET), any(HttpHeaders.class), anyString(), anyString()))
                .thenReturn(new ResponseEntity<>(getNonExistentManifestResponseBody, HttpStatus.NOT_FOUND));

        String getByInvalidTagRequest = String.format(DOCKER_REGISTRY_MANIFEST_API, testingRegistry, VALID_REPOSITORY, INVALID_TAG);
        when(restClient.exchange(eq(getByInvalidTagRequest), eq(GET), any(HttpHeaders.class), anyString(), anyString()))
                .thenReturn(new ResponseEntity<>("{}", HttpStatus.BAD_REQUEST));

        String deleteNonExistentManifestRequest = String.format(DOCKER_REGISTRY_MANIFEST_API, testingRegistry, VALID_REPOSITORY, NON_EXISTENT_DIGEST);
        when(restClient.exchange(eq(deleteNonExistentManifestRequest), eq(DELETE), any(HttpHeaders.class), anyString(), anyString()))
                .thenReturn(new ResponseEntity<>("{}", HttpStatus.NOT_FOUND));

        String deleteManifestByDigestRequest = String.format(DOCKER_REGISTRY_MANIFEST_API, testingRegistry, VALID_REPOSITORY, VALID_DIGEST);
        when(restClient.exchange(eq(deleteManifestByDigestRequest), eq(DELETE), any(HttpHeaders.class), anyString(), anyString()))
                .thenReturn(new ResponseEntity<>("{}", HttpStatus.ACCEPTED));

        String deleteManifestByInvalidDigestResponseBody =
                TestUtils.readDataFromFile("docker/delete_manifest_invalid_digest_response.json", StandardCharsets.UTF_8);
        String deleteManifestByInvalidDigestRequest = String.format(DOCKER_REGISTRY_MANIFEST_API, testingRegistry, VALID_REPOSITORY, INVALID_DIGEST);
        when(restClient.exchange(eq(deleteManifestByInvalidDigestRequest), eq(DELETE), any(HttpHeaders.class), anyString(), anyString()))
                .thenReturn(new ResponseEntity<>(deleteManifestByInvalidDigestResponseBody, HttpStatus.BAD_REQUEST));
    }

    @Test
    public void shouldReturnListOfManifestDigestsByValidRepoAndTag() {
        List<String> manifestDigests = dockerRegistryHelper.getManifestDigestsByTag(VALID_REPOSITORY, VALID_TAG);
        assertEquals(1, manifestDigests.size());
        assertEquals(VALID_DIGEST, manifestDigests.get(0));
    }

    @Test
    public void shouldReturnEmptyListIfTagNotExists() {
        List<String> actualResult = dockerRegistryHelper.getManifestDigestsByTag(VALID_REPOSITORY, NON_EXISTENT_TAG);
        assertTrue(actualResult.isEmpty());
    }

    @Test
    public void shouldFailWithExceptionIfNot200or404Response() {
        assertThrows(InternalRuntimeException.class, () -> dockerRegistryHelper.getManifestDigestsByTag(VALID_REPOSITORY, INVALID_TAG));
    }

    @Test
    public void shouldRemoveManifestFromRegistryByValidRepoAndTag() {
        dockerRegistryHelper.deleteManifestsByTag(VALID_REPOSITORY, VALID_TAG);
        verifyDeleteManifestByDigestCalled();
    }

    @Test
    public void shouldRemoveManifestFromRegistryByValidDigest() {
        dockerRegistryHelper.deleteManifestByDigest(VALID_REPOSITORY, VALID_DIGEST);
        verifyDeleteManifestByDigestCalled();
    }

    @Test
    public void shouldSkipManifestDeletionIfManifestNotExist() {
        try {
            dockerRegistryHelper.deleteManifestByDigest(VALID_REPOSITORY, NON_EXISTENT_DIGEST);
        } catch (Exception exception) {
            fail("Delete manifest by non-existent digest shouldn't throw exceptions, but exception was caught: " + exception);
        }
    }

    @Test
    public void shouldThrowExceptionOnBadRequestError() {
        assertThrows(InternalRuntimeException.class, () -> dockerRegistryHelper.deleteManifestByDigest(VALID_REPOSITORY, INVALID_DIGEST));
    }

    private void verifyDeleteManifestByDigestCalled() {
        String expectedRequest = String.format(DOCKER_REGISTRY_MANIFEST_API, testingRegistry, VALID_REPOSITORY, VALID_DIGEST);
        verify(restClient, times(1))
                .exchange(eq(expectedRequest), eq(DELETE), any(HttpHeaders.class), anyString(), anyString());
    }
}
