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
package com.ericsson.amonboardingservice.presentation.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.ericsson.amonboardingservice.presentation.services.dockerservice.ContainerRegistryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.presentation.models.Chart;
import com.ericsson.amonboardingservice.presentation.models.ChartAudit;
import com.ericsson.amonboardingservice.presentation.repositories.ChartAuditRepository;
import com.ericsson.amonboardingservice.presentation.repositories.ChartRepository;
import com.ericsson.amonboardingservice.presentation.services.auditservice.AuditService;

@SpringBootTest()
@ActiveProfiles("test")
public class AuditServiceTest extends AbstractDbSetupTest {

    @Autowired
    private AuditService auditService;

    @MockBean
    private ChartRepository chartRepository;

    @MockBean
    private ChartAuditRepository chartAuditRepository;

    @MockBean
    private ContainerRegistryService containerRegistryService;

    @TempDir
    public Path tempFolder;

    @Test
    public void testSaveChartAuditDetails() {
        var savedChart = auditService.saveChart(createChart());
        auditService.saveChartAuditDetails(savedChart);
        verify(chartRepository).save(any(Chart.class));
        verify(chartAuditRepository).save(any(ChartAudit.class));
    }

    @Test
    public void testSaveChartDetailsWhenContainerRegistryEnabled() throws IOException {
        // given
        ReflectionTestUtils.setField(auditService, "isContainerRegistryEnabled", true);
        var dockerRegistry = "DockerRegistry";
        var captor = ArgumentCaptor.forClass(Chart.class);
        when(containerRegistryService.getDockerRegistry()).thenReturn(dockerRegistry);

        // when
        auditService.saveChartDetails(
                Files.createFile(tempFolder.resolve("helm-chart")),
                Files.createFile(tempFolder.resolve("package-content")));

        // then
        verifyNoInteractions(chartAuditRepository);
        verify(chartRepository).save(captor.capture());
        var chart = captor.getValue();
        assertThat(chart.getDockerRegistry()).isEqualTo(dockerRegistry);
    }

    @Test
    public void testSaveChartDetailsWhenContainerRegistryDisabled() throws IOException {
        // given
        ReflectionTestUtils.setField(auditService, "isContainerRegistryEnabled", false);
        var captor = ArgumentCaptor.forClass(Chart.class);

        // when
        auditService.saveChartDetails(
                Files.createFile(tempFolder.resolve("helm-chart")),
                Files.createFile(tempFolder.resolve("package-content")));

        // then
        verifyNoInteractions(chartAuditRepository);
        verifyNoInteractions(containerRegistryService);
        verify(chartRepository).save(captor.capture());
        var chart = captor.getValue();
        assertThat(chart.getDockerRegistry()).isEqualTo("external");
    }

    @Test
    public void testListChartsAudits() {
        // when/then
        auditService.listChartsAudits();
        verify(chartAuditRepository).findAll();
    }

    @Test
    public void testListCharts() {
        // when/then
        auditService.listCharts();
        verify(chartRepository).findAll();
    }

    private Chart createChart() {
        Chart chart = new Chart();
        chart.setDockerRegistry("testDockerReg");
        chart.setHelmChart("testHelmReg");
        chart.setPackageName("testPackageName");
        return chart;
    }
}
