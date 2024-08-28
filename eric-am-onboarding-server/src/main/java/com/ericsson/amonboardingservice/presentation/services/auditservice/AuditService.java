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

import com.ericsson.amonboardingservice.presentation.services.dockerservice.ContainerRegistryService;
import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ericsson.amonboardingservice.presentation.models.Chart;
import com.ericsson.amonboardingservice.presentation.models.ChartAudit;
import com.ericsson.amonboardingservice.presentation.repositories.ChartAuditRepository;
import com.ericsson.amonboardingservice.presentation.repositories.ChartRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing audit details for the onboarding service
 */
@Slf4j
@Service
public class AuditService implements ChartService, ChartAuditService {
    private static final String EXTERNAL_DOCKER = "external";
    private static final String ONBOARDED_CHART_AUDIT_ACTION = "onboarded";


    @Value("${container.registry.enabled}")
    private boolean isContainerRegistryEnabled;

    @Autowired
    private ContainerRegistryService containerRegistryService;

    @Autowired
    private ChartRepository chartRepository;

    @Autowired
    private ChartAuditRepository chartAuditRepository;

    @Override
    public Chart saveChart(final Chart chart) {
        LOGGER.info("Saving chart details {}", chart);
        return chartRepository.save(chart);
    }

    @Override
    public ChartAudit saveChartAudit(final ChartAudit chartAudit) {
        LOGGER.info("Saving chart audit information {}", chartAudit);
        return chartAuditRepository.save(chartAudit);
    }

    @Override
    public List<ChartAudit> listChartsAudits() {
        return chartAuditRepository.findAll();
    }

    @Override
    public List<Chart> listCharts() {
        return chartRepository.findAll();
    }

    public Chart saveChartDetails(final Path helmChartPath, final Path packageContents) {
        LOGGER.info("Started saving chart details, helmChartPath: {}, packageContents: {}", helmChartPath, packageContents);
        Chart chart = new Chart();
        chart.setPackageName(packageContents.getFileName().toString());
        chart.setHelmChart(helmChartPath.getFileName().toString());
        setDockerRegistryForChart(chart);
        Chart savedChart = saveChart(chart);
        LOGGER.info("Completed saving chart details, helmChartPath: {}, packageContents: {}", helmChartPath, packageContents);
        return savedChart;
    }

    public ChartAudit saveChartAuditDetails(Chart chart) {
        LOGGER.info("Started saving chart audit details from chart: {}", chart);
        ChartAudit chartAudit = new ChartAudit();
        chartAudit.setChart(chart);
        chartAudit.setAction(ONBOARDED_CHART_AUDIT_ACTION);
        ChartAudit savedChartAudit = saveChartAudit(chartAudit);
        LOGGER.info("Completed saving chart audit details from chart: {}", chart);
        return savedChartAudit;
    }

    private void setDockerRegistryForChart(Chart chart) {
        if (isContainerRegistryEnabled) {
            chart.setDockerRegistry(containerRegistryService.getDockerRegistry());
        } else {
            chart.setDockerRegistry(EXTERNAL_DOCKER);
        }
    }
}
