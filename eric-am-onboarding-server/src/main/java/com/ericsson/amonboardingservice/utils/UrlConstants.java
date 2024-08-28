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
package com.ericsson.amonboardingservice.utils;

public final class UrlConstants {

    public static final String UPLOAD_URL = "https://%s/v2/%s/blobs/uploads/";
    public static final String LAYERS_IN_PRIVATE_DOCKER_REGISTRY_URL = "https://%s/v2/%s/blobs/sha256:%s";
    public static final String MANIFEST_IN_PRIVATE_DOCKER_REGISTRY_URL = "https://%s/v2/%s/manifests/%s";

    private UrlConstants() {}
}
