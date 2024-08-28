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
package com.ericsson.amonboardingservice.infrastructure.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.retry.RetryContext;
import org.springframework.web.client.HttpServerErrorException;

import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class DiskSpaceAwareRetryPolicyTest {

    private RetryContext retryContext;

    private DiskSpaceAwareRetryPolicy sut;

    @BeforeEach
    void setUp() {
        retryContext = Mockito.mock(RetryContext.class);

        sut = new DiskSpaceAwareRetryPolicy(5);
    }

    @Test
    public void shouldNotRetryDiskSpaceException() {
        when(retryContext.getLastThrowable())
                .thenReturn(createExceptionWithBody("no space left on device"));

        assertThat(sut.canRetry(retryContext)).isFalse();
    }

    @Test
    public void shouldRetryOnException() {
        when(retryContext.getLastThrowable())
                .thenReturn(createExceptionWithBody("Internal server error"));

        assertThat(sut.canRetry(retryContext)).isTrue();
    }

    @Test
    public void shouldNotRetryWhenError28() {
        when(retryContext.getLastThrowable())
                .thenReturn(createExceptionWithBody("{message: \"Err\":28}"));

        assertThat(sut.canRetry(retryContext)).isFalse();
    }

    private HttpServerErrorException createExceptionWithBody(String body) {
        return new HttpServerErrorException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                body.getBytes(),
                Charset.defaultCharset()
        );
    }
}
