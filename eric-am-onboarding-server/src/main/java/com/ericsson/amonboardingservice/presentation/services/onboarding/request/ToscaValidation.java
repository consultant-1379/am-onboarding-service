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

import com.ericsson.am.shared.vnfd.model.servicemodel.ServiceModel;
import com.ericsson.am.shared.vnfd.service.ToscaoService;
import com.ericsson.amonboardingservice.presentation.exceptions.ErrorMessage;
import com.ericsson.amonboardingservice.presentation.exceptions.FailedOnboardingValidationException;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.services.ToscaHelper;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.ericsson.amonboardingservice.presentation.services.packageservice.OnboardingPackageState;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageDatabaseService;
import com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants;
import com.ericsson.amonboardingservice.presentation.services.vnfdservice.VnfdService;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ericsson.amonboardingservice.presentation.services.onboarding.request.OnboardingPhase.TOSCAO_VALIDATION_PHASE;

/**
 * If the package is an ETSI compliant package, i.e. has a VNFD, then parse and validate the VNFD
 */
@Slf4j
@Component
public class ToscaValidation implements RequestHandler {
    @Value("${onboarding.skipToscaoValidation}")
    private boolean isToscaoDisabled;

    @Autowired
    private VnfdService vnfdService;
    @Autowired
    private ToscaoService toscaoService;
    @Autowired
    private ToscaHelper toscaHelper;
    @Autowired
    private AppPackageRepository appPackageRepository;
    @Autowired
    private PackageDatabaseService databaseService;
    @Autowired
    private FileService fileService;

    @Override
    public String getName() {
        return TOSCAO_VALIDATION_PHASE.toString();
    }

    @Override
    public void handle(PackageUploadRequestContext currentContext) {
        String packageId = currentContext.getPackageId();
        AppPackage appPackage = updatePackageState(packageId);
        boolean etsiPackage = toscaHelper.isEtsiPackage(currentContext);
        if (!etsiPackage) {
            LOGGER.warn("Package type by ID {} is not ETSI. Exiting Tosca-O validation stage.",
                    currentContext.getPackageId());

            LOGGER.info("Package type is NOT ETSI");
            appPackage.setDescriptorId(String.valueOf(Instant.now().getEpochSecond()));
            return;
        }

        LOGGER.info("Package type is ETSI");
        Map<String, Path> artifactPaths = currentContext.getArtifactPaths();
        appPackage.setVnfdZip(getVnfdZipByte(artifactPaths));
        appPackageRepository.save(appPackage);

        if (isToscaoDisabled) {
            LOGGER.info("Tosca-O is disabled.");
            return;
        }

        Path descriptorPath = artifactPaths.get(ToscaMetaConstants.ENTRY_DEFINITIONS);

        // legacy Tosca 1.2 packages are not validated with Tosca O
        // since they might not be fully compatible with the ETSI standard
        JSONObject vnfDescriptor = vnfdService.getVnfDescriptor(descriptorPath);
        LOGGER.info("Tosca-O validation: Started validation of package with Tosca-O");
        boolean needToValidateWithTosca = !toscaHelper.isTosca1Dot2(vnfDescriptor);

        if (needToValidateWithTosca) {
            ServiceModel serviceModel = currentContext.getServiceModel();
            if (serviceModel != null) {
                LOGGER.info("Tosca O validation is skipped bacause Service Model {} is already set to context",
                        serviceModel);
                return;
            }

            String vnfdId = vnfdService.getDescriptorId(vnfDescriptor);
            Optional<ServiceModel> serviceModelOptional = toscaoService.getServiceModelByDescriptorId(vnfdId);
            serviceModelOptional.ifPresentOrElse(model -> {
                LOGGER.info("Tosca-O validation: ServiceModel by vnfDescriptorId {} already exists in Tosca-O. Setting ServiceModel to context and "
                        + "skipping further varifications", vnfdId);
                currentContext.setServiceModel(serviceModelOptional.get());
            }, () -> {
                    LOGGER.info("Tosca-O validation: Starts uploading package with ID {}...", currentContext.getPackageId());
                    toscaHelper.uploadPackageToToscao(currentContext);
                    LOGGER.info("Tosca-O validation: completed");
                });

            List<ErrorMessage> errors = currentContext.getErrors();
            if (!errors.isEmpty()) {
                LOGGER.info("Tosca-O handler : Validation of package with TOSCAO is failed because of errors {}", errors);
                throw new FailedOnboardingValidationException(errors);
            }
        }

        LOGGER.info("Tosca-O handler : finished Tosca-O part");
    }

    private AppPackage updatePackageState(String packageId) {
        LOGGER.info("Started updating package state with {} for package with id: {}",
                OnboardingPackageState.PROCESSING_NOT_IN_USE_DISABLED,
                packageId);
        AppPackage appPackage = databaseService.getAppPackageById(packageId);
        AppPackage updatedPackage = appPackageRepository.save(OnboardingPackageState.PROCESSING_NOT_IN_USE_DISABLED.setPackageState(appPackage));
        LOGGER.info("Completed updating package state with {} for package with id: {}",
                OnboardingPackageState.PROCESSING_NOT_IN_USE_DISABLED, packageId);
        return updatedPackage;
    }


    private byte[] getVnfdZipByte(Map<String, Path> artifactPaths) {
        Path vnfdDirectory = artifactPaths.get(ToscaMetaConstants.ENTRY_DEFINITIONS).getParent();
        Path zipDirectory = null;
        try {
            zipDirectory = fileService.zipDirectory(vnfdDirectory);
            return Files.readAllBytes(zipDirectory);
        } catch (IOException ioe) {
            String message = String.format("Unable to create VNFD zip due to :: %s", ioe.getMessage());
            LOGGER.error(message, ioe);
            throw new FailedOnboardingValidationException("Unable to create VNFD zip file", ioe);
        } finally {
            if (zipDirectory != null) {
                fileService.deleteFile(zipDirectory.toFile());
            }
        }
    }
}