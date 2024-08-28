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

import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.io.Resources;

public class Utility {

    private Utility() {}

    public static void checkResponseMessage(MvcResult mvcResult, String message) throws UnsupportedEncodingException {
        assertThat(mvcResult.getResponse().getContentAsString()).contains(message);
    }

    public static String readDataFromFile(String fileName, Charset charset) throws IOException {
        return Resources.toString(Resources.getResource(fileName), charset);
    }
}
