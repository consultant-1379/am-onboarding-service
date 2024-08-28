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
package com.ericsson.amonboardingservice.utils.executor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ericsson.amonboardingservice.presentation.exceptions.CommandTimedOutException;
import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ProcessExecutor {
    private static final Pattern NON_ASCII_CHARS = Pattern.compile("[^\\p{ASCII}]");
    private static final boolean IS_WINDOWS = System.getProperty("os.name", "<none>").toLowerCase().contains("windows");
    private static Pattern usernamePassword;
    private static Pattern skopeoCmdUsernamePassword;

    @Value("${docker.registry.user.name}")
    private String userName;

    @Value("${docker.registry.user.password}")
    private String password;

    @PostConstruct
    @SuppressWarnings({ "squid:S2325", "squid:S2696" })
    private void init() {
        usernamePassword = Pattern.compile(userName + " " + password, Pattern.LITERAL);
        skopeoCmdUsernamePassword = Pattern.compile(userName + ":" + password, Pattern.LITERAL);
    }

    public ProcessExecutorResponse executeProcessBuilder(String command, int timeout) throws CommandTimedOutException {
        List<String> completeCommand = new ArrayList<>();
        if (IS_WINDOWS) {
            completeCommand.add("cmd.exe");
            completeCommand.add("/c");
        } else {
            completeCommand.add("bash");
            completeCommand.add("-c");
        }
        completeCommand.add(command);
        ProcessBuilder pb = new ProcessBuilder(completeCommand);
        Process process = null;
        try {
            LOGGER.info("Executing {}", hideSensitiveData(String.join(" ", pb.command())));
            process = pb.start();
            final boolean commandTimedOut = process.waitFor(timeout, TimeUnit.MINUTES);
            if (!commandTimedOut) {
                LOGGER.error("Command: {} took more than: {} minutes",
                             hideSensitiveData(String.join(" ", completeCommand)), timeout);
                throw new CommandTimedOutException("Unable to get the result in the time specified");
            }
            return parseProcessOutput(process);
        } catch (IOException e) {
            LOGGER.error("Failed to run process due to", e);
            throw new InternalRuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Failed to complete process due to", e);
            throw new InternalRuntimeException(e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroy();
            }
        }
    }

    private static ProcessExecutorResponse parseProcessOutput(Process process) throws IOException {
        final String inputStreamResult = collectResultByStreamType(process.getInputStream());
        final String errorStreamResult = collectResultByStreamType(process.getErrorStream());

        final ProcessExecutorResponse processExecutorResponse = new ProcessExecutorResponse(process.exitValue(),
                                                                                            inputStreamResult,
                                                                                            errorStreamResult);

        LOGGER.info("ProcessExecutorResponse: {} ", processExecutorResponse);
        return processExecutorResponse;
    }

    private static String collectResultByStreamType(final InputStream stream) throws IOException {
        try (InputStreamReader inputStream = new InputStreamReader(stream, StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(inputStream)) {
            String cmdResult = br.lines().map(String::trim)
                    .collect(Collectors.joining(System.lineSeparator()));
            Matcher m = NON_ASCII_CHARS.matcher(cmdResult);
            return m.replaceAll("");
        }
    }

    static String hideSensitiveData(String command) {
        String modifiedCmd = usernamePassword.matcher(command).replaceAll("****** ******");
        modifiedCmd = skopeoCmdUsernamePassword.matcher(modifiedCmd).replaceAll("******:******");

        return modifiedCmd;
    }
}
