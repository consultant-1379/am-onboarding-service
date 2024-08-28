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
package com.ericsson.amonboardingservice.presentation.services.toscametaservice;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tika.io.TikaInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.presentation.exceptions.UnsupportedMediaTypeException;
import com.ericsson.amonboardingservice.presentation.services.FileTypeDetector;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.google.common.net.MediaType;

import lombok.extern.slf4j.Slf4j;

import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_CERTIFICATE;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_CHANGE_LOG;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_HELM_DEFINITIONS;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_IMAGES;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_LICENSES;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_MANIFEST;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ETSI_ENTRY_CERTIFICATE;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ETSI_ENTRY_CHANGE_LOG;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ETSI_ENTRY_HELM_DEFINITIONS;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ETSI_ENTRY_IMAGES;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ETSI_ENTRY_MANIFEST;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ETSI_ENTRY_TESTS;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.RELATIVE_PATH_TO_TOSCA_META_FILE;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_TESTS;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ETSI_ENTRY_LICENSES;

/**
 * This service provides functionality to retrieve information from the TOSCA.meta file in the CSAR.
 */
@Slf4j
@Service
public class ToscaMetaService {

    private static final Map<String, String> MODERN_TO_LEGACY_META_FILE_KEYS_MAPPING = new HashMap<>();

    static {
        MODERN_TO_LEGACY_META_FILE_KEYS_MAPPING.put(ETSI_ENTRY_MANIFEST, ENTRY_MANIFEST);
        MODERN_TO_LEGACY_META_FILE_KEYS_MAPPING.put(ETSI_ENTRY_CHANGE_LOG, ENTRY_CHANGE_LOG);
        MODERN_TO_LEGACY_META_FILE_KEYS_MAPPING.put(ETSI_ENTRY_TESTS, ENTRY_TESTS);
        MODERN_TO_LEGACY_META_FILE_KEYS_MAPPING.put(ETSI_ENTRY_LICENSES, ENTRY_LICENSES);
        MODERN_TO_LEGACY_META_FILE_KEYS_MAPPING.put(ETSI_ENTRY_CERTIFICATE, ENTRY_CERTIFICATE);
        MODERN_TO_LEGACY_META_FILE_KEYS_MAPPING.put(ETSI_ENTRY_IMAGES, ENTRY_IMAGES);
        MODERN_TO_LEGACY_META_FILE_KEYS_MAPPING.put(ETSI_ENTRY_HELM_DEFINITIONS, ENTRY_HELM_DEFINITIONS);
    }

    @Autowired
    private FileService fileSystemService;

    @Autowired
    private FileTypeDetector fileTypeDetector;

    /**
     * @param unpackedDirectory The path to the unpacked CSAR.
     * @return A map of Paths which are in the TOSCA.meta file.
     */
    public Map<String, Path> getArtifactsMapFromToscaMetaFile(final Path unpackedDirectory) {
        LOGGER.info("Started getting file paths: {}", unpackedDirectory);
        Path toscaMetaPath = getToscaMetaPath(unpackedDirectory);
        Map<String, Path> paths = parseArtifactPathsToscaMetaFile(unpackedDirectory, toscaMetaPath);
        addDefaultPathsIfRequired(unpackedDirectory, paths);
        LOGGER.info("Completed getting file paths: {}", unpackedDirectory);
        return paths;
    }

    public Path getToscaMetaPath(final Path unpackedDirectory) {
        LOGGER.info("Get the path to the TOSCA.meta file in the unpacked CSAR");
        final Path path = unpackedDirectory.resolve(RELATIVE_PATH_TO_TOSCA_META_FILE);
        validateFile(path);
        LOGGER.info("Completed getting the path to the TOSCA.meta file in the unpacked CSAR");
        return path;
    }

    private void validateFile(final Path path) {
        LOGGER.info("Started validation of file: {}", path);
        if (Files.notExists(path)) {
            throw new UnsupportedMediaTypeException(String.format("Only CSAR packages of type A1 are supported."
                    + " The TOSCA.meta %s file was not found in the TOSCA-Metadata directory.", path));
        }
        if (path.toFile().length() == 0) {
            throw new UnsupportedMediaTypeException(String.format("The TOSCA.meta file %s was found to be empty", path));
        }
        try (InputStream stream = TikaInputStream.get(path)) {
            if (!fileTypeDetector.hasFileContentType(stream, MediaType.create("text", "plain"))) {
                throw new UnsupportedMediaTypeException(
                        String.format("The TOSCA.meta file %s type is incorrect, text/plain is all that is supported.", path));
            }
            LOGGER.info("Completed validation of file: {}", path);
        } catch (IOException e) {
            LOGGER.error("Failed to determine content type of TOSCA.meta file {} due to {}", path, e.getMessage(), e);
        }
    }

    private static Map<String, Path> parseArtifactPathsToscaMetaFile(final Path unpackedDirectory, final Path toscaMetaPath) {
        LOGGER.info("Parse the TOSCA.meta file: {} for all the paths it contains", toscaMetaPath);
        Map<String, Path> artifactPaths = new HashMap<>();
        List<String> toscaMetaFileLines = readFileLines(toscaMetaPath);
        for (String toscaMetaFileLine : toscaMetaFileLines) {
            String[] parts = toscaMetaFileLine.split(":");
            if (parts.length == 2) {
                String metaFileEntryKey = parts[0].trim();
                metaFileEntryKey = MODERN_TO_LEGACY_META_FILE_KEYS_MAPPING.
                        getOrDefault(metaFileEntryKey, metaFileEntryKey);
                Path metaFileArtifactPath = unpackedDirectory.resolve(parts[1].trim());
                if (Files.exists(metaFileArtifactPath)) {
                    artifactPaths.put(metaFileEntryKey, metaFileArtifactPath);
                }
            }
        }
        LOGGER.info("Completed parse the TOSCA.meta file {} for all the paths it contains", toscaMetaPath);
        return artifactPaths;
    }

    private static List<String> readFileLines(final Path filePath) {
        try (Stream<String> lines = Files.lines(filePath)) {
            return lines.collect(Collectors.toList());
        } catch (IOException e) {
            String errorMessage = String.format("Failed to read content of the %s file", filePath);
            LOGGER.error(errorMessage, e);
            throw new InternalRuntimeException(errorMessage, e);
        }
    }

    private void addDefaultPathsIfRequired(final Path unpackedDirectory, final Map<String, Path> paths) {
        LOGGER.info("Started adding default paths: {}, {}", unpackedDirectory, paths);
        Optional<Path> chartPath = fileSystemService.getChartPath(unpackedDirectory);
        Optional<Path> dockerPackagePath = fileSystemService.getDockerPackagePath(unpackedDirectory);
        if (!paths.containsKey(ENTRY_HELM_DEFINITIONS) && chartPath.isPresent()) {
            LOGGER.info("Put the default path to the helm chart");
            paths.put(ENTRY_HELM_DEFINITIONS, chartPath.get());
        }
        if (!paths.containsKey(ENTRY_IMAGES) && dockerPackagePath.isPresent()) {
            LOGGER.info("Put the default path to the docker images tar file");
            paths.put(ENTRY_IMAGES, dockerPackagePath.get());
        }
        LOGGER.info("Completed adding default paths: {}, {}", unpackedDirectory, paths);
    }
}
