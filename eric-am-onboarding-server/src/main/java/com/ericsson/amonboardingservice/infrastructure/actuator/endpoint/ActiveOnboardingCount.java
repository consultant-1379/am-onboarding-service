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
package com.ericsson.amonboardingservice.infrastructure.actuator.endpoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "activeonboardingcount")
public class ActiveOnboardingCount {

    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @ReadOperation(produces = MediaType.TEXT_PLAIN_VALUE)
    public String count() {
        return Integer.toString(threadPoolTaskExecutor.getActiveCount());
    }
}
