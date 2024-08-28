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

import static com.ericsson.amonboardingservice.presentation.services.onboarding.request.OnboardingPhase.PERSIST_PHASE;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_DEFINITIONS;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_HELM_DEFINITIONS;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_IMAGES;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.ericsson.am.shared.vnfd.model.VnfDescriptorDetails;
import com.ericsson.amonboardingservice.presentation.exceptions.FailedOnboardingException;
import com.ericsson.amonboardingservice.presentation.exceptions.UserInputException;
import com.ericsson.amonboardingservice.presentation.models.Chart;
import com.ericsson.amonboardingservice.presentation.models.ServiceModelRecordEntity;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackageArtifacts;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackageDockerImage;
import com.ericsson.amonboardingservice.presentation.models.vnfd.OperationDetailEntity;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.services.ToscaHelper;
import com.ericsson.amonboardingservice.presentation.services.auditservice.AuditService;
import com.ericsson.amonboardingservice.presentation.services.packageservice.OnboardingPackageState;
import com.ericsson.amonboardingservice.presentation.services.servicemodelservice.ServiceModelService;
import com.ericsson.amonboardingservice.presentation.services.supportedoperationservice.SupportedOperationService;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageDatabaseService;
import com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaService;
import com.ericsson.amonboardingservice.presentation.services.vnfdservice.VnfdService;
import com.ericsson.amonboardingservice.utils.Constants;

import lombok.extern.slf4j.Slf4j;

/**
 * Persist the AppPackage information to the database
 */
@Slf4j
@Component
public class Persist implements RequestHandler {
    private static final String SERVICE_MODEL_ABSENT_IN_CONTEXT_FOR_TOSCA_1_3_ERROR_MESSAGE = "TOSCAO validation enabled and ServiceModel must be "
            + "present in context for package with TOSCA 1.3 VNFD";
    private static final String SERVICE_MODEL_ABSENT_IN_CONTEXT_FOR_TOSCA_1_2_WARN_MESSAGE =
            "TOSCAO validation enabled and ServiceModel is not present in context. "
                    + "Probably TOSCAO validation failed for package with TOSCA 1.2 VNFD.";

    @Autowired
    private AppPackageRepository appPackageRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private ToscaMetaService toscaMetaService;

    @Autowired
    private ServiceModelService serviceModelService;

    @Autowired
    private VnfdService vnfdService;

    @Autowired
    private ToscaHelper toscaHelper;

    @Autowired
    private SupportedOperationService supportedOperationService;

    @Autowired
    private PackageDatabaseService databaseService;

    @Value("${onboarding.skipToscaoValidation}")
    private boolean isToscaoDisabled;

    @Transactional
    @Override
    public void handle(final PackageUploadRequestContext context) {
        LOGGER.info("Starting Persist onboarding handler");

        AppPackage appPackage = databaseService.getAppPackageById(context.getPackageId());

        Path packageContents = context.getPackageContents();
        LOGGER.info("Resolved package contents {}", packageContents);

        saveChartDetails(packageContents, context.getHelmChartPaths());

        appPackage.setChecksum(context.getChecksum());
        appPackage.setPackageSecurityOption(context.getPackageSecurityOption());
        setVnfdDetailsToAppPackage(context.getVnfd(), appPackage);
        appPackage = appPackageRepository.saveAndFlush(appPackage);

        setPackageArtifacts(appPackage, context.getArtifactPaths(), packageContents.getParent());
        setAppPackageDockerImages(appPackage, context.getRenamedImageList());
        OnboardingPackageState.ONBOARDED_NOT_IN_USE_ENABLED.setPackageState(appPackage);
        LOGGER.info("Update package status {}", OnboardingPackageState.ONBOARDED_NOT_IN_USE_ENABLED);
        setSupportedOperations(appPackage);
        appPackage.setErrorDetails(null);
        appPackage = appPackageRepository.save(appPackage);

        saveServiceModelFromContext(context, appPackage);

        LOGGER.info("Successfully finished Persist onboarding handler");
    }

    @Override
    public String getName() {
        return PERSIST_PHASE.toString();
    }

    private void saveChartDetails(final Path packageContents, final Set<Path> chartPaths) {
        for (final Path chartPath : chartPaths) {
            if (chartPath == null) {
                continue;
            }

            LOGGER.info("Save chart detail {}", chartPath);

            Chart chart = auditService.saveChartDetails(chartPath, packageContents);
            auditService.saveChartAuditDetails(chart);
        }
    }

    private void setVnfdDetailsToAppPackage(VnfDescriptorDetails vnfDescriptorDetails, AppPackage appPackage) {
        LOGGER.info("Started setting details from VnfDescriptorDetails: {} to app package: {}",
                    vnfDescriptorDetails.getVnfDescriptorId(),
                    appPackage.getPackageId());

        appPackage.setDescriptorId(vnfDescriptorDetails.getVnfDescriptorId());
        appPackage.setDescriptorVersion(vnfDescriptorDetails.getVnfDescriptorVersion());
        appPackage.setProvider(vnfDescriptorDetails.getVnfProvider());
        appPackage.setProductName(vnfDescriptorDetails.getVnfProductName());
        appPackage.setSoftwareVersion(vnfDescriptorDetails.getVnfSoftwareVersion());
        appPackage.setDescriptorModel(vnfDescriptorDetails.getDescriptorModel());

        if (!CollectionUtils.isEmpty(vnfDescriptorDetails.getFlavours())) {
            appPackage.setMultipleVnfd(true);
        }

        LOGGER.info("Completed setting VnfDescriptorDetails: {} to package: {}",
                    vnfDescriptorDetails.getVnfDescriptorId(),
                    appPackage.getPackageId());
    }

    public void setPackageArtifacts(AppPackage appPackage, final Map<String, Path> artifactPathsMap, Path packageContents) {
        LOGGER.info("Started setting package artifacts");

        appPackage.getAppPackageArtifacts().addAll(collectAllPackageArtifacts(artifactPathsMap, packageContents, appPackage));

        LOGGER.info("Completed setting package artifacts");
    }

    private List<AppPackageArtifacts> collectAllPackageArtifacts(final Map<String, Path> artifactPaths,
                                                                 Path packageContents, AppPackage appPackage) {
        LOGGER.info("Started collecting all package: {} artifacts from artifactPaths", appPackage.getPackageId());
        Set<Path> artifactsToSave = artifactPaths.entrySet().stream()
                .filter(filterFilesToSave())
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
        collectDefinitionFiles(artifactsToSave, artifactPaths);
        artifactsToSave.add(toscaMetaService.getToscaMetaPath(packageContents));

        List<AppPackageArtifacts> packageArtifacts = artifactsToSave.stream()
                .map(file -> getPackageArtifactFromPath(packageContents, file, appPackage))
                .collect(toList());
        LOGGER.info("Completed collecting all package artifacts from artifactPaths");
        return packageArtifacts;
    }

    private static Predicate<Map.Entry<String, Path>> filterFilesToSave() {
        return file -> Files.isRegularFile(file.getValue()) && !file.getKey().equals(ENTRY_IMAGES) && !file.getKey().equals(ENTRY_HELM_DEFINITIONS);
    }

    private static void collectDefinitionFiles(Set<Path> artifactsToSave, Map<String, Path> artifactPaths) {
        LOGGER.info("Started collecting Definitions files from: {} to: {}", artifactPaths, artifactsToSave);
        Path vnfDescriptorParentPath = artifactPaths.get(ENTRY_DEFINITIONS).getParent();
        List<Path> entryHelmDefinitionsPaths = artifactPaths.entrySet().stream()
                .filter(file -> file.getKey().equals(ENTRY_HELM_DEFINITIONS)).map(Map.Entry::getValue).collect(Collectors.toList());
        try (Stream<Path> stream = Files.walk(vnfDescriptorParentPath)) {
            List<Path> definitionFiles = stream.filter(Files::isRegularFile)
                    .filter(path -> FilenameUtils.isExtension(path.toString().toLowerCase(), Arrays.asList("yaml", "yml")))
                    .filter(path -> isYamlInEntryHelmDefinitions(entryHelmDefinitionsPaths, path))
                    .collect(Collectors.toList());
            artifactsToSave.addAll(definitionFiles);
        } catch (IOException e) {
            LOGGER.error("Unable to walk through the Definitions : {} due to : {}", vnfDescriptorParentPath, e.getMessage());
            throw new UserInputException("Unable to walk through the Definitions due to exception: ", e);
        }
        LOGGER.info("Completed collecting Definitions files from: {} to: {}", artifactPaths, artifactsToSave);
    }

    private static boolean isYamlInEntryHelmDefinitions(List<Path> entryHelmDefinitionsPaths, Path path) {
        boolean isEntryHelmDefinitionsPath = entryHelmDefinitionsPaths.stream()
                .anyMatch(entryHelmDefinitionsPath -> path.toString().contains(entryHelmDefinitionsPath.toString()));
        if (isEntryHelmDefinitionsPath && !path.toString().contains(Constants.SCALING_MAPPING)) {
            return FilenameUtils.isExtension(path.toString().toLowerCase(), Arrays.asList("yaml", "yml"));
        }
        return true;
    }

    private static AppPackageArtifacts getPackageArtifactFromPath(Path parentPath, Path artifactPath, AppPackage appPackage) {
        Path artifactRelativePath = parentPath.relativize(artifactPath);
        AppPackageArtifacts appPackageArtifacts = new AppPackageArtifacts();
        appPackageArtifacts.setAppPackage(appPackage);
        appPackageArtifacts.setArtifactPath(artifactRelativePath.toString());
        try {
            appPackageArtifacts.setArtifact(IOUtils.toByteArray(artifactPath.toUri()));
        } catch (IOException e) {
            LOGGER.error("Unable to get the artifact : {} due to : {} ", artifactPath, e.getMessage(), e);
            throw new UserInputException("Unable to get the artifact for appPackage due to exception: ", e);
        }
        return appPackageArtifacts;
    }

    private void setAppPackageDockerImages(AppPackage appPackage, List<String> renamedImageList) {
        LOGGER.info("Started setting Docker images: {} to app package", renamedImageList);

        List<AppPackageDockerImage> dockerImages = renamedImageList.stream()
                .map(image -> createImageDetails(image, appPackage))
                .collect(toList());

        appPackage.setAppPackageDockerImages(dockerImages);

        LOGGER.info("Completed setting Docker images: {} to app package", renamedImageList);
    }

    private static AppPackageDockerImage createImageDetails(final String image, final AppPackage appPackage) {
        LOGGER.info("Started creating image details with image: {} and app package", image);
        AppPackageDockerImage appPackageDockerImage = new AppPackageDockerImage();
        appPackageDockerImage.setAppPackage(appPackage);
        appPackageDockerImage.setImageId(image);
        LOGGER.info("Completed creating image details with image: {} and app package", image);
        return appPackageDockerImage;
    }

    private void setSupportedOperations(final AppPackage appPackage) {
        LOGGER.info("Started setting supported operations");

        appPackage.getOperationDetails().addAll(parseSupportedOperations(appPackage));

        LOGGER.info("Completed setting supported operations");
    }

    private List<OperationDetailEntity> parseSupportedOperations(AppPackage appPackage) {
        LOGGER.info("Run Parsing operation details has been started: ");
        try {
            return supportedOperationService.parsePackageSupportedOperations(appPackage);
        } catch (RuntimeException e) {
            LOGGER.error("Parsing operation details has failed: ", e);
            throw new FailedOnboardingException(String.format("Onboarding package has failed due to %s", e.getMessage()));
        }
    }

    private void saveServiceModelFromContext(final PackageUploadRequestContext context, final AppPackage appPackage) {
        LOGGER.info("Started saving service model from context: {}", context);

        JSONObject vnfDescriptor = vnfdService.getVnfDescriptor(context.getArtifactPaths().get(ENTRY_DEFINITIONS));
        boolean isTosca1Dot2 = toscaHelper.isTosca1Dot2(vnfDescriptor);

        if (!isToscaoDisabled) {
            if (context.getServiceModel() == null && isTosca1Dot2) {
                LOGGER.warn(SERVICE_MODEL_ABSENT_IN_CONTEXT_FOR_TOSCA_1_2_WARN_MESSAGE);
            }

            if (context.getServiceModel() == null && !isTosca1Dot2) {
                LOGGER.error(SERVICE_MODEL_ABSENT_IN_CONTEXT_FOR_TOSCA_1_3_ERROR_MESSAGE);
                throw new FailedOnboardingException(SERVICE_MODEL_ABSENT_IN_CONTEXT_FOR_TOSCA_1_3_ERROR_MESSAGE);
            }

            Optional<ServiceModelRecordEntity> serviceModelRecordEntity = serviceModelService.saveServiceModelFromRequestContext(context, appPackage);
            if (serviceModelRecordEntity.isPresent()) {
                LOGGER.info("Starts to save service model from context");
                appPackage.setServiceModelRecordEntity(serviceModelRecordEntity.get());
                appPackageRepository.save(appPackage);
            }
        }
        LOGGER.info("Completed saving service model from context: {}", context);
    }
}
