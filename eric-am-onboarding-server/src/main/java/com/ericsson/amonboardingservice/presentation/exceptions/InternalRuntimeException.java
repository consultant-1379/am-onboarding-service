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

/**
 * Created by ekellmi on 9/6/18.
 */
public class InternalRuntimeException extends RuntimeException {
    public InternalRuntimeException(String message) {
        super(message);
    }

    public InternalRuntimeException(Exception e) {
        super(e);
    }

    public InternalRuntimeException(String message, Throwable t) {
        super(message, t);
    }
}
