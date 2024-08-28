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
package com.ericsson.amonboardingservice.presentation.services.helmservice;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import static com.ericsson.amonboardingservice.utils.JsonUtils.formatJsonString;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import com.ericsson.amonboardingservice.infrastructure.client.RestClient;
import com.ericsson.amonboardingservice.infrastructure.configuration.HelmRegistryConfig;
import com.ericsson.amonboardingservice.presentation.services.dockerservice.UploadConcurrencyLevel;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.ericsson.amonboardingservice.presentation.services.manifestservice.ManifestService;
import com.ericsson.amonboardingservice.utils.TimeoutUtils;
import com.google.common.base.Stopwatch;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class HelmService {

    private static final String UPLOAD_CHART_URI = "/api/%s/charts";
    private static final String DELETE_URI = "/api/%s/charts/%s/%s";
    private static final String LIST_URI = "/api/%s/charts";
    private static final String LIST_VERSIONS_URI = "/api/%s/charts/%s";
    private static final String HEALTH_URI = "/health";

    private static final long UPLOAD_CHART_IO_ERROR_RETRY_DELAY_MILLIS = 5000L;

    @Value("${helm.registry.repo}")
    private String helmRegistryRepo;

    @Value("${helm.registry.postWaitTimeoutSeconds}")
    private String postWaitTimeoutSeconds;

    @Value("${onboarding.upload.concurrency:high}")
    private String uploadConcurrencyLevel;

    @Autowired
    private RestClient restClient;

    @Autowired
    private ManifestService manifestService;

    @Autowired
    private FileService fileService;

    @Autowired
    private HelmRegistryConfig helmRegistryConfig;

    /**
     * Checks if the chart is PRESENT, NOT_PRESENT and PRESENT_BUT_CONTENT_NOT_MATCHING
     *
     * @param chartToUpload
     *
     * @return HelmChartStatus enum having value PRESENT, NOT_PRESENT and PRESENT_BUT_CONTENT_NOT_MATCHING
     */
    public HelmChartStatus checkChartPresent(final Path chartToUpload) {
        final String chartName = chartToUpload.getFileName().toString();
        LOGGER.info("Attempting to get existing chart, {}", chartName);
        String chartUrl = getChartInstallUrl(chartName);
        Optional<File> existingChart = restClient.getFile(chartUrl,
                                                          "existing-chart.tgz",
                                                          helmRegistryConfig.getBASIC_AUTH_USER(),
                                                          helmRegistryConfig.getBASIC_AUTH_PASS());
        if (existingChart.isPresent()) {
            if (manifestService.compareArtifacts(existingChart.get().toPath(), chartToUpload)) {
                fileService.deleteFile(existingChart.get());
                return HelmChartStatus.PRESENT;
            } else {
                fileService.deleteFile(existingChart.get());
                return HelmChartStatus.PRESENT_BUT_CONTENT_NOT_MATCHING;
            }
        } else {
            return HelmChartStatus.NOT_PRESENT;
        }
    }

    /**
     * Checks if the chart is PRESENT, NOT_PRESENT and PRESENT_BUT_CONTENT_NOT_MATCHING
     *
     * @param chartToUpload
     *
     * @return HelmChartStatus enum having value PRESENT, NOT_PRESENT and PRESENT_BUT_CONTENT_NOT_MATCHING
     */
    public Map<Path, HelmChartStatus> checkChartPresent(final Collection<Path> chartToUpload) {
        LOGGER.info("Started checking whether chart: {} is PRESENT, NOT_PRESENT or PRESENT_BUT_CONTENT_NOT_MATCHING", chartToUpload);
        final Map<Path, HelmChartStatus> helmStatusMap = new HashMap<>();

        chartToUpload.forEach(chart -> {

            final String chartName = chart.getFileName().toString();
            LOGGER.info("Attempting to get existing chart, {}", chartName);
            String chartUrl = getChartInstallUrl(chartName);
            Optional<File> existingChart = restClient.getFile(chartUrl,
                                                              "existing-chart.tgz",
                                                              helmRegistryConfig.getBASIC_AUTH_USER(),
                                                              helmRegistryConfig.getBASIC_AUTH_PASS());
            if (existingChart.isPresent()) {
                if (manifestService.compareArtifacts(existingChart.get().toPath(), chart)) {
                    helmStatusMap.put(chart, HelmChartStatus.PRESENT);
                    fileService.deleteFile(existingChart.get());
                } else {
                    helmStatusMap.put(chart, HelmChartStatus.PRESENT_BUT_CONTENT_NOT_MATCHING);
                    fileService.deleteFile(existingChart.get());
                }
            } else {
                helmStatusMap.put(chart, HelmChartStatus.NOT_PRESENT);
            }
        });
        LOGGER.info("Completed checking whether chart: {} is PRESENT, NOT_PRESENT or PRESENT_BUT_CONTENT_NOT_MATCHING", chartToUpload);
        return helmStatusMap;
    }

    /**
     * Used to upload a new chart
     *
     * @param helmChartsWithStatus
     * @param timeoutDate
     *
     * @return ResponseEntity < String> response entity related to package upload
     */
    public Map<Path, ResponseEntity<String>> uploadNewCharts(final Map<Path, HelmChartStatus> helmChartsWithStatus, final LocalDateTime timeoutDate) {
        LOGGER.info("Started uploading new charts from: {}", helmChartsWithStatus);

        final Map<Path, ResponseEntity<String>> uploadedCharts = streamByUploadConcurrencyLevel(helmChartsWithStatus.entrySet())
                .filter(chartAndStatus -> chartAndStatus.getValue() == HelmChartStatus.NOT_PRESENT)
                .map(Map.Entry::getKey)
                .collect(toMap(identity(), chart -> uploadChart(chart, timeoutDate)));

        LOGGER.info("Completed uploading new charts from: {}", helmChartsWithStatus);

        return uploadedCharts;
    }

    private <T> Stream<T> streamByUploadConcurrencyLevel(final Collection<T> items) {
        return uploadConcurrencyLevel.equalsIgnoreCase(UploadConcurrencyLevel.NONE.name())
                ? items.stream()
                : items.parallelStream();
    }

    /***
     * Used to invoke the rest call to upload a new Helm chart
     *
     * @param chart
     * @return ResponseEntity
     */
    public ResponseEntity<String> uploadChart(final Path chart, final LocalDateTime timeoutDate) {
        final String url = getHelmChartUri();

        LOGGER.info("Uploading Helm chart: {} to {}", chart, url);

        final ResponseEntity<String> responseEntity = uploadChartWithRetry(chart, url, timeoutDate);

        LOGGER.info("Completed uploading Helm chart: {} to: {}", chart, url);

        return responseEntity;
    }

    private ResponseEntity<String> uploadChartWithRetry(final Path chart, final String url, final LocalDateTime timeoutDate) {
        final int timeout = TimeoutUtils.resolveTimeOut(timeoutDate, ChronoUnit.SECONDS);
        final Stopwatch stopwatch = Stopwatch.createStarted();
        while (stopwatch.elapsed(TimeUnit.SECONDS) < timeout) {
            try {
                return restClient.postFile(url, chart, helmRegistryConfig.getBASIC_AUTH_USER(), helmRegistryConfig.getBASIC_AUTH_PASS());
            } catch (final ResourceAccessException e) {
                LOGGER.info("Failed to upload Helm chart", e);
                LOGGER.info("Retrying in {} seconds", UPLOAD_CHART_IO_ERROR_RETRY_DELAY_MILLIS / 1000);

                delay(UPLOAD_CHART_IO_ERROR_RETRY_DELAY_MILLIS);
            }
        }

        return ResponseEntity.internalServerError().build();
    }

    private static void delay(final long timeInMillis) {
        try {
            LOGGER.debug("Sleeping for {} milliseconds", timeInMillis);
            Thread.sleep(timeInMillis);
        } catch (final InterruptedException e) {
            LOGGER.debug("Caught InterruptedException: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Get the Chart upload URI.
     *
     * @return Chart upload URI.
     */
    public String getHelmChartUri() {
        return restClient.buildUrl(helmRegistryConfig.getUrl(), String.format(UPLOAD_CHART_URI, helmRegistryRepo));
    }

    /**
     * List Charts stored in Helm Registry
     *
     * @return Json of Charts in Helm Registry
     */
    public String listCharts() {
        String url = restClient.buildUrl(helmRegistryConfig.getUrl(), String.format(LIST_URI, helmRegistryRepo));
        return formatJsonString(restClient.get(url,
                                               helmRegistryConfig.getBASIC_AUTH_USER(),
                                               helmRegistryConfig.getBASIC_AUTH_PASS()).getBody());
    }

    /**
     * List versions of charts with name
     *
     * @param chartName
     *
     * @return Path to created directory
     */
    public String listChartVersions(String chartName) {
        LOGGER.info("Executing rest call to get chart detail {} ", chartName);
        String url = restClient.buildUrl(helmRegistryConfig.getUrl(), String.format(LIST_VERSIONS_URI, helmRegistryRepo, chartName));
        return formatJsonString(restClient.get(url,
                                               helmRegistryConfig.getBASIC_AUTH_USER(),
                                               helmRegistryConfig.getBASIC_AUTH_PASS()).getBody());
    }

    /**
     * Retrieves helm package using provided {@code chartUrl} and stores it in a tmp file with {@code fileName}.
     *
     * @param chartUrl
     *         url to the a helm package
     * @param fileName
     *         name of a tmp file
     *
     * @return retrieved helm package
     */
    public Optional<File> getChart(String chartUrl, String fileName) {
        return restClient.getFile(chartUrl,
                                  fileName,
                                  helmRegistryConfig.getBASIC_AUTH_USER(),
                                  helmRegistryConfig.getBASIC_AUTH_PASS());
    }

    /**
     * Delete Chart from Registry
     *
     * @param chartName
     * @param chartVersion
     */
    public void deleteChart(String chartName, String chartVersion) {
        String url = restClient.buildUrl(helmRegistryConfig.getUrl(), String.format(DELETE_URI, helmRegistryRepo, chartName, chartVersion));
        LOGGER.info("Delete url is {}", url);
        restClient.delete(url, helmRegistryConfig.getBASIC_AUTH_USER(), helmRegistryConfig.getBASIC_AUTH_PASS());
    }

    /**
     * Get Health Status of Helm Registry
     *
     * @return health
     */
    public String healthStatus() {
        try {
            String url = restClient.buildUrl(helmRegistryConfig.getUrl(), HEALTH_URI);
            return restClient.get(url, this.helmRegistryConfig.getBASIC_AUTH_USER(), this.helmRegistryConfig.getBASIC_AUTH_PASS()).getBody();
        } catch (Exception e) {
            LOGGER.debug("healthStatus:", e);
            return "{health : false}";
        }
    }

    public String getChartInstallUrl(final String chart) {
        return String.format("%s/%s/charts/%s", helmRegistryConfig.getUrl(), helmRegistryRepo, chart);
    }
}
