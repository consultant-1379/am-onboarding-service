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

import com.ericsson.amonboardingservice.presentation.models.ChartAudit;
/**
 * Interface for saving audit details for the onboarding service
 */
public interface ChartAuditService {
    /**
     * Save chart audit information for onboarding history
     * @param chartAudit :audit details to be saved
     */
    ChartAudit saveChartAudit(ChartAudit chartAudit);

    /**
     * List all chartAudit details stored
     */
    List<ChartAudit> listChartsAudits();
}
