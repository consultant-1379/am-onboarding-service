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
package com.ericsson.amonboardingservice.presentation.services.onboarding.request;

import static java.lang.String.format;

import static com.ericsson.amonboardingservice.presentation.services.onboarding.request.OnboardingPhase.TOSCA_VERSION_IDENTIFICATION_PHASE;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_DEFINITIONS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

import com.ericsson.amonboardingservice.utils.Constants;
import org.springframework.stereotype.Component;

import com.ericsson.amonboardingservice.presentation.exceptions.FailedOnboardingValidationException;
import com.ericsson.amonboardingservice.presentation.models.ToscaDefinitionsVersion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ToscaVersionIdentification implements RequestHandler {

    @Override
    public void handle(final PackageUploadRequestContext context) {
        LOGGER.info("Starting ToscaVersionIdentification onboarding handler");

        final String toscaVersionName = deriveDefinitionsVersion(context.getArtifactPaths());

        LOGGER.info("Onboarding package TOSCA definitions version identified: {}", toscaVersionName);

        final ToscaDefinitionsVersion toscaVersion = getToscaDefinitionsVersionByName(toscaVersionName);
        context.setToscaVersion(toscaVersion);

        LOGGER.info("Successfully finished ToscaVersionIdentification onboarding handler");
    }

    @Override
    public String getName() {
        return TOSCA_VERSION_IDENTIFICATION_PHASE.toString();
    }

    private ToscaDefinitionsVersion getToscaDefinitionsVersionByName(final String toscaVersionName) {
        LOGGER.info("Started getting TOSCA Definitions version by name: {}", toscaVersionName);
        ToscaDefinitionsVersion toscaDefinitionsVersion = Arrays.stream(ToscaDefinitionsVersion.values())
                .filter(version -> version.getToscaVersionName().equals(toscaVersionName))
                .findFirst()
                .orElseThrow(() -> new FailedOnboardingValidationException(format("ToscaDefinitionsVersion cannot be identified: %s",
                                                                                  toscaVersionName)));
        LOGGER.info("Completed getting TOSCA Definitions version by name: {}", toscaVersionName);
        return toscaDefinitionsVersion;
    }

    private String deriveDefinitionsVersion(final Map<String, Path> artifactPaths) {
        LOGGER.info("Started defining TOSCA definition version");
        final Path definitionsFile = artifactPaths.get(ENTRY_DEFINITIONS);
        try {
            String definitionsVersions = extractVersionFromFile(definitionsFile);
            LOGGER.info("Completed defining TOSCA definition version. Versions are: {}", definitionsVersions);
            return definitionsVersions;
        } catch (IOException e) {
            throw new FailedOnboardingValidationException(String.format("Failed to extract version from file: %s", definitionsFile), e);
        }
    }

    private String extractVersionFromFile(final Path definitionsFile) throws IOException {
        LOGGER.info("Started extracting versions from file: {}", definitionsFile);
        final String definitionsContent = Files.readString(definitionsFile);
        final JsonNode definitionsYaml = new YAMLMapper().readTree(definitionsContent);
        String version = definitionsYaml.get(Constants.TOSCA_DEFINITIONS_VERSION).asText();
        LOGGER.info("Completed extracting versions from file: {}", definitionsFile);
        return version;
    }
}
