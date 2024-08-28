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
package com.ericsson.amonboardingservice.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Slf4j
public final class TimeoutUtils {

    private static final int EXCLUSIVE_MINUTE_SHIFT = 1;

    private TimeoutUtils() {}

    public static int resolveTimeOut(LocalDateTime timeoutDate, ChronoUnit unit) {
        if (Objects.isNull(unit)) {
            return 0;
        }

        LocalDateTime currentTime = LocalDateTime.now();
        int timeout = (int) unit.between(currentTime, timeoutDate) + EXCLUSIVE_MINUTE_SHIFT;

        if (timeout < 0) {
            LOGGER.warn("Timeout has now expired, setting timeout to 0");
            return 0;
        }
        return timeout;
    }

}
