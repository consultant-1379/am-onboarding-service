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

import static java.util.stream.Collectors.toList;

import static com.ericsson.amonboardingservice.presentation.models.ChartUrlsEntity.ChartTypeEnum.CNF;
import static com.ericsson.amonboardingservice.presentation.services.onboarding.request.OnboardingPhase.CHARTS_ONBOARDING_PHASE;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_HELM_DEFINITIONS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ericsson.amonboardingservice.presentation.services.packageservice.CleanUpOnFailureService;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageDatabaseService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.ericsson.am.shared.vnfd.model.HelmChart;
import com.ericsson.am.shared.vnfd.model.VnfDescriptorDetails;
import com.ericsson.amonboardingservice.presentation.exceptions.ChartOnboardingException;
import com.ericsson.amonboardingservice.presentation.models.ChartUrlsEntity;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.services.comparatorservice.ChartUrlsComparator;
import com.ericsson.amonboardingservice.presentation.services.comparatorservice.HelmChartComparator;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.ericsson.amonboardingservice.presentation.services.helmservice.HelmChartStatus;
import com.ericsson.amonboardingservice.presentation.services.helmservice.HelmService;
import com.ericsson.amonboardingservice.presentation.services.packageservice.OnboardingPackageState;
import com.ericsson.amonboardingservice.utils.Constants;
import com.ericsson.amonboardingservice.utils.HelmChartUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Find the charts in the package and upload them to the helm chart registry
 */
@Slf4j
@Component
public class OnboardCharts implements RequestHandler {
    private static final ChartUrlsComparator CHART_URLS_COMPARATOR = new ChartUrlsComparator();

    @Autowired
    private HelmService helmService;

    @Autowired
    private AppPackageRepository appPackageRepository;

    @Autowired
    private PackageDatabaseService databaseService;

    @Autowired
    private CleanUpOnFailureService failureService;

    @Autowired
    private FileService fileService;

    @Override
    public void handle(final PackageUploadRequestContext context) {
        LOGGER.info("Starting OnboardCharts onboarding handler");

        Map<Path, ChartUrlsEntity> helmCharts = new HashMap<>();
        byte[] helmfile = null;
        Map<String, Path> artifactPaths = context.getArtifactPaths();
        if (context.isEtsiPackage()) {
            Path csarPath = artifactPaths.get(Constants.CSAR_DIRECTORY);
            helmCharts = getHelmChartDirectory(context.getVnfd(), csarPath);
            helmfile = getHelmfile(context.getVnfd(), csarPath);
        } else {
            addHelmChartsFromTosca(artifactPaths.get(ENTRY_HELM_DEFINITIONS), helmCharts, 1);
        }
        context.setHelmChartPaths(helmCharts.keySet());
        context.setHelmfile(helmfile);
        synchronized (this) {
            String packageId = context.getPackageId();
            LOGGER.info("Starting to save chart details for package {}", packageId);
            saveChartDetails(packageId, helmCharts, helmfile);
            LOGGER.info("Completed save chart details for package {}", packageId);
            Map<Path, HelmChartStatus> helmChartStatus = helmService.checkChartPresent(helmCharts.keySet());
            context.setHelmChartStatus(helmChartStatus);
            LocalDateTime timeoutDate = context.getTimeoutDate();
            Map<Path, ResponseEntity<String>> uploadHelmChart = helmService.uploadNewCharts(helmChartStatus, timeoutDate);

            handleHelmUploadResponse(uploadHelmChart, packageId, helmChartStatus);
        }

        LOGGER.info("Successfully finished OnboardCharts onboarding handler");
    }

    @Override
    public String getName() {
        return CHARTS_ONBOARDING_PHASE.toString();
    }

    static Map<Path, ChartUrlsEntity> getHelmChartDirectory(final VnfDescriptorDetails vnfDescriptorDetails, final Path csarPath) {
        LOGGER.info("Started getting Helm chart directory");
        int priority = 1;
        StringBuilder errorMessage = new StringBuilder();
        Map<Path, ChartUrlsEntity> helmChartPathToChartUrl = new HashMap<>();
        final HelmChartComparator helmChartComparator = new HelmChartComparator();
        List<HelmChart> helmCharts = vnfDescriptorDetails.getHelmCharts()
                .stream()
                .filter(item -> !HelmChartUtils.containHelmfile(csarPath.resolve(item.getPath())))
                .sorted(helmChartComparator)
                .collect(toList());

        List<String> helmChartPaths = helmCharts.stream().map(HelmChart::getPath).collect(Collectors.toUnmodifiableList());
        LOGGER.info("Descriptor Helm Chart paths: {} ", helmChartPaths);

        if (vnfDescriptorDetails.getFlavours() == null) {
            for (HelmChart helmChart : helmCharts) {
                String helmChartPath = helmChart.getPath();
                ChartUrlsEntity.ChartTypeEnum chartType = ChartUrlsEntity.ChartTypeEnum.valueOf(helmChart.getChartType().toString());
                helmChartPathToChartUrl.put(csarPath.resolve(helmChartPath), createChartUrlEntity(priority, chartType, helmChart.getChartKey()));
                priority++;
            }
        } else {
            for (HelmChart helmChart : helmCharts) {
                String helmChartPath = helmChart.getPath();
                ChartUrlsEntity.ChartTypeEnum chartType = ChartUrlsEntity.ChartTypeEnum.valueOf(helmChart.getChartType().toString());
                helmChartPathToChartUrl.put(csarPath.resolve(helmChartPath), createChartUrlEntity(0, chartType, helmChart.getChartKey()));
            }
        }

        checkAllChartExist(errorMessage, helmChartPathToChartUrl);

        if (StringUtils.isNotEmpty(errorMessage)) {
            throw new ChartOnboardingException(errorMessage.toString());
        }

        LOGGER.info("Helm charts to be uploaded with priority : {} ", helmChartPathToChartUrl);
        return helmChartPathToChartUrl;
    }

    private static void checkAllChartExist(final StringBuilder errorMessage, final Map<Path, ChartUrlsEntity> helmChartPathToChartUrl) {
        LOGGER.info("Starts checking all charts are exist");
        helmChartPathToChartUrl
                .keySet()
                .stream()
                .filter(chart -> chart != null && !chart.toFile().exists())
                .forEach(chart -> errorMessage.append(
                        String.format(Constants.HELM_CHART_NOT_PRESENT, chart.getFileName(), chart)));
        LOGGER.info("Completed checking all charts are exist");
    }

    public static void addHelmChartsFromTosca(Path entryHelm, Map<Path, ChartUrlsEntity> helmCharts, Integer priority) {
        LOGGER.info("Started adding Helm charts from directory {}", entryHelm);
        if (entryHelm != null) {
            if (Files.isDirectory(entryHelm)) {
                try (Stream<Path> entries = Files.list(entryHelm)) {
                    List<Path> helmChartPaths = entries
                            .filter(Objects::nonNull)
                            .collect(toList());
                    addHelmChartsWithPriority(helmCharts, priority, helmChartPaths);
                } catch (IOException e) {
                    throw new ChartOnboardingException(
                            String.format("Unable to add files from the directory %s due to : %s ", entryHelm,
                                          e.getMessage()), e);
                }
            } else {
                helmCharts.computeIfAbsent(entryHelm, k -> createChartUrlEntity(priority, CNF, null));
            }
        }
        LOGGER.info("Completed adding Helm charts from directory {}", entryHelm);
    }

    private static void addHelmChartsWithPriority(Map<Path, ChartUrlsEntity> helmCharts, Integer priority,
                                                  List<Path> helmChartPaths) {
        LOGGER.info("Started adding Helm charts: {} with priority: {}", helmCharts, priority);
        if (!CollectionUtils.isEmpty(helmChartPaths)) {
            final AtomicInteger priorityTemp = new AtomicInteger(priority);
            for (Path helmPath : helmChartPaths) {
                helmCharts.computeIfAbsent(helmPath, k -> createChartUrlEntity(priorityTemp.getAndIncrement(), CNF, null));
            }
        }
        LOGGER.info("Completed adding Helm charts: {} with priority: {}", helmCharts, priority);
    }

    private static ChartUrlsEntity createChartUrlEntity(int priority, ChartUrlsEntity.ChartTypeEnum chartType, String chartKey) {
        LOGGER.info("Started creating ChartUrlEntity with priority: {}, chartType: {}, chartKey: {}", priority, chartType, chartKey);
        ChartUrlsEntity chartUrlsEntity = new ChartUrlsEntity();
        chartUrlsEntity.setPriority(priority);
        chartUrlsEntity.setChartType(chartType);
        chartUrlsEntity.setChartArtifactKey(chartKey);
        LOGGER.info("Completed creating ChartUrlEntity with priority: {}, chartType: {}, chartKey: {}", priority, chartType, chartKey);
        return chartUrlsEntity;
    }

    public void handleHelmUploadResponse(Map<Path, ResponseEntity<String>> chartsUploadResponse, String packageId,
                                           Map<Path, HelmChartStatus> helmChartStatus) {
        boolean isHelmUploadFailed = false;
        StringBuilder error = new StringBuilder();
        Map<Path, ResponseEntity<String>> failedCharts = getFailedCharts(chartsUploadResponse);
        if (!failedCharts.isEmpty()) {
            LOGGER.error("Failed to upload Helm Chart due to following {}", failedCharts.values());
            isHelmUploadFailed = true;
            error.append(updatePackageStateFailed(packageId, List.copyOf(failedCharts.values())));
            failureService.cleanupChartInFailure(helmChartStatus);
        }
        if (helmChartStatus.values().stream()
                .anyMatch(chartStatus -> (HelmChartStatus.PRESENT_BUT_CONTENT_NOT_MATCHING == chartStatus))) {
            isHelmUploadFailed = true;
            final String errorMessage = getErrorMessage(helmChartStatus);
            error.append(errorMessage);
            persistErrorInformation(errorMessage, packageId);
        }

        if (isHelmUploadFailed) {
            LOGGER.info("Helm charts have been uploaded for package {}", packageId);
            throw new ChartOnboardingException(error.toString());
        }
    }

    private Map<Path, ResponseEntity<String>> getFailedCharts(Map<Path, ResponseEntity<String>> uploadHelmChart) {
        if (uploadHelmChart.values().stream().anyMatch(response -> response.getStatusCode().isError())) {
            Map<Path, ResponseEntity<String>> non409ConflictErrors = uploadHelmChart.entrySet().stream()
                    .filter(chart -> HttpStatus.CONFLICT != chart.getValue().getStatusCode())
                    .filter(chart -> chart.getValue().getStatusCode().isError())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            Map<Path, ResponseEntity<String>> conflictErrorsNotUploaded = uploadHelmChart.entrySet().stream()
                    .filter(chart -> HttpStatus.CONFLICT == chart.getValue().getStatusCode())
                    .filter(chart -> HelmChartStatus.PRESENT != helmService.checkChartPresent(chart.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            Map<Path, ResponseEntity<String>> result = new HashMap<>();
            result.putAll(non409ConflictErrors);
            result.putAll(conflictErrorsNotUploaded);

            return result;
        }
        return Collections.emptyMap();
    }

    private void persistErrorInformation(String message, String packageId) {
        if (!StringUtils.isEmpty(packageId)) {
            AppPackage appPackage = updateErrorPackageState(packageId);
            appPackage.setErrorDetails(message);
            appPackageRepository.save(appPackage);
        }
        LOGGER.error("Message : {}, ErrorCode: {}", message, HttpStatus.CONFLICT.value());
    }

    private AppPackage updateErrorPackageState(String packageId) {
        AppPackage appPackage = databaseService.getAppPackageById(packageId);
        return appPackageRepository.save(OnboardingPackageState.ERROR_NOT_IN_USE_DISABLED.setPackageState(appPackage));
    }

    private static String getErrorMessage(final Map<Path, HelmChartStatus> helmChartStatus) {
        StringBuilder message = new StringBuilder();

        helmChartStatus
                .entrySet()
                .stream()
                .filter(chartStatus -> (HelmChartStatus.PRESENT_BUT_CONTENT_NOT_MATCHING == chartStatus.getValue()))
                .forEach(chart -> message.append(String.format(Constants.HELM_CHART_ALREADY_PRESENT_MESSAGE, chart
                        .getKey()
                        .getFileName())));
        return message.toString();
    }

    private String updatePackageStateFailed(String packageId, List<ResponseEntity<String>> errorResponses) {
        StringBuilder errorMessage = new StringBuilder();
        if (!StringUtils.isEmpty(packageId)) {
            AppPackage tempPackage = updateErrorPackageState(packageId);
            errorResponses.stream()
                    .map(HttpEntity::getBody)
                    .filter(Objects::nonNull)
                    .forEach(errorMessage::append);
            tempPackage.setErrorDetails(errorMessage.toString());
            appPackageRepository.save(tempPackage);

            return errorMessage.toString();
        }

        return StringUtils.EMPTY;
    }

    private void saveChartDetails(final String packageId, final Map<Path, ChartUrlsEntity> helmCharts, final byte[] helmfileData) {
        LOGGER.info("Started saving helm charts and helmfile content if exist. Charts: {}.", helmCharts);

        updateChartUrlAndHelmfile(packageId, helmCharts, helmfileData);

        LOGGER.info("Completed saving chart details: {}", helmCharts);
    }

    private void updateChartUrlAndHelmfile(final String pkgId, final Map<Path, ChartUrlsEntity> helmCharts, final byte[] helmfile) {
        if (StringUtils.isEmpty(pkgId) || (helmCharts.isEmpty() && helmfile == null)) {
            return;
        }

        AppPackage appPackage = databaseService.getAppPackageById(pkgId);

        if (!CollectionUtils.isEmpty(helmCharts)) {
            List<ChartUrlsEntity> updatedHelmCharts = getUpdatedHelmCharts(helmCharts);
            updatedHelmCharts.forEach(chartEntity -> chartEntity.setAppPackage(appPackage));
            updatedHelmCharts.sort(CHART_URLS_COMPARATOR);

            appPackage.setChartsRegistryUrl(updatedHelmCharts);
        }

        appPackage.setHelmfile(helmfile);

        appPackageRepository.save(appPackage);
    }

    private List<ChartUrlsEntity> getUpdatedHelmCharts(final Map<Path, ChartUrlsEntity> helmCharts) {
        List<ChartUrlsEntity> chartUrlsEntities = new ArrayList<>();
        Set<String> uniqueChartUrls = new HashSet<>();
        for (Map.Entry<Path, ChartUrlsEntity> entry : helmCharts.entrySet()) {
            if (entry.getKey() != null) {
                Path chartPath = entry.getKey();
                String chartUrl = updateChartUrl().apply(chartPath);
                if (!uniqueChartUrls.add(chartUrl)) {
                    throw new ChartOnboardingException("Duplicate chart URL");
                }
                ChartUrlsEntity chartUrlsEntity = entry.getValue();
                chartUrlsEntity.setChartsRegistryUrl(chartUrl);
                chartUrlsEntity.setChartName(HelmChartUtils.getChartYamlProperty(chartPath, "name"));
                chartUrlsEntity.setChartVersion(HelmChartUtils.getChartYamlProperty(chartPath, "version"));
                chartUrlsEntities.add(chartUrlsEntity);
            }
        }
        return chartUrlsEntities;
    }

    private Function<Path, String> updateChartUrl() {
        return chartUrl -> {
            LOGGER.info("ChartUrl is :: {} ", chartUrl);
            return helmService.getChartInstallUrl(chartUrl.getFileName().toString());
        };
    }

    private byte[] getHelmfile(final VnfDescriptorDetails details, final Path artifactPaths) {
        LOGGER.info("Getting helmfile content from vnf details");
        List<HelmChart> helmCharts = details.getHelmCharts();
        return helmCharts.stream().map(HelmChart::getPath)
                .map(artifactPaths::resolve)
                .filter(HelmChartUtils::containHelmfile)
                .map(this::createHelmfile)
                .findAny().orElse(null);
    }

    private byte[] createHelmfile(Path path) {
        return fileService.getFileContent(path);
    }
}
