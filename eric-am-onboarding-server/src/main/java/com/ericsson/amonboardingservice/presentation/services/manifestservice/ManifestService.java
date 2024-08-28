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
package com.ericsson.amonboardingservice.presentation.services.manifestservice;

import java.nio.file.Path;
import java.util.Map;

/**
 * This service provides functionality to retrieve and act on information in the Manifest file
 */
public interface ManifestService {

    /**
     * Validate the digests of the artifacts against the digests in the Manifest file.
     * @param unpackedDirectory the directory containing the VNF Package artifacts
     * @param artifactPaths A map of Paths to all the artifacts in the VNF Package
     */
    void validateDigests(Path unpackedDirectory, Map<String, Path> artifactPaths);

    /**
     * Compare the digests of two artifacts to see if they have the same content.
     * @param newArtifact
     * @param existingArtifact
     * @return
     */
    boolean compareArtifacts(Path newArtifact, Path existingArtifact);
}
