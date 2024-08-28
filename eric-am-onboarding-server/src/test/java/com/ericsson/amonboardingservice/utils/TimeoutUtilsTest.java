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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

public class TimeoutUtilsTest {

    @Test
    public void shouldReturnTimeoutInSeconds() {
        LocalDateTime timeoutDate = LocalDateTime.now().plusSeconds(5);
        assertEquals(5, TimeoutUtils.resolveTimeOut(timeoutDate, ChronoUnit.SECONDS));
    }

    @Test
    public void shouldReturnTimeoutInMinutes() {
        LocalDateTime timeoutDate = LocalDateTime.now().plusMinutes(5);
        assertEquals(5, TimeoutUtils.resolveTimeOut(timeoutDate, ChronoUnit.MINUTES));
    }

    @Test
    public void shouldReturnZeroOnChronUnitIsNull() {
        assertEquals(0, TimeoutUtils.resolveTimeOut(LocalDateTime.now(), null));
    }

}