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

import com.ericsson.amonboardingservice.presentation.exceptions.FailedOnboardingValidationException;
import com.ericsson.amonboardingservice.presentation.exceptions.UnsupportedMediaTypeException;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.ericsson.amonboardingservice.presentation.services.manifestservice.ManifestService;
import com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaService;
import com.ericsson.amonboardingservice.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static com.ericsson.amonboardingservice.presentation.services.onboarding.request.OnboardingPhase.CSAR_UNPACKING_PHASE;
import static com.ericsson.amonboardingservice.utils.TimeoutUtils.resolveTimeOut;

/**
 * Unpack the package and add the paths to the context
 */
@Slf4j
@Component
public class UnpackCSAR implements RequestHandler {

    @Autowired
    private FileService fileService;

    @Autowired
    private ToscaMetaService toscaMetaService;

    @Autowired
    private ManifestService manifestService;

    @Override
    public void handle(final PackageUploadRequestContext context) {
        LOGGER.info("Starting UnpackCSAR onboarding handler");

        Path packageContents = context.getPackageContents();
        LocalDateTime timeoutDate = context.getTimeoutDate();
        String originalFileName = context.getOriginalFileName();
        boolean packageSigned = context.isPackageSigned();

        Map<String, Path> artifactPaths = validateCsarArtifacts(packageContents, originalFileName, packageSigned, timeoutDate);
        context.setArtifactPaths(artifactPaths);

        LOGGER.info("Successfully finished UnpackCSAR onboarding handler");
    }

    @Override
    public String getName() {
        return CSAR_UNPACKING_PHASE.toString();
    }

    private Map<String, Path> validateCsarArtifacts(final Path packageContents,
                                                    final String originalFileName,
                                                    final boolean packageSigned,
                                                    final LocalDateTime timeoutDate) {

        LOGGER.info("Started validation of CSAR artifacts from package contents: {}", packageContents);

        validateCsarExtension(originalFileName, packageSigned);

        final Path csarPath = resolveCsarPath(packageContents, packageSigned);
        fileService.unpack(csarPath, resolveTimeOut(timeoutDate, ChronoUnit.MINUTES));

        Map<String, Path> artifactPaths = toscaMetaService.getArtifactsMapFromToscaMetaFile(packageContents.getParent());
        manifestService.validateDigests(packageContents.getParent(), artifactPaths);

        // Validating if all the files provided in the Map are present in directory
        for (Map.Entry<String, Path> entry : artifactPaths.entrySet()) {
            if (!entry.getValue().toFile().exists()) {
                throw new FailedOnboardingValidationException(
                        String.format("Failed to validate CSAR artifacts: %s. %s file does not exist in the extracted CSAR",
                                      csarPath, entry.getValue().toString()));
            }
        }

        artifactPaths.put(Constants.PATH_TO_PACKAGE, csarPath);
        artifactPaths.put(Constants.CSAR_DIRECTORY, packageContents.getParent());

        LOGGER.info("Completed validation of CSAR artifacts: {}", csarPath);

        return artifactPaths;
    }

    private Path resolveCsarPath(final Path packageContents, final boolean packageSigned) {
        if (packageSigned) {
            return fileService.getFileByExtension(packageContents.getParent(), Constants.CSAR_ARCHIVE_EXTENSION).orElseThrow();
        }

        return packageContents;
    }

    private void validateCsarExtension(final String originalFileName, final boolean packageSigned) {
        if (!packageSigned && !originalFileName.endsWith(Constants.CSAR_ARCHIVE_EXTENSION)) {
            throw new UnsupportedMediaTypeException(String.format("Unsupported CSAR extension: %s. Only .csar extension is supported.",
                                                                  fileService.getFileExtension(originalFileName)));
        }
    }
}
