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
package com.ericsson.amonboardingservice.infrastructure.configuration;

import io.micrometer.context.ContextExecutorService;
import io.micrometer.context.ContextSnapshotFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class TraceAsyncConfiguration implements AsyncConfigurer {
    private final ThreadPoolTaskExecutor taskExecutor;

    @Override
    public Executor getAsyncExecutor() {
        return ContextExecutorService.wrap(taskExecutor.getThreadPoolExecutor(),
                () -> ContextSnapshotFactory.builder().build().captureAll());
    }
}
