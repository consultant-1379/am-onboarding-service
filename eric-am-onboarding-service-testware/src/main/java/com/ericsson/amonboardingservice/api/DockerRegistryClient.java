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
package com.ericsson.amonboardingservice.api;

import com.ericsson.amonboardingservice.model.DockerImage;
import com.ericsson.amonboardingservice.utilities.KubernetesCLIUtils;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import static com.ericsson.amonboardingservice.utilities.KubernetesCLIUtils.getUserSecret;
import static com.ericsson.amonboardingservice.utilities.RestUtils.getAuthenticationHeader;
import static com.ericsson.amonboardingservice.utilities.RestUtils.httpGetCall;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class DockerRegistryClient {
    private static final String PATH_TEMPLATE = "/v2/%s/manifests/%s";
    public static final String DOCKER_REGISTRY_HOST = KubernetesCLIUtils.getDockerRegistryHost();
    public static final String DOCKER_REGISTRY_AUTH = getAuthenticationHeader(getUserSecret());

    private DockerRegistryClient() {
    }

    public static ResponseEntity<String> getManifest(DockerImage image) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", DOCKER_REGISTRY_AUTH);
        LOGGER.info("Request to docker registry");
        final String uri = DOCKER_REGISTRY_HOST + String.format(PATH_TEMPLATE, image.getName(), image.getTag());
        return httpGetCall(uri, httpHeaders, String.class);
    }
}