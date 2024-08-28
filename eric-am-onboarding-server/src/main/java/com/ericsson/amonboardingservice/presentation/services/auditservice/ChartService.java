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
package com.ericsson.amonboardingservice.presentation.services.auditservice;

import java.util.List;

import com.ericsson.amonboardingservice.presentation.models.Chart;

/**
 * Interface for managing chart information
 */
public interface ChartService {

    /**
     * Save chart information
     * @param chart :Chart details to be saved
     */
    Chart saveChart(Chart chart);

    /**
     * List all chart details stored in audit Service
     */
    List<Chart> listCharts();
}
