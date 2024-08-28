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

import org.springframework.retry.RetryContext;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.web.client.HttpServerErrorException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiskSpaceAwareRetryPolicy extends SimpleRetryPolicy {

    public static final Pattern REGISTRY_NO_SPACE_LEFT_ERROR_PATTERN =
            Pattern.compile("\"Err\":28|no space left on device");

    public DiskSpaceAwareRetryPolicy(int maxAttempts) {
        super(maxAttempts);
    }

    @Override
    public boolean canRetry(RetryContext context) {
        Throwable t = context.getLastThrowable();
        if (t instanceof HttpServerErrorException) {
            String body = ((HttpServerErrorException) t).getResponseBodyAsString();
            String detailedMessage = t.getMessage();
            Matcher bodyMatcher = REGISTRY_NO_SPACE_LEFT_ERROR_PATTERN.matcher(body);
            Matcher messageMatcher = REGISTRY_NO_SPACE_LEFT_ERROR_PATTERN.matcher(detailedMessage);
            return !(bodyMatcher.find() || messageMatcher.find());
        }
        return super.canRetry(context);
    }

}
