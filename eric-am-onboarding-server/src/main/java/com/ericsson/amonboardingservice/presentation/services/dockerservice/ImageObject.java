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

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImageObject {

    @NotNull(message = "CONFIG_FILE_LOCATION_REQUIRED")
    @Size(min = 1, message = "CONFIG_FILE_LOCATION_EMPTY")
    @JsonProperty("Config")
    private String config;

    @NotNull(message = "REPOTAGS_REQUIRED")
    @Size(min = 1, message = "REPOTAGS_EMPTY")
    @JsonProperty("RepoTags")
    private List<String> repoTags;

    @NotNull(message = "LAYERS_REQUIRED")
    @Size(min = 1, message = "LAYERS_EMPTY")
    @JsonProperty("Layers")
    private List<String> layers;
}
