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

import com.ericsson.amonboardingservice.exceptions.CommandTimedOutException;
import com.ericsson.amonboardingservice.exceptions.TestRuntimeException;
import com.ericsson.amonboardingservice.model.User;
import com.ericsson.amonboardingservice.utilities.executor.ProcessExecutor;
import com.ericsson.amonboardingservice.utilities.executor.ProcessExecutorResponse;
import com.google.common.base.Strings;
import org.json.JSONObject;

import java.util.InputMismatchException;

import static com.ericsson.amonboardingservice.utilities.TestConstants.NAMESPACE;
import static com.ericsson.amonboardingservice.utilities.TestConstants.USER_SECRET_NAME;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class KubernetesCLIUtils {
    private static final int COMMAND_TIMEOUT = 10000;
    private static final String GET_SECRET_COMMAND_TEMPLATE = "kubectl get secret %s -o json -n %s";
    public static final String DOCKER_REGISTRY_HOSTNAME = "eric-lcm-container-registry-registry";
    static ProcessExecutor processExecutor = new ProcessExecutor();

    private KubernetesCLIUtils() {
    }

    public static boolean isCommandRunSuccessful(String command) {
        boolean hasCommandRunSuccessfully = false;
        try {
            ProcessExecutorResponse response = processExecutor.executeProcessBuilder(command, COMMAND_TIMEOUT);
            if (response.getExitValue() != 0) {
                final String errorMessage = String.format("The program exited with a non zero code. It exited with: %s", response.getCmdResult());
                throw new InputMismatchException(errorMessage);
            } else {
                hasCommandRunSuccessfully = true;
            }
        } catch (Exception e) {
            LOGGER.error("Unknown exception occurred", e);
        }
        return hasCommandRunSuccessfully;
    }

    public static User getUserSecret() {
        String secret = executeGetUserSecretCommand();
        return extractUserFromSecret(secret);
    }

    public static String getDockerRegistryHost() {
        String secret = executeGetDockerRegistryHostCommand();
        return String.format("https://%s", extractDockerRegistryHostnameSecret(secret));
    }

    private static String executeGetUserSecretCommand() {
        String command = String.format(GET_SECRET_COMMAND_TEMPLATE, USER_SECRET_NAME, NAMESPACE);
        return executeCommand(command);
    }

    private static String executeGetDockerRegistryHostCommand() {
        String command = String.format(GET_SECRET_COMMAND_TEMPLATE, DOCKER_REGISTRY_HOSTNAME, NAMESPACE);
        return executeCommand(command);
    }

    private static String executeCommand(String command) {
        ProcessExecutorResponse response;
        try {
            response = processExecutor.executeProcessBuilder(command, 20000);
        } catch (CommandTimedOutException e) {
            throw new TestRuntimeException("Connection response timeout", e);
        }
        if (Strings.isNullOrEmpty(response.getCmdResult())) {
            throw new TestRuntimeException("Response from kubectl is empty");
        } else {
            return response.getCmdResult();
        }
    }

    private static User extractUserFromSecret(String secret) {
        String formattedSecret = secret.substring(secret.indexOf('{')).trim();

        LOGGER.info("Secret json content: " + formattedSecret);
        JSONObject data = new JSONObject(formattedSecret).getJSONObject("data");
        String name = AccTestUtils.decodeValue(data.getString("userid"));
        String password = AccTestUtils.decodeValue(data.getString("userpasswd"));
        return new User(name, password);
    }

    private static String extractDockerRegistryHostnameSecret(String secret) {
        String formattedSecret = secret.substring(secret.indexOf('{')).trim();

        LOGGER.info("Secret json content: " + formattedSecret);
        JSONObject data = new JSONObject(formattedSecret).getJSONObject("data");
        return AccTestUtils.decodeValue(data.getString("url"));
    }
}
