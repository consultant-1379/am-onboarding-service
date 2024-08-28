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


import static com.ericsson.amonboardingservice.presentation.services.onboarding.request.OnboardingPhase.CSAR_SIGNATURE_VALIDATION_PHASE;

import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ericsson.amonboardingservice.presentation.models.SignedFileContent;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.ericsson.amonboardingservice.utils.Constants;
import com.ericsson.amonboardingservice.utils.PackageSignatureRetriever;
import com.ericsson.signatureservice.SignatureService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CsarSignatureValidation implements RequestHandler {

    @Autowired
    private PackageSignatureRetriever packageSignatureRetriever;

    @Autowired(required = false)
    private SignatureService signatureService;

    @Autowired
    private FileService fileService;

    @Value("${onboarding.skipCertificateValidation}")
    private boolean isSkipCertificateValidation;

    @Override
    public void handle(final PackageUploadRequestContext context) {
        if (isSkipCertificateValidation) {
            return;
        }

        LOGGER.info("Starting CsarSignatureValidation onboarding handler");

        Path packageContents = context.getPackageContents();
        boolean isCsarSigned = false;

        SignedFileContent signedCsar = packageSignatureRetriever.extractSignedFileForOption2(packageContents.getParent());
        if (StringUtils.isNoneBlank(signedCsar.getSignatureContent())) {
            if (context.getPackageSecurityOption() == null) {
                signatureService.verifyContentSignature(signedCsar.getFileContent(),
                                                        signedCsar.getSignatureContent(),
                                                        signedCsar.getCertificateContent());
            }

            isCsarSigned = true;
            fileService.getFileByExtension(packageContents.getParent(), Constants.CERT_ARCHIVE_EXTENSION)
                    .ifPresent(cert -> fileService.deleteFile(cert.toFile()));
            fileService.getFileByExtension(packageContents.getParent(), Constants.CMS_ARCHIVE_EXTENSION)
                    .ifPresent(cms -> fileService.deleteFile(cms.toFile()));
        } else {
            LOGGER.info("Csar is NOT signed, skipped signature validation.");
        }
        if (isCsarSigned) {
            context.setPackageSecurityOption(AppPackage.PackageSecurityOption.OPTION_2);
        }

        LOGGER.info("Successfully finished CsarSignatureValidation onboarding handler");
    }

    @Override
    public String getName() {
        return CSAR_SIGNATURE_VALIDATION_PHASE.toString();
    }
}
