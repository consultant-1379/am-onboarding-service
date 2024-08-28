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

import com.ericsson.am.shared.vnfd.model.servicemodel.ServiceModel;
import com.ericsson.am.shared.vnfd.service.ToscaoService;
import com.ericsson.am.shared.vnfd.service.exception.ToscaoException;
import com.ericsson.amonboardingservice.presentation.exceptions.ErrorMessage;
import com.ericsson.amonboardingservice.presentation.exceptions.FailedOnboardingValidationException;
import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.presentation.models.ServiceModelRecordEntity;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.ericsson.amonboardingservice.presentation.services.onboarding.request.PackageUploadRequestContext;
import com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants;
import com.ericsson.amonboardingservice.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.String.format;

@Slf4j
@Component
public class ToscaHelper {
    private static final Pattern TEMPLATE_BASE_FILE = Pattern.compile("^template base file$");
    @Value("${onboarding.skipToscaoValidation}")
    private boolean isToscaoDisabled;

    @Autowired
    private FileService fileService;

    @Autowired
    private ToscaoService toscaoService;

    @Autowired
    private AppPackageRepository appPackageRepository;

    public String getToscaDefinitionsVersion(final JSONObject vnfDescriptor) {
        Objects.requireNonNull(vnfDescriptor, Constants.VNF_DESCRIPTOR_MUST_NOT_BE_NULL);
        if (vnfDescriptor.has(Constants.TOSCA_DEFINITIONS_VERSION)) {
            return vnfDescriptor.getString(Constants.TOSCA_DEFINITIONS_VERSION);
        } else {
            throw new IllegalArgumentException(Constants.VNF_DESCRIPTOR_DOES_NOT_CONTAIN_DEFINITIONS_VERSION_FIELD);
        }
    }

    public boolean isTosca1Dot2(JSONObject vnfDescriptor) {
        return getToscaDefinitionsVersion(vnfDescriptor).equals(Constants.TOSCA_1_2_DEFINITIONS_VERSION);
    }

    public void uploadPackageToToscao(PackageUploadRequestContext context) {
        LOGGER.info("Started uploading VNF package to TOSCAO");
        Path packagePath = context.getArtifactPaths().get(Constants.PATH_TO_PACKAGE);
        Path repackedCSAR = createCsarWithoutImagesAndCharts(packagePath);

        try {
            LOGGER.info("Uploading package to TOSCAO service from {} with package path {}", repackedCSAR, packagePath);
            ServiceModel serviceModel = toscaoService.uploadPackageToToscao(repackedCSAR)
                .orElseThrow(() -> new InternalRuntimeException(format("Failed to upload Vnf package %s to Tosca O", packagePath)));
            context.setServiceModel(serviceModel);
            LOGGER.info("Completed uploading VNF package from {} to TOSCAO", repackedCSAR);
        } catch (ToscaoException e) {
            LOGGER.warn("Failed to upload VNF package", e);
            context.addErrors(new ErrorMessage(format("Failed to upload VNF package from %s to TOSCAO. Error message: %s", repackedCSAR,
                                                      e.getMessage())));
        } finally {
            fileService.deleteFile(repackedCSAR.toFile());
        }
    }

    public boolean isEtsiPackage(PackageUploadRequestContext context) {
        Map<String, Path> artifactPaths = context.getArtifactPaths();
        boolean etsiPackage = isEtsiPackage(artifactPaths);
        context.setEtsiPackage(etsiPackage);
        return etsiPackage;
    }

    private boolean isEtsiPackage(Map<String, Path> paths) {
        LOGGER.info("Started checking whether package is ETSI");
        if (paths.containsKey(ToscaMetaConstants.ENTRY_DEFINITIONS)) {
            LOGGER.info("Package contains Entry Definitions entry");
            Path vnfdPath = paths.get(ToscaMetaConstants.ENTRY_DEFINITIONS);
            LOGGER.info("Package path VNFD is {}", vnfdPath);
            try (Stream<String> lines = Files.lines(vnfdPath)) {
                boolean contentsAreAutogenerated = lines.noneMatch(line -> TEMPLATE_BASE_FILE
                    .matcher(line)
                    .matches());
                LOGGER.info("Contents are autogenerated {}", contentsAreAutogenerated);
                return contentsAreAutogenerated;
            } catch (IOException e) {
                throw new FailedOnboardingValidationException(String.format("Failed to check whether package is ETSI. VNFD is not readable at "
                                                                                + "path %s", vnfdPath), e);
            }
        } else {
            LOGGER.info("Completed checking whether package is ETSI. Package is not ETSI");
            return false;
        }
    }

    public void deleteServiceModelFromToscaoByAppPackage(String packageSetForDetetionId) {
        Optional<AppPackage> appPackageForDeletion = appPackageRepository.findByPackageId(packageSetForDetetionId);
        appPackageForDeletion.ifPresent(appPackage -> {
            ServiceModelRecordEntity serviceModelRecordEntity = appPackage.getServiceModelRecordEntity();
            if (serviceModelRecordEntity != null) {
                optionallyDeleteServiceModel(serviceModelRecordEntity.getServiceModelId());
            }
        });
    }

    public void optionallyDeleteServiceModel(String serviceModelId) {
        if (!isToscaoDisabled) {
            deleteServiceModel(serviceModelId);
        }
    }

    private void deleteServiceModel(String serviceModelId) {
        try {
            toscaoService.deleteServiceModel(serviceModelId);
        } catch (ToscaoException e) {
            String errorMessage = String.format("Failed to delete service model %s from Tosca O", serviceModelId);
            LOGGER.error(errorMessage, e);
            throw new InternalRuntimeException(errorMessage, e);
        }
    }
    private Path createCsarWithoutImagesAndCharts(Path packagePath) {
        Path newZipFile = Paths.get(packagePath.getParent().toString(), "imagelessAndChartlessPackage.csar");
        try {
            fileService.repackCsarWithoutImagesAndCharts(packagePath.getParent(), newZipFile);
        } catch (IOException e) {
            LOGGER.error("Could not create CSAR without images and charts");
            throw new FailedOnboardingValidationException(format("Could not create CSAR without images and charts with package path: %s",
                                                                 packagePath), e);
        }
        return newZipFile;
    }
}
