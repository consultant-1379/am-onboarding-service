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

import com.ericsson.amonboardingservice.model.ImageResponse;
import com.ericsson.amonboardingservice.model.ImageResponseImages;
import com.ericsson.amonboardingservice.model.ImageResponseProjects;
import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageDockerImageRepository;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.ericsson.amonboardingservice.presentation.services.imageservice.ImageService;
import com.ericsson.amonboardingservice.utils.Constants;
import com.ericsson.amonboardingservice.utils.TimeoutUtils;
import com.ericsson.amonboardingservice.utils.docker.DockerRegistryHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_256;

/**
 * Service for working with Docker images
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "skopeo", name = "enabled", havingValue = "false", matchIfMissing = true)
public class HttpClientDockerServiceImpl implements DockerService {
    private static final Pattern REGISTRY_PATTERN = Pattern.compile("(.+?(?=\\/))\\/([^\\/]+?(?=\\/))?");
    private static final String UNABLE_TO_PARSE_YAML_FILE_ERROR_MESSAGE = "Unable to parse yaml file";

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private AppPackageDockerImageRepository dockerImageRepository;

    @Autowired
    private DockerRegistryHelper dockerRegistryHelper;

    @Autowired
    private ContainerRegistryService containerRegistryService;

    @Autowired
    private FileService fileService;

    @Autowired
    private ImageService imageService;

    @Override
    public List<String> onboardDockerTar(Path pathToTar, LocalDateTime onboardingTimeoutDate) {
        LOGGER.info("Onboard docker tar from path: {}", pathToTar);
        List<String> onboardedImages;
        int timeoutInMinutes = TimeoutUtils.resolveTimeOut(onboardingTimeoutDate, ChronoUnit.MINUTES);
        Path manifestPath = fileService.extractFileFromTar(pathToTar, pathToTar.getParent(),
                                                           Constants.MANIFEST_JSON, timeoutInMinutes);
        JSONArray manifest = readManifestFile(manifestPath);
        try {
            List<ImageObject> imageObjects = mapper.readValue(
                    manifest.toString(), mapper.getTypeFactory().constructCollectionType(List.class, ImageObject.class));

            Map<String, Set<String>> imagesInRegistry = getImagesInRegistry();

            onboardedImages = this.stream(imageObjects)
                    .map(imageObject -> {
                        LOGGER.info("Starting processing {} Image layers", imageObject.getLayers().size());
                        return processImage(pathToTar, imageObject, onboardingTimeoutDate, imagesInRegistry);
                    })
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        } catch (IOException ioe) {
            LOGGER.error(UNABLE_TO_PARSE_YAML_FILE_ERROR_MESSAGE);
            throw new IllegalArgumentException(UNABLE_TO_PARSE_YAML_FILE_ERROR_MESSAGE, ioe);
        }
        LOGGER.info("Successfully onboarded images: {}. Size is: {}", onboardedImages, onboardedImages.size());
        return onboardedImages;
    }

    private Map<String, Set<String>> getImagesInRegistry() {
        try {
            ImageResponse imageResponse = imageService.getAllImages();
            return imageResponse.getProjects().stream()
                    .map(ImageResponseProjects::getImages)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .collect(Collectors.groupingBy(ImageResponseImages::getRepository,
                                                   Collectors.flatMapping(imageResponseImages ->
                                                                                  Optional.ofNullable(imageResponseImages.getTags())
                                                                                          .stream()
                                                                                          .flatMap(Collection::stream),
                                                                          Collectors.toSet())));
        } catch (Exception ex) {
            LOGGER.warn("Exception occurred when getting registry catalog: ", ex);
            return Collections.emptyMap();
        }

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

    private List<String> processImage(Path pathToTar, ImageObject imageObject,
                                      LocalDateTime onboardingTimeoutDate, Map<String, Set<String>> imagesInRegistry) {
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

            if (isImageExistInRegistry(imageName, tag, imagesInRegistry)) {
                LOGGER.info("Skip uploading image {} as it's already presented in registry", imageTag);
            } else {
                AtomicInteger counter = new AtomicInteger(0);
                final int layersAmount = imageObject.getLayers().size() + 1;

                Path extractPath = createExtractDirectory(pathToTar);

                FutureLayerProcessor<LayerObject> layerProcessor = new FutureLayerProcessor<>();
                List<LayerObject> layers = this.stream(imageObject.getLayers())
                      .map(layer -> processLayer(layerProcessor, pathToTar, extractPath, imageName, layer, counter,
                           layersAmount, onboardingTimeoutDate))
                      .collect(Collectors.toList());

                LOGGER.info("uploading config {} to registry {}", imageObject.getConfig(), containerRegistryService.getDockerRegistry());
                LayerObject config = processLayer(layerProcessor, pathToTar, extractPath, imageName, imageObject.getConfig(),
                            counter, layersAmount, onboardingTimeoutDate);
                config.setMediaType(Constants.DOCKER_LAYER_MEDIA_TYPE_JSON);
                containerRegistryService.processManifest(layers, imageName, tag, config);
                fileService.deleteDirectory(extractPath.toString());
            }
            onboardedImages.add(imageTag);
        }

        return onboardedImages;
    }

    private boolean isImageExistInRegistry(String repo, String tag, Map<String, Set<String>> imagesInRegistry) {
        Set<String> tags = imagesInRegistry.get(repo);
        if (CollectionUtils.isEmpty(tags)) {
            return false;
        }

        return tags.contains(tag);
    }

    private LayerObject processLayer(FutureLayerProcessor<LayerObject> layerProcessor, Path pathToTar, Path extractPath, String repo, String layer,
                                     AtomicInteger counter,
                                     int layersAmount, LocalDateTime onboardingTimeoutDate) {
        LOGGER.info("Current status of layer processing: {} of {} , layer {}", counter.incrementAndGet(), layersAmount, layer);
        boolean present = layerProcessor.processedLayerPresent(layer);
        if (present) {
            return layerProcessor.getProcessedLayer(layer);
        }
        int onboardingTimeout = TimeoutUtils.resolveTimeOut(onboardingTimeoutDate, ChronoUnit.MINUTES);
        Path layerPath = fileService.extractFileFromTar(pathToTar, extractPath, layer, onboardingTimeout);
        if (Files.isSymbolicLink(layerPath)) {
            try {
                String targetLayer = Files.readSymbolicLink(layerPath).toString().replace("../", "");
                deleteExtractedLayerDirectory(layerPath);
                LOGGER.info("Link layer {} processing, waiting for completed target layer {}", layer, targetLayer);
                LayerObject processedLayer = processLayer(layerProcessor, pathToTar, extractPath, repo, targetLayer, counter,
                                                          layersAmount, onboardingTimeoutDate);
                return layerProcessor.completeFuture(targetLayer, processedLayer);
            } catch (IOException e) {
                throw new InternalRuntimeException("Failed to extract layer for docker tar", e);
            }
        }

        LayerObject processedLayerObject = processExtractedLayer(layerPath, layer, repo);
        deleteExtractedLayerDirectory(layerPath);
        layerProcessor.completeFuture(layer, processedLayerObject);
        LOGGER.info("Layer {} size {} bytes was processed successfully", layer, processedLayerObject.getSize());
        return processedLayerObject;
    }

    private <T> Stream<T> stream(Collection<T> items) {
        return items.stream();
    }

    private LayerObject processExtractedLayer(Path layerPath, String layer, String repo) {
        String layerDigest = fileService.generateHash(layerPath, SHA_256);

        if (containerRegistryService.isLayerExists(repo, layerDigest)) {
            LOGGER.info("Layer {} already exists", layer);
        } else {
            containerRegistryService.uploadLayer(repo, layer, layerPath, layerDigest);
        }

        long size = layerPath.toFile().length();
        return new LayerObject(size, layerDigest);
    }

    @Override
    public void removeDockerImagesByPackageId(String packageId) {
        List<String> imagesToRemove = dockerImageRepository.findAllRemovableImagesByPackageId(packageId);

        for (String image : imagesToRemove) {
            String[] imageAttributes = image.split(":");
            dockerRegistryHelper.deleteManifestsByTag(imageAttributes[0], imageAttributes[1]);
        }
    }

    private void deleteExtractedLayerDirectory(Path layerPath) {
        LOGGER.info("Deleting extracted layer directory {}", layerPath);
        FileUtils.deleteQuietly(layerPath.toFile());
    }

    private Path createExtractDirectory(final Path pathToTar) {
        Path extractDirectory = Paths.get(pathToTar.getParent().toString(), UUID.randomUUID().toString());
        fileService.createDirectory(extractDirectory.toString());
        return extractDirectory;
    }

    private static class FutureLayerProcessor<T> {
        private final Map<String, CompletableFuture<T>> layerProcessingResult = Collections.synchronizedMap(new HashMap<>());

        boolean processedLayerPresent(String layerName) {
            CompletableFuture<T> futureFromMap = this.layerProcessingResult.putIfAbsent(layerName, new CompletableFuture<>());
            return futureFromMap != null;
        }

        T getProcessedLayer(String layerName) {
            try {
                return layerProcessingResult.get(layerName).get();
            } catch (InterruptedException | ExecutionException e) { // NOSONAR
                throw new InternalRuntimeException("Failed to extract layer from docker tar", e);
            }
        }

        T completeFuture(String layerName, T result) {
            layerProcessingResult.get(layerName).complete(result);
            return result;
        }
    }
}