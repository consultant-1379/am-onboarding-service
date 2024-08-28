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

import static com.ericsson.amonboardingservice.presentation.services.onboarding.request.OnboardingPhase.IMAGES_ONBOARDING_PHASE;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_IMAGES;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.ericsson.am.shared.vnfd.model.ImageDetails;
import com.ericsson.am.shared.vnfd.model.VnfDescriptorDetails;
import com.ericsson.amonboardingservice.presentation.exceptions.ImageOnboardingException;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppUserDefinedData;
import com.ericsson.amonboardingservice.presentation.services.dockerservice.DockerService;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageDatabaseService;
import com.ericsson.amonboardingservice.utils.Constants;

import lombok.extern.slf4j.Slf4j;

/**
 * Find the tar file of docker images in the package and push them to the container registry
 */
@Slf4j
@Component
public class OnboardImages implements RequestHandler {

    @Autowired
    private PackageDatabaseService databaseService;

    @Autowired
    private DockerService dockerService;

    @Value("${container.registry.enabled}")
    private boolean isContainerRegistryEnabled;

    @Override
    public void handle(final PackageUploadRequestContext context) {
        LOGGER.info("Starting OnboardImages onboarding handler");

        boolean isPackageImageless = isPackageImageless(context.getPackageId(), isContainerRegistryEnabled);

        if (!isPackageImageless) {

            List<Path> dockerTarsToUpload = getDockerTarsToUpload(context);
            LOGGER.info("These Docker tars were found: {}. Size is: {}", dockerTarsToUpload, dockerTarsToUpload.size());

            List<String> renamedImageList = new ArrayList<>();

            for (final Path dockerTar : dockerTarsToUpload) {
                List<String> onboardedImages = dockerService.onboardDockerTar(dockerTar, context.getTimeoutDate());
                LOGGER.info("Onboarded images {}. Size is: {}", onboardedImages, onboardedImages.size());
                renamedImageList.addAll(onboardedImages);
            }

            context.setRenamedImageList(renamedImageList);
        }

        LOGGER.info("Successfully finished OnboardImages onboarding handler");
    }

    @Override
    public String getName() {
        return IMAGES_ONBOARDING_PHASE.toString();
    }

    protected boolean isPackageImageless(final String packageId, final boolean isContainerRegistryEnabled) {
        LOGGER.info("Started checking whether it's needed to skip image upload for appPackage ID: {}", packageId);

        boolean isSkipImagesUpload;

        AppPackage appPackage = databaseService.getAppPackageById(packageId);
        List<AppUserDefinedData> allUSerDefinedData = Optional.ofNullable(appPackage.getUserDefinedData())
                .orElseGet(() -> {
                    var userDefinedData = new ArrayList<AppUserDefinedData>();
                    appPackage.setUserDefinedData(userDefinedData);
                    return userDefinedData;
                });

        AppUserDefinedData userDefinedData = allUSerDefinedData
                .stream()
                .filter(definedData -> Constants.SKIP_IMAGE_UPLOAD.equalsIgnoreCase(definedData.getKey()))
                .findFirst()
                .orElseGet(() -> {
                    AppUserDefinedData defaultData = defaultSkipImageUpload(appPackage);
                    allUSerDefinedData.add(defaultData);
                    return defaultData;
                });

        isSkipImagesUpload = updateIsSkipImagesRegardingContainerRegistry(appPackage, userDefinedData, isContainerRegistryEnabled);

        LOGGER.info("Completed checking whether it's needed to skip image upload for appPackage ID: {}. Result is: {}",
                    packageId, isSkipImagesUpload);
        return isSkipImagesUpload;
    }

    private List<Path> getDockerTarsToUpload(PackageUploadRequestContext context) {
        LOGGER.info("Started getting Docker tars to upload package with ID {}", context.getPackageId());
        List<Path> dockerTarsToUpload = new ArrayList<>();

        Map<String, Path> artifactPaths = context.getArtifactPaths();
        Path csarDirectoryPath = artifactPaths.get(Constants.CSAR_DIRECTORY);
        VnfDescriptorDetails vnfDescriptorDetails = context.getVnfd();

        if (context.isEtsiPackage() && !CollectionUtils.isEmpty(vnfDescriptorDetails.getImagesDetails())) {
            LOGGER.info("ETSI package identified");
            for (final ImageDetails imagesDetail : vnfDescriptorDetails.getImagesDetails()) {

                String imagesDetailPath = imagesDetail.getPath();
                LOGGER.info("Starts resolving directory {}", imagesDetailPath);
                Path dockerTar = csarDirectoryPath.resolve(imagesDetailPath);
                checkDockerPathExists(dockerTar);
                LOGGER.info("Found image tar using path {}", imagesDetailPath);

                dockerTarsToUpload.add(dockerTar);
            }
        } else {
            LOGGER.info("Not an ETSI package. Looking for tar specified in Entry-Images");

            Path dockerTar = artifactPaths.get(ENTRY_IMAGES);
            checkDockerPathExists(dockerTar);
            dockerTarsToUpload.add(dockerTar);
        }

        LOGGER.info("Completed getting Docker tars to upload");
        return dockerTarsToUpload;
    }

    private static void checkDockerPathExists(Path dockerPath) {
        if (!dockerPath.toFile().exists()) {
            throw new ImageOnboardingException(String.format(Constants.DOCKER_IMAGE_NOT_PRESENT_ERROR_MESSAGE, dockerPath));
        }
    }

    protected boolean updateIsSkipImagesRegardingContainerRegistry(AppPackage appPackage,
                                                                   AppUserDefinedData userDefinedData,
                                                                   boolean isContainerRegistryEnabled) {
        if (!isContainerRegistryEnabled) {
            userDefinedData.setValue("true");
            databaseService.save(appPackage);
            LOGGER.info("Inverting skipImagesUpload parameter of package {}, due to disabled Container Registry", appPackage.getPackageId());
        }
        return Boolean.parseBoolean(userDefinedData.getValue());
    }

    private static AppUserDefinedData defaultSkipImageUpload(AppPackage appPackage) {
        return new AppUserDefinedData().setKey(Constants.SKIP_IMAGE_UPLOAD).setValue("false").setAppPackages(appPackage);
    }
}
