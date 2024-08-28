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

import java.nio.file.Path;

import org.springframework.web.multipart.MultipartFile;

/**
 * Service for working with files
 */
public interface FileStorageService {

    /**
     * Delete file from Object Storage
     *
     * @param path
     *         to the stored file
     */
    void deleteFileFromObjectStorage(Path path);

    /**
     * Store the contents of the uploaded file to the specified file
     *
     * @param zipFile
     * @param directoryPath
     * @param fileName
     * @return the path to the stored file
     */
    Path storeFile(MultipartFile zipFile, Path directoryPath, String fileName);

    Path storePackageFromObjectStorage(String filename, String originalFilename);
}
