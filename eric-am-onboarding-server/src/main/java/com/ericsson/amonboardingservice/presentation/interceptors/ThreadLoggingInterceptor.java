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
package com.ericsson.amonboardingservice.presentation.interceptors;

import com.ericsson.amonboardingservice.aspect.AsyncExecutionTrackerAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class ThreadLoggingInterceptor implements HandlerInterceptor {

    private final AtomicInteger concurrentRequests = new AtomicInteger(0);

    @Autowired
    private AsyncExecutionTrackerAspect asyncExecutionTrackerAspect;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        int currentConcurrentRequests = concurrentRequests.incrementAndGet();
        int asyncThreadsCount = asyncExecutionTrackerAspect.getActiveAsyncThreads();
        LOGGER.info("Entering operation. Number of concurrent requests: {}. Async operations: {}", currentConcurrentRequests,
                asyncThreadsCount);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        int currentConcurrentRequests = concurrentRequests.decrementAndGet();
        int asyncThreadsCount = asyncExecutionTrackerAspect.getActiveAsyncThreads();
        LOGGER.info("Exiting operation. Number of concurrent requests: {}. Async operations: {}", currentConcurrentRequests,
                asyncThreadsCount);
    }
}
