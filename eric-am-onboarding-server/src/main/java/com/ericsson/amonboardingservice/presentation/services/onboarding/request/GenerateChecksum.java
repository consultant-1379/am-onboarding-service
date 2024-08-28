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

import static com.ericsson.amonboardingservice.presentation.services.onboarding.request.OnboardingPhase.GENERATE_CHECKSUM_PHASE;

import java.nio.file.Path;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.amonboardingservice.presentation.exceptions.FailedOnboardingException;
import com.ericsson.amonboardingservice.presentation.models.OnboardingDetail;
import com.ericsson.amonboardingservice.presentation.services.manifestservice.ManifestServiceImpl;
import com.ericsson.amonboardingservice.presentation.services.packageservice.OnboardingDetailsService;

import lombok.extern.slf4j.Slf4j;

/**
 * Generate a checksum of the package and add it to the context.
 */
@Slf4j
@Component
public class GenerateChecksum implements RequestHandler {

    @Autowired
    private OnboardingDetailsService detailsService;

    public void handle(final PackageUploadRequestContext context) {
        LOGGER.info("Starting GenerateChecksum onboarding handler for package with ID: '{}'", context.getPackageId());

        Path packageContents = context.getPackageContents();
        String checksum = ManifestServiceImpl.generateHash(packageContents, ManifestServiceImpl.SHA_512, false);
        context.setChecksum(checksum);

        boolean duplicateCsarUploadingPostChecksumPhase = detailsService.findAllDetails()
                .stream()
                .filter(details -> !details.getAppPackage().getPackageId().equals(context.getPackageId()))
                .map(OnboardingDetail::getPackageUploadContext)
                .filter(Objects::nonNull)
                .map(PackageUploadRequestContext::getChecksum)
                .anyMatch(checksum::equals);

        if (duplicateCsarUploadingPostChecksumPhase) {
            throw new FailedOnboardingException("Package with same content is already processing");
        }

        LOGGER.info("Successfully finished GenerateChecksum onboarding handler. Package ID: '{}'. SHA-512 checksum: '{}'",
                    context.getPackageId(),
                    checksum);
    }

    @Override
    public String getName() {
        return GENERATE_CHECKSUM_PHASE.toString();
    }
}
