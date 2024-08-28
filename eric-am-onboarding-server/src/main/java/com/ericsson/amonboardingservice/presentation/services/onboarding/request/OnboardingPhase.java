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

import com.fasterxml.jackson.annotation.JsonValue;

public enum OnboardingPhase {
    GENERATE_CHECKSUM_PHASE("Checksum generation"),
    ZIP_UNPACKING_PHASE("ZIP unpacking"),
    CSAR_SIGNATURE_VALIDATION_PHASE("Csar signature validation"),
    CSAR_UNPACKING_PHASE("CSAR unpacking"),
    MANIFEST_SIGNATURE_VALIDATION_PHASE("Manifest signature validation"),
    TOSCA_VERSION_IDENTIFICATION_PHASE("TOSCA version identification"),
    ONBOARDING_SYNCHRONIZATION_PHASE("Onboarding synchronization"),
    TOSCAO_VALIDATION_PHASE("TOSCA-O validation"),
    VNFD_VALIDATION_PHASE("VNFD validation"),
    CHARTS_ONBOARDING_PHASE("Charts onboarding"),
    IMAGES_ONBOARDING_PHASE("Images onboarding"),
    PERSIST_PHASE("Persisting operation");

    private final String phaseName;

    OnboardingPhase(final String phaseName) {
        this.phaseName = phaseName;
    }

    @Override
    @JsonValue
    public String toString() {
        return String.valueOf(phaseName);
    }
}
