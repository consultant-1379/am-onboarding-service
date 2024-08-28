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
package com.ericsson.amonboardingservice.presentation.services.filestorage;

import java.io.File;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.ericsson.amonboardingservice.presentation.services.objectstorage.ObjectStorageService;
import com.ericsson.amonboardingservice.utils.Constants;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Autowired
    private FileService fileService;
    @Autowired
    private ObjectStorageService objectStorageService;

    @Value("${onboarding.highAvailabilityMode}")
    private boolean highAvailabilityMode;

    public void deleteFileFromObjectStorage(final Path path) {
        if (highAvailabilityMode) {
            objectStorageService.deleteFile(path.toString());
        }
    }

    public Path storeFile(final MultipartFile zipFile, final Path directoryPath, String fileName) {
        final Path packagePath = fileService.storeFile(zipFile, directoryPath, fileName);
        File packageFile = packagePath.toFile();
        if (highAvailabilityMode) {
            objectStorageService.uploadFile(packageFile, packagePath.toString());
        }
        return packagePath;
    }

    public Path storePackageFromObjectStorage(String filename, String originalFilename) {
        Path pathToFile = Path.of(filename);
        if (fileService.isFileExist(pathToFile)) {
            return pathToFile;
        }

        String directoryName = pathToFile.getParent().toString();
        if (!pathToFile.getParent().toFile().exists()) {
            fileService.createDirectory(directoryName);
        }

        Path path = fileService.create(pathToFile.getParent(), Constants.VNF_PACKAGE_ZIP);
        LOGGER.info("Storing {} to {} from object storage", originalFilename, filename);
        objectStorageService.downloadFile(path.toFile(), filename);
        return path;
    }
}
