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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.Test;

public class FailedOnboardingValidationExceptionTest {

    private static final String ERROR_MESSAGE = "Failed to onboard";

    @Test
    public void testGetErrorsForGenericMessage() {
        // when
        var exception = new FailedOnboardingValidationException(ERROR_MESSAGE);

        // then
        assertThat(exception.getErrors()).isEmpty();
        assertThat(exception.getMessage()).isEqualTo(ERROR_MESSAGE);
        assertThat(exception.getMessage()).isEqualTo(ERROR_MESSAGE);
    }

    @Test
    public void testGetErrorsForGenericMessageWithCause() {
        // given
        var causeException = new RuntimeException("cause");

        // when
        var exception = new FailedOnboardingValidationException(ERROR_MESSAGE, causeException);

        // then
        assertThat(exception.getErrors()).isEmpty();
        assertThat(exception.getMessage()).isEqualTo(ERROR_MESSAGE);
        assertThat(exception.getMessage()).isEqualTo(ERROR_MESSAGE);
    }

    @Test
    public void testGetErrorsForErrorMessage() {
        // given
        var errorMessage = new ErrorMessage(ERROR_MESSAGE);
        var errorMessages = Collections.singletonList(errorMessage);

        // when
        var exception = new FailedOnboardingValidationException(errorMessages);

        // then
        assertThat(exception.getErrors()).hasSize(1);
        assertThat(exception.getErrors().get(0)).isEqualTo(errorMessage);
        assertThat(exception.getMessage()).isEqualTo(String.format("{%s}", ERROR_MESSAGE));
    }
}