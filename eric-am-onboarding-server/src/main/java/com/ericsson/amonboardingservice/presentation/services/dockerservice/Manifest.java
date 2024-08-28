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

import java.util.Collection;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
@Setter
public class Manifest {

    @Setter(value = AccessLevel.NONE)
    private int schemaVersion = 2;
    @Setter(value = AccessLevel.NONE)
    private String mediaType = "application/vnd.docker.distribution.manifest.v2+json";

    private final LayerObject config;

    private final Collection<LayerObject> layers;
}
