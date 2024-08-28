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
package com.ericsson.amonboardingservice.presentation.services.dockerservice;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.ericsson.amonboardingservice.presentation.repositories.AppPackageDockerImageRepository;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.ericsson.amonboardingservice.utils.Constants;
import com.ericsson.amonboardingservice.utils.TimeoutUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "skopeo", name = "enabled", havingValue = "true")
public class SkopeoDockerServiceImpl implements DockerService {

    private static final Pattern REGISTRY_PATTERN = Pattern.compile("(.+?(?=\\/))\\/([^\\/]+?(?=\\/))?");
    private static final String UNABLE_TO_PARSE_MANIFEST_ERROR_MESSAGE = "Unable to parse manifest file";

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private SkopeoService skopeoService;

    @Autowired
    private AppPackageDockerImageRepository appPackageDockerImageRepository;

    @Autowired
    private FileService fileService;

    @Override
    public List<String> onboardDockerTar(Path pathToTar, LocalDateTime onboardingTimeoutDate) {
        LOGGER.info("Onboard docker tar from path: {}", pathToTar);
        List<String> onboardedImages;
        int timeoutInMinutes = TimeoutUtils.resolveTimeOut(onboardingTimeoutDate, ChronoUnit.MINUTES);
        Path manifestPath = fileService.extractFileFromTar(pathToTar, pathToTar.getParent(),
                                                           Constants.MANIFEST_JSON, timeoutInMinutes);
        JSONArray manifest = readManifestFile(manifestPath);
        List<ImageObject> imageObjects;
        try {
            imageObjects = mapper.readValue(
                    manifest.toString(), mapper.getTypeFactory().constructCollectionType(List.class, ImageObject.class));
        } catch (IOException ioe) {
            LOGGER.error(UNABLE_TO_PARSE_MANIFEST_ERROR_MESSAGE);
            throw new IllegalArgumentException(UNABLE_TO_PARSE_MANIFEST_ERROR_MESSAGE, ioe);
        }

        onboardedImages = imageObjects.stream()
                .map(imageObject -> {
                    LOGGER.info("Starting processing {} Image layers", imageObject.getLayers().size());
                    return processImage(pathToTar, imageObject, onboardingTimeoutDate);
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        LOGGER.info("Successfully onboarded images: {}. Size is: {}", onboardedImages, onboardedImages.size());
        return onboardedImages;
    }

    private JSONArray readManifestFile(final Path filePath) {
        if (filePath.toFile().exists()) {
            String contents = fileService.readFile(filePath);
            LOGGER.info("Manifest file has been successfully read");
            return new JSONArray(contents);
        } else {
            final String msgErr = String.format("%s file does not exist", filePath);
            throw new IllegalArgumentException(msgErr);
        }
    }

    private List<String> processImage(Path pathToTar, ImageObject imageObject, LocalDateTime onboardingTimeout) {
        List<String> originalRepoTags = Optional.ofNullable(imageObject.getRepoTags())
                .orElseThrow(() -> new IllegalArgumentException("Cannot process image. RepoTags are null."));
        LOGGER.info("RepoTags are {} ", originalRepoTags);
        List<String> onboardedImages = new ArrayList<>();
        for (String originalRepoTag : originalRepoTags) {
            Matcher registryMatcher = REGISTRY_PATTERN.matcher(originalRepoTag);

            String repoArtifactorySubPath = registryMatcher.find() ? registryMatcher.group(1) : "";
            String imageTag = originalRepoTag.replace(repoArtifactorySubPath + "/", "");
            String[] repoTagSplit = imageTag.split(":");
            if (repoTagSplit.length < 2) {
                throw new IllegalArgumentException("Array length must be greater than 1, but was " + repoTagSplit.length);
            }
            String imageName = repoTagSplit[0];
            String tag = repoTagSplit[1];

            int leftTimeoutMinutes = TimeoutUtils.resolveTimeOut(onboardingTimeout, ChronoUnit.MINUTES);

            skopeoService.pushImageFromDockerTar(pathToTar.toString(), repoArtifactorySubPath, imageName, tag, leftTimeoutMinutes);
            onboardedImages.add(imageTag);
        }
        return onboardedImages;
    }

    @Override
    public void removeDockerImagesByPackageId(String packageId) {
        List<String> imagesToRemove = appPackageDockerImageRepository.findAllRemovableImagesByPackageId(packageId);

        for (String image : imagesToRemove) {
            String[] imageAttributes = image.split(":");
            skopeoService.deleteImageFromRegistry(imageAttributes[0], imageAttributes[1]);
        }
    }
}
