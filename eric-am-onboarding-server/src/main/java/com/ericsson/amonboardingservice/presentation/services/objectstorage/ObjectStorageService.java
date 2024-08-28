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
package com.ericsson.amonboardingservice.presentation.services.objectstorage;

import java.io.File;
import java.io.InputStream;

public interface ObjectStorageService {

    void checkAndMakeBucket();

    void uploadFile(File file, String filePath);

    void uploadFile(InputStream file, String filePath);

    void downloadFile(File storeTo, String filePath);

    void deleteFile(String filePath);
}
