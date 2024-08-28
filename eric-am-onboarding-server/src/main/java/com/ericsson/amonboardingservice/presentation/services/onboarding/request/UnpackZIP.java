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

import static com.ericsson.amonboardingservice.presentation.services.onboarding.request.OnboardingPhase.ZIP_UNPACKING_PHASE;
import static com.ericsson.amonboardingservice.utils.TimeoutUtils.resolveTimeOut;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.amonboardingservice.presentation.exceptions.UnsupportedMediaTypeException;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.ericsson.amonboardingservice.utils.Constants;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UnpackZIP implements RequestHandler {

    @Autowired
    private FileService fileService;

    @Override
    public void handle(final PackageUploadRequestContext context) {
        Path packageContents = context.getPackageContents();
        LocalDateTime timeoutDate = context.getTimeoutDate();
        String originalFileName = context.getOriginalFileName();
        if (originalFileName.endsWith(Constants.ZIP_ARCHIVE_EXTENSION)) {
            LOGGER.info("Starting UnpackZIP onboarding handler");

            fileService.unpack(packageContents, resolveTimeOut(timeoutDate, ChronoUnit.MINUTES));

            if (isFileWithExtensionNotPresent(packageContents, Constants.CSAR_ARCHIVE_EXTENSION)) {
                throw new UnsupportedMediaTypeException(String.format("Cannot locate csar archive in %s.", originalFileName));
            }
            if (isFileWithExtensionNotPresent(packageContents, Constants.CMS_ARCHIVE_EXTENSION)) {
                throw new UnsupportedMediaTypeException(String.format("Cannot locate signature in %s.", originalFileName));
            }

            context.setPackageSigned(true);

            LOGGER.info("Successfully finished UnpackZIP onboarding handler");
        }
    }

    @Override
    public String getName() {
        return ZIP_UNPACKING_PHASE.toString();
    }

    private boolean isFileWithExtensionNotPresent(final Path packageContents, final String extension) {
        return fileService
                .getFileByExtension(packageContents.getParent(), extension)
                .isEmpty();
    }
}
