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
package com.ericsson.amonboardingservice.presentation.services.dockerservice;

import java.util.List;
import java.util.function.Supplier;

import com.ericsson.amonboardingservice.utils.executor.ProcessExecutorResponse;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
public class RetrySkopeoCommand {

    private int maxAttempts;

    private long fixedBackOff;

    private List<String> retryOn;

    public ProcessExecutorResponse retry(Supplier<ProcessExecutorResponse> function) {
        int retryCounter = 0;
        while (true) {
            ProcessExecutorResponse response = function.get();
            if (retryErrorPresent(response.getCmdErrorResult())) {
                LOGGER.info("Attempt:{}. Retrying skopeo command due to: {}", retryCounter, response.getCmdErrorResult());
                retryCounter++;

                if (retryCounter >= maxAttempts) {
                    LOGGER.error("Max retries exceeded with message: {}", response.getCmdErrorResult());
                    return response;
                }

                try {
                    Thread.sleep(fixedBackOff);
                } catch (InterruptedException e) {
                    LOGGER.error("Exception occurred during thread sleep: {}", e.getMessage());
                    Thread.currentThread().interrupt();
                }
            } else {
                return response;
            }
        }
    }

    private boolean retryErrorPresent(String cmdResult) {
        return cmdResult != null && retryOn.stream().anyMatch(cmdResult::contains);
    }
}
