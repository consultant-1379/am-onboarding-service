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
package com.ericsson.amonboardingservice.presentation.services.packageservice;

import java.util.Arrays;

public enum PackageResponseVerbosity {
    UI("ui"), DEFAULT("default");

    private final String verbosity;

    PackageResponseVerbosity(String v) {
        verbosity = v;
    }

    public static PackageResponseVerbosity getByValue(String value) {
        return Arrays.stream(values()).filter(v -> v.verbosity.equals(value))
                .findFirst().orElseThrow(() ->
                        new IllegalArgumentException(String.format("Response verbosity with level %s is not defined", value)));
    }
}
