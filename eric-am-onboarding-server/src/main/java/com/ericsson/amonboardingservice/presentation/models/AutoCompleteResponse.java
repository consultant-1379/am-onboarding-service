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

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AutoCompleteResponse {

    private List<String> type;
    private List<String> provider;
    private List<String> packageVersion;
    private List<String> softwareVersion;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
