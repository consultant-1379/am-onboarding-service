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

import com.ericsson.amonboardingservice.presentation.models.ChartUrlsEntity;

public class ChartUrlsComparator implements Comparator<ChartUrlsEntity> {

    @Override
    public int compare(ChartUrlsEntity chart1, ChartUrlsEntity chart2) {
        if (chart1.getChartType().equals(chart2.getChartType())) {
            return Integer.compare(chart1.getPriority(), chart2.getPriority());
        }
        return Integer.compare(chart1.getChartType().getPriority(), chart2.getChartType().getPriority());
    }
}
