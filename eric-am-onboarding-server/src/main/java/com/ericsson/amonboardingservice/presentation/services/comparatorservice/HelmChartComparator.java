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
package com.ericsson.amonboardingservice.presentation.services.comparatorservice;

import java.util.Comparator;

import com.ericsson.am.shared.vnfd.model.HelmChart;
import com.ericsson.am.shared.vnfd.model.HelmChartType;

public class HelmChartComparator implements Comparator<HelmChart> {

    @Override
    public int compare(final HelmChart chart1, final HelmChart chart2) {
        if (chart1.getChartType() != chart2.getChartType()) {
            if (chart1.getChartType() == HelmChartType.CRD) {
                return -1;
            } else {
                return 1;
            }
        }
        return 0;
    }
}
