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
package com.ericsson.amonboardingservice.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile({ "prod" })
public class ConnectionPoolMonitoring {

    @Autowired
    private HikariDataSource hikariDataSource;

    private final double connectionsTreshold = 0.85;

    @Scheduled(fixedDelay = 1000)
    public void monitorConnectionPool() {
        int maxPoolSize = hikariDataSource.getMaximumPoolSize();
        int activeConnections = hikariDataSource.getHikariPoolMXBean().getActiveConnections();

        LOGGER.debug("Active DB connections count: {}", activeConnections);

        if ((double) activeConnections / maxPoolSize > connectionsTreshold) {
            int freeConnectionsCount = maxPoolSize - activeConnections;
            LOGGER.warn("There are only {} free DB connections", freeConnectionsCount);
        }
    }
}

