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
package com.ericsson.amonboardingservice.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Aspect
@Component
public class AsyncExecutionTrackerAspect {

    @Value("${onboarding.logConcurrentRequests}")
    private boolean logConcurrentRequests;

    private AtomicInteger activeAsyncThreads = new AtomicInteger(0);

    @Before("@annotation(org.springframework.scheduling.annotation.Async)")
    public void beforeAdvice(JoinPoint joinPoint) {
        if (logConcurrentRequests) {
            activeAsyncThreads.incrementAndGet();
        }
    }

    @After("@annotation(org.springframework.scheduling.annotation.Async)")
    public void afterAdvice(JoinPoint joinPoint) {
        if (logConcurrentRequests) {
            activeAsyncThreads.decrementAndGet();
        }
    }

    public int getActiveAsyncThreads() {
        return activeAsyncThreads.get();
    }
}
