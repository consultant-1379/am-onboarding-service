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
package com.ericsson.amonboardingservice.infrastructure.client;

/**
 * Be aware that increase of the retry properties for the license service can greatly reduce usability
 * of the EVNFM demo version that provided without a NeLs microservice.
 */
public final class RetryProperties {

    private RetryProperties() {}

    public static final Properties DEFAULT = new Properties(500L, 10000L, 10, 1000L);
    public static final Properties LICENSE = new Properties(500L, 500L, 2, 1000L);
    public static final Properties SKOPEO = new Properties(0L, 0L, 10, 5000L);

    public record Properties(long connectTimeout, long requestTimeout, int maxAttempts, long backOff) { }

}
