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
package com.ericsson.amonboardingservice.presentation.controllers.internal;

import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_DEFINITIONS;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.am.shared.vnfd.model.HelmChart;
import com.ericsson.am.shared.vnfd.model.VnfDescriptorDetails;
import com.ericsson.amonboardingservice.api.InternalPackageControllerApi;
import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.presentation.models.ChartUrlsEntity;
import com.ericsson.amonboardingservice.presentation.models.OnboardingResponseLinks;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.services.ToscaHelper;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.ericsson.amonboardingservice.presentation.services.helmservice.HelmService;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageService;
import com.ericsson.amonboardingservice.presentation.services.vnfdservice.VnfdService;
import com.ericsson.amonboardingservice.utils.HelmChartUtils;
import com.ericsson.amonboardingservice.utils.LinksUtility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class InternalPackageControllerImpl implements InternalPackageControllerApi {
    @Autowired
    private FileService fileService;
    @Autowired
    private PackageService packageService;
    @Autowired
    private VnfdService vnfdService;
    @Autowired
    private ToscaHelper toscaHelper;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private HelmService helmService;

    @Override
    public ResponseEntity<String> v1PackagesPost(final String helmChartURI, final MultipartFile file) {
        Path savedFile;
        try {
            savedFile = storeYamlFile(file);
        } catch (IOException e) {
            LOGGER.error("An error occurred during yaml storing:", e);
            return new ResponseEntity<>(
                "Failed to upload vnfd file successfully\nCheck application logs for details",
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        JSONObject vnfd = vnfdService.getVnfDescriptor(savedFile);

        if (!toscaHelper.isTosca1Dot2(vnfd)) {
            return new ResponseEntity<>(
                "Failed to upload vnfd file because 1.3 TOSCA VNFD is not supported",
                HttpStatus.BAD_REQUEST);
        }

        Map<String, Path> artifactPaths = new HashMap<>();
        artifactPaths.put(ENTRY_DEFINITIONS, savedFile);
        Pair<String, String> chartNameAndVersion = getChartNameAndVersion(helmChartURI);
        VnfDescriptorDetails vnfDescriptorDetails = vnfdService.validateAndGetVnfDescriptorDetails(artifactPaths);
        HelmChart helmChart = vnfDescriptorDetails.getHelmCharts().get(0);
        List<ChartUrlsEntity> chartUrlsEntities = getChartUrlsEntities(helmChartURI,
                                                                       chartNameAndVersion.getLeft(),
                                                                       chartNameAndVersion.getRight(),
                                                                       helmChart.getChartType().name(),
                                                                       helmChart.getChartKey(),
                                                                       1);
        AppPackage appPackage = packageService.savePackageDetails(vnfDescriptorDetails, chartUrlsEntities);

        fileService.deleteFile(savedFile.toFile());
        JSONObject response = new JSONObject();
        response.put("success", true).put("message", "vnfd file has been successfully onboarded");
        response.put("vnfdId", vnfDescriptorDetails.getVnfDescriptorId());
        final OnboardingResponseLinks links = LinksUtility.getOnboardingResponseLinks(appPackage.getPackageId());
        try {
            response.put("_links", mapper.writeValueAsString(links));
        } catch (JsonProcessingException e) {
            LOGGER.error("Unable to convert the java object to json. Failed with exception: ", e);
        }
        return new ResponseEntity<>(response.toString(), HttpStatus.CREATED);
    }

    private static Path storeYamlFile(MultipartFile packageContents) throws IOException {
        Path tempFileCreated;
        try {
            tempFileCreated = Files.createTempFile("evnfmvnfd", ".yaml");
            packageContents.transferTo(tempFileCreated.toFile());
        } catch (IOException e) {
            String message = String.format("Failed to store package due to: %s", e.getMessage());
            LOGGER.error(message);
            throw new InternalRuntimeException(message, e);
        }
        return tempFileCreated;
    }

    private static List<ChartUrlsEntity> getChartUrlsEntities(final String helmChartURI,
                                                              final String chartName,
                                                              final String chartVersion,
                                                              final String chartType,
                                                              final String chartKey,
                                                              int priority) {
        ChartUrlsEntity chartUrlsEntity = new ChartUrlsEntity();
        chartUrlsEntity.setChartsRegistryUrl(helmChartURI);
        chartUrlsEntity.setChartName(chartName);
        chartUrlsEntity.setChartVersion(chartVersion);
        chartUrlsEntity.setChartType(ChartUrlsEntity.ChartTypeEnum.valueOf(chartType));
        chartUrlsEntity.setChartArtifactKey(chartKey);
        chartUrlsEntity.setPriority(priority);

        List<ChartUrlsEntity> chartUrlsEntities = new ArrayList<>();
        chartUrlsEntities.add(chartUrlsEntity);
        return chartUrlsEntities;
    }

    private Pair<String, String> getChartNameAndVersion(String helmChartURI) {
        String chartFileName = helmChartURI.substring(helmChartURI.lastIndexOf("/") + 1);
        Optional<File> optionalChartFile = helmService.getChart(helmChartURI, chartFileName);
        String chartName;
        String chartVersion;
        if (optionalChartFile.isPresent()) {
            File chartFile = optionalChartFile.get();
            chartName = HelmChartUtils.getChartYamlProperty(chartFile.toPath(), "name");
            chartVersion = HelmChartUtils.getChartYamlProperty(chartFile.toPath(), "version");
            fileService.deleteFile(chartFile);
        } else {
            return HelmChartUtils.parseChartUrl(helmChartURI);
        }
        return Pair.of(chartName, chartVersion);
    }
}
