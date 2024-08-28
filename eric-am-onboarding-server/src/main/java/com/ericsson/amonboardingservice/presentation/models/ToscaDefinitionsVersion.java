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
package com.ericsson.amonboardingservice.presentation.models;

import com.ericsson.amonboardingservice.utils.Constants;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum ToscaDefinitionsVersion {

    LEGACY_TOSCA(Constants.TOSCA_1_2_DEFINITIONS_VERSION),
    ETSI_TOSCA(Constants.TOSCA_1_3_DEFINITIONS_VERSION);

    private final String toscaVersionName;
}
