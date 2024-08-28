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
package com.ericsson.amonboardingservice.utils.executor;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProcessExecutorResponse {

    private int exitValue;
    private String cmdResult;
    private String cmdErrorResult;

    public ProcessExecutorResponse(int exitValue, String cmdResult, String cmdErrorResult) {
        this.exitValue = exitValue;
        this.cmdResult = cmdResult;
        this.cmdErrorResult = cmdErrorResult;
    }

    public boolean isProcessTerminatedNormally() {
        return this.exitValue == 0;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
