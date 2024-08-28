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
package com.ericsson.amonboardingservice.presentation.exceptions;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class FailedOnboardingValidationException extends RuntimeException {
    private final transient List<ErrorMessage> errors;

    public FailedOnboardingValidationException(final List<ErrorMessage> errors) {
        this.errors = errors;
    }

    public FailedOnboardingValidationException(final String message) {
        super(message);
        errors = new ArrayList<>();
    }

    public FailedOnboardingValidationException(final String message, final Exception e) {
        super(message, e);
        errors = new ArrayList<>();
    }

    public List<ErrorMessage> getErrors() {
        return errors;
    }

    @Override
    public String getMessage() {
        if (StringUtils.isBlank(super.getMessage())) {
            return errors.stream()
                    .map(ErrorMessage::getMessage)
                    .map(message -> String.format("{%s}", message))
                    .collect(Collectors.joining("\n"));
        }
        return super.getMessage();
    }
}
