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
package com.ericsson.amonboardingservice.presentation.services.fileservice;

import com.ericsson.amonboardingservice.presentation.exceptions.InsufficientDiskSpaceException;
import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.presentation.exceptions.UnsupportedMediaTypeException;
import com.ericsson.amonboardingservice.presentation.services.FileTypeDetector;
import com.google.common.net.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
public class FileValidator {

    @Value("${vnf.packages.root}")
    private String rootDirectory;

    @Autowired
    private FileTypeDetector fileTypeDetector;

    @Autowired
    private FileService fileService;

    /**
     * Pre validation step before upload of a package
     *
     * @param packageContents
     */
    public void validateFileOnPreUpload(final MultipartFile packageContents) {
        if (packageContents == null) {
            throw new IllegalArgumentException("PackageContents is not defined");
        }
        if (!isEnoughSpaceInRootDirectory(packageContents)) {
            throw new InsufficientDiskSpaceException("There is not enough space on disk to upload file");
        }

        try (InputStream packageInputStream = packageContents.getInputStream()) {
            validate(packageInputStream, MediaType.ZIP);
        } catch (IOException e) {
            throw new InternalRuntimeException("Failed to load the file due to: " + e.getMessage(), e);
        }
    }

    public void validateFileOnPreUpload(final InputStream packageInputStream, long fileSize) {
        if (packageInputStream == null) {
            throw new IllegalArgumentException("PackageContents is not defined");
        }

        if (!fileService.isEnoughSpaceInRootDirectory(fileSize)) {
            throw new InsufficientDiskSpaceException("There is not enough space on disk to upload file");
        }

        validate(packageInputStream, MediaType.ZIP);
    }

    /**
     * Check if there is enough space in root directory of VNF Packages before uploading
     * @param multipartFile
     * @return boolean value if there is enough space in root directory
     */
    public boolean isEnoughSpaceInRootDirectory(final MultipartFile multipartFile) {
        LOGGER.info("Checking if there is enough space on disk to upload file");
        return fileService.isEnoughSpaceInRootDirectory(multipartFile.getSize());
    }

    private void validate(final InputStream inputStream, final MediaType mediaType) {
        if (!fileTypeDetector.hasFileContentType(inputStream, mediaType)) {
            throw new UnsupportedMediaTypeException(String.format("Provided Invalid Content type : %s. "
                    + "Valid content type that is currently"
                    + " supported is MediaType.ZIP", mediaType));
        }
    }
}
