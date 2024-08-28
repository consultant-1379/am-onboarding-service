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
package com.ericsson.amonboardingservice.presentation.services.packageservice;

import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.repositories.ChartUrlsRepository;
import com.ericsson.amonboardingservice.presentation.services.ToscaHelper;
import com.ericsson.amonboardingservice.presentation.services.dockerservice.DockerService;
import com.ericsson.amonboardingservice.presentation.services.helmservice.HelmChartStatus;
import com.ericsson.amonboardingservice.presentation.services.helmservice.HelmService;
import com.ericsson.amonboardingservice.presentation.services.onboarding.request.PackageUploadRequestContext;
import com.ericsson.amonboardingservice.presentation.services.supportedoperationservice.SupportedOperationService;
import com.ericsson.amonboardingservice.utils.HelmChartUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.nio.file.Path;
import java.util.Map;

@Slf4j
@Service
public class CleanUpOnFailureService {

    @Autowired
    private AppPackageRepository appPackageRepository;
    @Autowired
    private ChartUrlsRepository chartUrlsRepository;
    @Autowired
    private HelmService helmService;
    @Autowired
    private DockerService dockerService;
    @Autowired
    private PackageDatabaseService databaseService;
    @Autowired
    private SupportedOperationService supportedOperationService;
    @Autowired
    private ToscaHelper toscaHelper;

    public void cleanUpOnFailure(final String packageId, final PackageUploadRequestContext context) {
        LOGGER.info("Starting onboarding cleanUp on failure");
        cleanUpChartAndUpdateUrl(packageId, context.getHelmChartStatus());
        cleanUpServiceModel(context);
        cleanUpSupportedOperations(packageId);
        dockerService.removeDockerImagesByPackageId(packageId);
        LOGGER.info("Finished onboarding cleanUp on failure");
    }

    public void cleanupChartInFailure(Map<Path, HelmChartStatus> helmChartStatus) {
        helmChartStatus.forEach((key, value) -> {
            //Check to verify if the chart is already present
            if (HelmChartStatus.PRESENT != value) {
                HelmChartStatus currentChartStatus = helmService.checkChartPresent(key);
                //If chart is not present check to verify if it was uploaded
                if (HelmChartStatus.PRESENT == currentChartStatus) {
                    //If the chart is uploaded then it should be deleted
                    String chartName = HelmChartUtils.getChartYamlProperty(key, "name");
                    String chartVersion = HelmChartUtils.getChartYamlProperty(key, "version");
                    helmService.deleteChart(chartName, chartVersion);
                }
            }
        });
    }

    private void cleanUpServiceModel(PackageUploadRequestContext context) {
        deleteServiceModelFromToscaOByContext(context);
        cleanupServiceModelRecordEntity(context.getPackageId());
    }

    private void cleanupServiceModelRecordEntity(String packageId) {
        if (!StringUtils.isEmpty(packageId)) {
            AppPackage appPackage = databaseService.getAppPackageById(packageId);
            appPackage.setServiceModelRecordEntity(null);
            appPackageRepository.save(appPackage);
        }
    }

    private void deleteServiceModelFromToscaOByContext(PackageUploadRequestContext context) {
        if (context.getServiceModel() != null) {
            toscaHelper.optionallyDeleteServiceModel(context.getServiceModel().getId());
        }
    }

    private void cleanUpSupportedOperations(String packageId) {
        if (!StringUtils.isEmpty(packageId)) {
            LOGGER.info("Removing supported operations for package {}", packageId);
            supportedOperationService.deleteSupportedOperations(packageId);
        }
    }

    private void cleanUpChartAndUpdateUrl(final String packageId, final Map<Path, HelmChartStatus> helmChartStatus) {
        if (!CollectionUtils.isEmpty(helmChartStatus)) {
            cleanupChartInFailure(helmChartStatus);
        }
        deleteChartsUrlByPkgId(packageId);
    }

    private void deleteChartsUrlByPkgId(final String pkgId) {
        LOGGER.info("Starting delete chart URL for package ID: {}", pkgId);
        AppPackage appPackage = databaseService.getAppPackageById(pkgId);
        chartUrlsRepository.deleteByAppPackage(appPackage);
        LOGGER.info("Chart URL for package ID: {} deleted", pkgId);
    }
}
