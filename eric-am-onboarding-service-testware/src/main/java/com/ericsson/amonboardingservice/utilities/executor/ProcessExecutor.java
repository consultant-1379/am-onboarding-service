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
package com.ericsson.amonboardingservice.utilities.executor;

import com.ericsson.amonboardingservice.exceptions.CommandTimedOutException;
import com.ericsson.amonboardingservice.exceptions.InternalRuntimeException;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public final class ProcessExecutor {

    private static final Pattern NON_ASCII_CHARS = Pattern.compile("[^\\p{ASCII}]");
    private static final boolean IS_WINDOWS = System.getProperty("os.name", "<none>").toLowerCase().contains("windows");

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
        pb.redirectErrorStream(true);
        Process process = null;
        try {
            LOGGER.info("Executing {}", pb.command().stream().collect(Collectors.joining(" ")));
            process = pb.start();
            final boolean commandTimedOut = process.waitFor(timeout, TimeUnit.MINUTES);
            if (!commandTimedOut) {
                LOGGER.error("Command :: {} took more than : {} minutes", completeCommand, timeout);
                throw new CommandTimedOutException("Unable to get the result in the time specified");
            }
            return parseProcessOutput(process);
        } catch (IOException e) {
            LOGGER.error("Failed to run process due to:", e);
            throw new InternalRuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Failed to complete process due to:", e);
            throw new InternalRuntimeException(e);
        } finally {
            if (process != null && process.isAlive())
                process.destroy();
        }
    }

    private static ProcessExecutorResponse parseProcessOutput(Process process) throws IOException {
        final ProcessExecutorResponse processExecutorResponse = new ProcessExecutorResponse();
        processExecutorResponse.setExitValue(process.exitValue());
        try (InputStreamReader inputStream = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(inputStream)) {
            String cmdResult = br.lines().map(String::trim)
                    .collect(Collectors.joining(System.lineSeparator()));
            Matcher m = NON_ASCII_CHARS.matcher(cmdResult);
            cmdResult = m.replaceAll("");
            processExecutorResponse.setCmdResult(cmdResult);
        }
        LOGGER.info("ProcessExecutorResponse :: {} ", processExecutorResponse);
        return processExecutorResponse;
    }
}
