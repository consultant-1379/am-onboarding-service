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
package com.ericsson.amonboardingservice.api;

import org.springframework.http.ResponseEntity;

import java.io.File;

import static com.ericsson.amonboardingservice.utilities.RestUtils.httpGetCall;
import static com.ericsson.amonboardingservice.utilities.TestConstants.BASE_URI;
import static com.ericsson.amonboardingservice.utilities.TestConstants.HOST;

public final class HelmChartsClient {
    public static final String HELM_CHARTS_URI = "/charts";
    public static final String ALL_HELM_CHARTS_URI = HOST + BASE_URI + HELM_CHARTS_URI;

    private HelmChartsClient() {
    }

    public static ResponseEntity<String> getHelmCharts() {
        return httpGetCall(ALL_HELM_CHARTS_URI, String.class);
    }

    public static ResponseEntity<String> getHelmChart(String chartName) {
        String url = ALL_HELM_CHARTS_URI + File.separator + chartName;
        return httpGetCall(url, String.class);
    }
}
