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
package com.ericsson.amonboardingservice.presentation.services.onboarding.request;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.amonboardingservice.presentation.models.ChartUrlsEntity.ChartTypeEnum.CNF;
import static com.ericsson.amonboardingservice.presentation.models.ChartUrlsEntity.ChartTypeEnum.CRD;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.am.shared.vnfd.model.HelmChart;
import com.ericsson.am.shared.vnfd.model.HelmChartType;
import com.ericsson.am.shared.vnfd.model.VnfDescriptorDetails;
import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.presentation.models.ChartUrlsEntity;
import com.ericsson.amonboardingservice.presentation.services.comparatorservice.ChartUrlsComparator;

@SpringBootTest()
@ActiveProfiles("test")
public class OnboardChartsTest extends AbstractDbSetupTest {

    @TempDir
    public Path tempFolder;

    @Test
    public void testAddHelmChartsFromToscaDirectory(@TempDir Path temporaryFolder) throws IOException {
        Map<Path, ChartUrlsEntity> helmCharts = new HashMap<>();

        Files.createFile(temporaryFolder.resolve("helmChart.tgz"));

        ChartUrlsEntity chartUrlsEntity = new ChartUrlsEntity();
        chartUrlsEntity.setPriority(1);
        chartUrlsEntity.setChartType(CNF);
        helmCharts.put(temporaryFolder, chartUrlsEntity);

        OnboardCharts.addHelmChartsFromTosca(temporaryFolder, helmCharts, 0);

        assertThat(helmCharts.size()).isEqualTo(2);
    }

    @Test
    public void testAddHelmChartsFromToscaFile(@TempDir Path temporaryFolder) throws IOException {
        Map<Path, ChartUrlsEntity> helmCharts = new HashMap<>();

        File file = Files.createFile(temporaryFolder.resolve("helmChart.tgz")).toFile();
        Path paths = file.toPath();

        OnboardCharts.addHelmChartsFromTosca(paths, helmCharts, 0);

        assertThat(helmCharts.size()).isEqualTo(1);
    }

    @Test
    public void getHelmChartsFromVNFDNotTOSCAMeta() throws IOException {
        String crdChartName = "crd_package1";
        String cnfChartName = "helm-chart-1";
        File crdChartFile = Files.createFile(tempFolder.resolve(crdChartName)).toFile();
        File cnfChartFile = Files.createFile(tempFolder.resolve(cnfChartName)).toFile();

        HelmChart crdChart = new HelmChart(crdChartName, HelmChartType.CRD, "crd_helm_package1");
        HelmChart cnfChart = new HelmChart(cnfChartName, HelmChartType.CNF, "helm_package2");
        final VnfDescriptorDetails vnfDescriptorDetails = new VnfDescriptorDetails();
        vnfDescriptorDetails.setHelmCharts(Arrays.asList(cnfChart, crdChart)); // putting cnf chart first

        Path chartsParentPath = crdChartFile.toPath().getParent();
        Path crdChartPath = chartsParentPath.resolve(crdChartName);
        Path cnfChartPath = chartsParentPath.resolve(cnfChartName);

        final Map<Path, ChartUrlsEntity> helmCharts = OnboardCharts.getHelmChartDirectory(vnfDescriptorDetails, chartsParentPath);
        assertThat(helmCharts).hasSize(2);
        assertThat(helmCharts.get(crdChartPath).getChartType()).isEqualTo(CRD);
        assertThat(helmCharts.get(crdChartPath).getPriority()).isEqualTo(1);
        assertThat(helmCharts.get(cnfChartPath).getChartType()).isEqualTo(CNF);
        assertThat(helmCharts.get(cnfChartPath).getPriority()).isEqualTo(2);
    }

    @Test
    public void testCorrectChartsOrder() {
        List<ChartUrlsEntity> chartUrls = createCharts();

        final ChartUrlsComparator chartUrlsComparator = new ChartUrlsComparator();
        chartUrls.sort(chartUrlsComparator);

        assertThat(chartUrls.get(0).getChartType()).isEqualTo(CRD);
        assertThat(chartUrls.get(0).getPriority()).isEqualTo(1);
        assertThat(chartUrls.get(1).getChartType()).isEqualTo(CRD);
        assertThat(chartUrls.get(1).getPriority()).isEqualTo(2);
        assertThat(chartUrls.get(2).getChartType()).isEqualTo(CNF);
        assertThat(chartUrls.get(2).getPriority()).isEqualTo(1);
        assertThat(chartUrls.get(3).getChartType()).isEqualTo(CNF);
        assertThat(chartUrls.get(3).getPriority()).isEqualTo(2);
    }

    private static List<ChartUrlsEntity> createCharts() {
        List<ChartUrlsEntity> chartUrls = new ArrayList<>();
        chartUrls.add(createChartUrl(CNF, 2));
        chartUrls.add(createChartUrl(CNF, 1));
        chartUrls.add(createChartUrl(CRD, 2));
        chartUrls.add(createChartUrl(CRD, 1));
        return chartUrls;
    }

    private static ChartUrlsEntity createChartUrl(ChartUrlsEntity.ChartTypeEnum chartType, int priority) {
        ChartUrlsEntity result = new ChartUrlsEntity();
        result.setPriority(priority);
        result.setChartType(chartType);
        return result;
    }
}
