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

import java.util.List;

/**
 * Interface that defines methods to interact with Docker Registry
 */

public interface DockerRegistryHelper {

    /**
     * Retrieves digests of image manifests from Docker registry
     * identified by repository name and tag
     *
     * @param repository repository name in Docker registry in use
     * @param imageTag image tag to identify manifests
     *
     * @return list of manifest digests with algorithm prefixes
     */
    List<String> getManifestDigestsByTag(String repository, String imageTag);

    /**
     * Removes manifest files of image from Docker Registry
     * identified by repository name and tag
     *
     * @param repository repository name in Docker registry in use
     * @param imageTag image tag to identify manifests
     */
    void deleteManifestsByTag(String repository, String imageTag);

    /**
     * Removes manifest file of image from Docker Registry
     * identified by repository name and full digest
     *
     * @param repository repository name in Docker registry in use
     * @param digest manifest digest including algorithm prefix
     */
    void deleteManifestByDigest(String repository, String digest);
}
