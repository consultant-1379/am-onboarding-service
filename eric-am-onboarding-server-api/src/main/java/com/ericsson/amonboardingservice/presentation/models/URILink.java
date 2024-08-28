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

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

/**
 * This type represents a link to a resource.
 */
@Setter
public class URILink {
    @Getter(onMethod = @__({@NotNull}))
    @JsonProperty("href")
    private String href = null;

    @JsonIgnore
    private String rel;

    public URILink href(String href) {
        this.href = href;
        return this;
    }

    public URILink rel(String rel) {
        this.rel = rel;
        return this;
    }

    public String getFormattedRel() {
        return "rel=\"" + rel + "\"";
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}

