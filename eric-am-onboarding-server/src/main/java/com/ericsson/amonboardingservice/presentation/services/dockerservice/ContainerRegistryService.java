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
import java.util.Collection;


public interface ContainerRegistryService {

    /**
     * Returns Docker Registry in use
     */
    String getDockerRegistry();

    boolean isLayerExists(String repo, String digest);

    void processManifest(Collection<LayerObject> layers, String repo, String tag, LayerObject config);

    void uploadLayer(String repo, String layer, Path layerPath, String layerDigest);

}
