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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.TestUtils;
import com.ericsson.amonboardingservice.presentation.exceptions.UnhandledException;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;

@SpringBootTest
@ActiveProfiles("test")
public class HelmChartUtilsTest extends AbstractDbSetupTest {

    @Autowired
    private FileService fileService;

    @Test
    public void getChartYamlProperty() throws URISyntaxException {
        Path helmChart = TestUtils.getResource("sampledescriptor-0.0.1-223.tgz");
        String chartName = HelmChartUtils.getChartYamlProperty(helmChart, "name");
        String chartVersion = HelmChartUtils.getChartYamlProperty(helmChart, "version");
        assertThat(chartName).isEqualTo("sampledescriptor");
        assertThat(chartVersion).isEqualTo("0.0.1-223");
    }

    @Test
    public void getChartYamlPropertyFromTemporaryChart() throws URISyntaxException, IOException {
        Path tempChart = fileService.createTempPath("sampledescriptor-0.1.1-223.tgz");
        Path helmChart = TestUtils.getResource("sampledescriptor-0.0.1-223.tgz");
        Files.copy(helmChart, tempChart);
        String chartName = HelmChartUtils.getChartYamlProperty(tempChart, "name");
        String chartVersion = HelmChartUtils.getChartYamlProperty(tempChart, "version");
        assertThat(chartName).isEqualTo("sampledescriptor");
        assertThat(chartVersion).isEqualTo("0.0.1-223");
    }

    @Test
    public void shouldParseChartUrl(){
        Pair<String, String> chartNameAndVersion = HelmChartUtils.parseChartUrl("http://10.210.53.96:31028/api/onboarded/charts/Ericsson.SGSN-MME/1.20");
        assertThat(chartNameAndVersion.getLeft()).isEqualTo("Ericsson.SGSN-MME");
        assertThat(chartNameAndVersion.getRight()).isEqualTo("1.20");
    }

    @Test
    public void shouldParseChartUrlWithExtension(){
        Pair<String, String> chartNameAndVersion = HelmChartUtils.parseChartUrl("http://10.210.53.96:31028/onboarded/charts/test-scale-chart-0.1.0.tgz");
        assertThat(chartNameAndVersion.getLeft()).isEqualTo("test-scale-chart");
        assertThat(chartNameAndVersion.getRight()).isEqualTo("0.1.0");
    }

    @Test
    public void shouldFailToParseInvalidChartUrl(){
        assertThrows(UnhandledException.class, () -> HelmChartUtils.parseChartUrl("invalidChartUrl"));
    }

    @Test
    public void shouldReturnTrueIfContainHelmfile() throws URISyntaxException {
        Path helmChart = TestUtils.getResource("helmfile-a.tgz");
        boolean result = HelmChartUtils.containHelmfile(helmChart);
        assertThat(result).isTrue();
    }

    @Test
    public void shouldReturnFalseIfPackageNotContainHelmfile() throws URISyntaxException {
        Path helmChart = TestUtils.getResource("sampledescriptor-0.0.1-223.tgz");
        boolean result = HelmChartUtils.containHelmfile(helmChart);
        assertThat(result).isFalse();
    }

}
