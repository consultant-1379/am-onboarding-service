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

import java.nio.file.Path;
import java.util.List;

import com.ericsson.amonboardingservice.presentation.models.SignedFileContent;

public interface PackageSignatureRetriever {

    SignedFileContent extractSignedFileForOption2(Path pathToFilesInPackage);

    List<SignedFileContent> extractSignedFilesForOption1(Path pathToManifest, Path pathToCertificate);
}
