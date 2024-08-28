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
package com.ericsson.amonboardingservice.utils;

import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.amonboardingservice.presentation.exceptions.SignatureValidationException;
import com.ericsson.amonboardingservice.presentation.models.SignedFileContent;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PackageSignatureRetrieverImpl implements PackageSignatureRetriever {

    private static final String BEGIN_HEADER = "-----BEGIN";
    private static final int WITH_CERTIFICATE = 3;
    private static final int WITHOUT_CERTIFICATE = 2;

    @Autowired
    private FileService fileService;

    @Override
    public SignedFileContent extractSignedFileForOption2(Path pathToFilesInPackage) {
        LOGGER.info("Started checking whether csar is signed: {}", pathToFilesInPackage);
        SignedFileContent signedFileContent = new SignedFileContent();

        Optional<Path> csarPackageLocation = fileService.getFileByExtension(pathToFilesInPackage, Constants.CSAR_ARCHIVE_EXTENSION);
        Optional<Path> signatureLocation = fileService.getFileByExtension(pathToFilesInPackage, Constants.CMS_ARCHIVE_EXTENSION);
        Optional<Path> certificateLocation = fileService.getFileByExtension(pathToFilesInPackage, Constants.CERT_ARCHIVE_EXTENSION);

        if (signatureLocation.isEmpty()) {
            return signedFileContent;
        }
        validateNumberOfFiles(pathToFilesInPackage, csarPackageLocation);
        validateFileNames(signatureLocation, csarPackageLocation, certificateLocation);

        signedFileContent.setSignatureContent(fileService.readFile(signatureLocation.get()));
        certificateLocation.ifPresent(path -> signedFileContent.setCertificateContent(fileService.readFile(path)));
        csarPackageLocation.ifPresent(path -> signedFileContent.setFileContent(fileService.readFileAsFileStream(path)));

        return signedFileContent;
    }

    @Override
    public List<SignedFileContent> extractSignedFilesForOption1(Path pathToManifest, Path pathToCertificate) {
        LOGGER.info("Started checking whether Manifest is signed: {}", pathToManifest);

        final String signatureContent = retrieveSignatureFromManifest(pathToManifest);

        if (StringUtils.isBlank(signatureContent)) {
            return List.of();
        }

        final String certificateContent = trimToNull(fileService.readFile(pathToCertificate));

        final SignedFileContent signedFileContent1 = new SignedFileContent();
        signedFileContent1.setSignatureContent(signatureContent);
        signedFileContent1.setCertificateContent(certificateContent);
        signedFileContent1.setFileContent(retrieveManifestContentStrippingNewline(pathToManifest));

        final SignedFileContent signedFileContent2 = new SignedFileContent();
        signedFileContent2.setSignatureContent(signatureContent);
        signedFileContent2.setCertificateContent(certificateContent);
        signedFileContent2.setFileContent(retrieveManifestContent(pathToManifest));

        return List.of(signedFileContent1, signedFileContent2);
    }

    private String retrieveSignatureFromManifest(final Path pathToManifest) {
        String signatureContent = fileService.readFile(pathToManifest);
        if (signatureContent.contains(BEGIN_HEADER)) {
            return signatureContent.substring(signatureContent.indexOf(BEGIN_HEADER)).trim();
        }

        return null;
    }

    private InputStream retrieveManifestContent(final Path manifestPath) {
        return retrieveManifestContentStrippingChars(manifestPath, 0);
    }

    private InputStream retrieveManifestContentStrippingNewline(final Path manifestPath) {
        return retrieveManifestContentStrippingChars(manifestPath, 1);
    }

    private InputStream retrieveManifestContentStrippingChars(final Path manifestPath, final int charsToStrip) {
        final String manifestContent = fileService.readFile(manifestPath);

        if (manifestContent.contains(BEGIN_HEADER)) {
            final String strippedContent = manifestContent.substring(0, manifestContent.indexOf(BEGIN_HEADER) - charsToStrip);

            return new ByteArrayInputStream(strippedContent.getBytes(StandardCharsets.UTF_8));
        }

        return null;
    }

    private void validateNumberOfFiles(Path pathToFilesInPackage, Optional<Path> csarPackageLocation) {
        if (Files.isDirectory(pathToFilesInPackage) && csarPackageLocation.isPresent() &&
                !isCorrectNoOfFiles(pathToFilesInPackage)) {
            throw new SignatureValidationException("For Option 2 signature verification Zip archive should only contain Csar, one Signature file "
                                                           + "and "
                                                           + "optionally "
                                                           + "one Certificate file.");
        }
    }

    private void validateFileNames(final Optional<Path> signatureLocation,
                                   final Optional<Path> csarPackageLocation,
                                   final Optional<Path> certificateLocation) {
        if (csarPackageLocation.isPresent()) {
            signatureLocation.ifPresent(path -> validateFileNameAgainstCsar(csarPackageLocation.get(), path));
            certificateLocation.ifPresent(path -> validateFileNameAgainstCsar(csarPackageLocation.get(), path));
        }
    }

    private void validateFileNameAgainstCsar(Path csarPath, Path filePath) {
        String csarNameNoExtension = fileService.getFileNameWithoutExtension(csarPath);
        String fileNameNoExtension = fileService.getFileNameWithoutExtension(filePath);
        if (!csarNameNoExtension.equals(fileNameNoExtension)) {
            throw new SignatureValidationException("Csar, Certificate and Signature file names must be identical for Option 2 signature "
                                                           + "verification.");
        }
    }

    private boolean isCorrectNoOfFiles(Path pathToFilesInPackage) {
        return pathToFilesInPackage.toFile().list().length == WITH_CERTIFICATE || pathToFilesInPackage.toFile().list().length == WITHOUT_CERTIFICATE;
    }
}
