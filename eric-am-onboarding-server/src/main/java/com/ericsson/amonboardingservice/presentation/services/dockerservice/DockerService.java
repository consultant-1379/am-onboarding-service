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

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public interface DockerService {

    /**
     * Upload Docker Images within a tar file to Docker Registry
     *
     * @param pathToTar
     *         The path to where the docker tar file has been saved.
     * @param onboardingTimeoutDate
     *         Timeout in minutes for onboarding docker tar
     * @return A list of docker images that have been retagged and pushed to the docker registry.
     */
    List<String> onboardDockerTar(Path pathToTar, LocalDateTime onboardingTimeoutDate);

    /**
     * Remove all images from Docker registry referenced by package with given id
     * and not referenced by any of other packages.
     *
     * @param packageId id of package that identifies images to remove
     */
    void removeDockerImagesByPackageId(String packageId);
}
