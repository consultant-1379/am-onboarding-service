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

import static com.ericsson.amonboardingservice.presentation.services.onboarding.request.OnboardingPhase.MANIFEST_SIGNATURE_VALIDATION_PHASE;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_CERTIFICATE;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_MANIFEST;

import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ericsson.amonboardingservice.presentation.exceptions.SignatureValidationException;
import com.ericsson.amonboardingservice.presentation.models.SignedFileContent;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.ericsson.amonboardingservice.utils.PackageSignatureRetriever;
import com.ericsson.signatureservice.SignatureService;
import com.ericsson.signatureservice.exception.SignatureVerificationException;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ManifestSignatureValidation implements RequestHandler {
    @Autowired
    private PackageSignatureRetriever packageSignatureRetriever;

    @Autowired(required = false)
    private SignatureService signatureService;

    @Autowired
    private FileService fileService;

    @Value("${onboarding.skipCertificateValidation}")
    private boolean isSkipCertificateValidation;

    @Value("${onboarding.allowUnsignedVNFPackageOnboarding}")
    @Setter(value = AccessLevel.PACKAGE)
    private boolean allowUnsignedVNFPackageOnboarding;

    @Override
    public void handle(final PackageUploadRequestContext context) {
        if (isSkipCertificateValidation) {
            return;
        }

        LOGGER.info("Starting ManifestSignatureValidation onboarding handler");

        Path manifestPath = context.getArtifactPaths().get(ENTRY_MANIFEST);
        Path certificatePath = context.getArtifactPaths().get(ENTRY_CERTIFICATE);
        boolean isManifestSigned = false;
        AppPackage.PackageSecurityOption securityOption = context.getPackageSecurityOption();

        List<SignedFileContent> signedManifestCandidates = packageSignatureRetriever.extractSignedFilesForOption1(manifestPath, certificatePath);
        if (!signedManifestCandidates.isEmpty()) {

            if (securityOption == null || AppPackage.PackageSecurityOption.OPTION_2.equals(securityOption)) {
                verifySignedManifest(signedManifestCandidates);
            }

            if (certificatePath != null) {
                fileService.deleteFile(certificatePath.toFile());
            }
            fileService.deleteFile(manifestPath.toFile());
            fileService.storeFile(signedManifestCandidates.get(0).getFileContent(), manifestPath.getParent(), manifestPath.getFileName().toString());
            isManifestSigned = true;
        } else {
            LOGGER.info("Manifest is NOT signed, skipped signature validation.");
        }
        context.setPackageSecurityOption(getPackageSecurityOption(context.getPackageId(),
                                                                  context.getPackageSecurityOption(),
                                                                  isManifestSigned));

        LOGGER.info("Successfully finished ManifestSignatureValidation onboarding handler");
    }

    @Override
    public String getName() {
        return MANIFEST_SIGNATURE_VALIDATION_PHASE.toString();
    }

    private AppPackage.PackageSecurityOption getPackageSecurityOption(String packageId,
                                                                      AppPackage.PackageSecurityOption currentSecurityOption,
                                                                      boolean isSignedManifest) {
        if (AppPackage.PackageSecurityOption.OPTION_2 == currentSecurityOption) {
            return AppPackage.PackageSecurityOption.OPTION_2;
        }
        if (isSignedManifest) {
            return AppPackage.PackageSecurityOption.OPTION_1;
        }
        if (allowUnsignedVNFPackageOnboarding) {
            return AppPackage.PackageSecurityOption.UNSIGNED;
        }
        throw new SignatureValidationException(String.format("Package with id: %s does not contain signature."
                                                                     + " Unsigned packages are not allowed.", packageId));
    }

    private void verifySignedManifest(final List<SignedFileContent> signedManifestCandidates) {
        SignatureVerificationException validationFailure = null;

        for (final SignedFileContent signedManifest : signedManifestCandidates) {
            try {
                signatureService.verifyContentSignature(signedManifest.getFileContent(),
                                                        signedManifest.getSignatureContent(),
                                                        signedManifest.getCertificateContent());

                return;
            } catch (final SignatureVerificationException e) {
                validationFailure = e;
            }
        }

        if (validationFailure != null) {
            throw validationFailure;
        }
    }
}
