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
package com.ericsson.amonboardingservice.utilities;

import com.google.common.base.Strings;
import com.google.common.io.Resources;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;


public final class AccTestUtils {

    private AccTestUtils() {
    }

    public static String getHost() {
        String host = System.getProperty("container.host");
        assertFalse(Strings.isNullOrEmpty(host));
        return host;
    }

    public static String getRunType() {
        String runType = System.getProperty("run.type");
        return (runType == null) ? "" : runType;
    }

    public static String getNamespace() {
        String namespace = System.getProperty("namespace");
        assertFalse(Strings.isNullOrEmpty(namespace));
        return namespace;
    }

    public static String getUserSecretName() {
        String userSecretName = System.getenv("IAM_ONBOARDING_SECRET");
        return (userSecretName == null) ? "eric-evnfm-rbac-default-user" : userSecretName;
    }

    public static boolean isRunLocal() {
        return TestConstants.RUN_TYPE.equals(TestConstants.RUN_TYPE_LOCAL);
    }

    public static Path getResource(String resourceName) throws URISyntaxException {
        return Paths.get(Resources.getResource(resourceName).toURI());
    }

    public static String decodeValue(String value) {
        return new String(java.util.Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    public static void delay(int milis) {
        try {
            Thread.sleep(milis);
        } catch (InterruptedException e) { // NOSONAR
            // Ignore
        }
    }
}
