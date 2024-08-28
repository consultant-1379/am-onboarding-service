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
package com.ericsson.amonboardingservice.utilities;

import com.ericsson.amonboardingservice.exceptions.TestRuntimeException;
import com.ericsson.amonboardingservice.model.OnboardApp;
import com.ericsson.amonboardingservice.model.TestPackageData;
import com.ericsson.amonboardingservice.steps.OnboardingAcceptanceTestSteps;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static com.ericsson.amonboardingservice.utilities.RestUtils.httpGetCall;
import static com.ericsson.amonboardingservice.utilities.TestConstants.EXTERNAL_CSAR_URI;
import static java.util.Objects.requireNonNull;

@Slf4j
public final class CsarFileUtils {
    private static final String TESTING_CSARS_FILE = "testing_csars.json";

    private CsarFileUtils() {
    }

    public static FileSystemResource getCsarPackage(OnboardApp app) {
        return getCsarPackage(app, false);
    }

    public static FileSystemResource getCsarPackage(OnboardApp app, boolean isLocalResource) {
        String csarPath = app.getCsarDownloadUrl();
        File file;
        if (isLocalResource) {
            URL resource = Resources.getResource(csarPath);
            file = new File(csarPath);
            try {
                FileUtils.copyURLToFile(resource, file);
            } catch (IOException e) {
                LOGGER.error("Failed to load local csar file from path: " + csarPath, e);
                throw new TestRuntimeException(e.getMessage());
            }
        } else {
            file = new File(csarPath);
        }
        if (file.exists()) {
            file = loadCsarFromClasspath(csarPath);
        } else {
            LOGGER.warn(String.format("The file %s does not exist. Will try to download it.", file.getAbsolutePath()));
            file = downloadRemoteCsar(app);
        }
        LOGGER.info("CSAR package loaded from {}, filesize: {}", file.getAbsolutePath(), file.length());
        return new FileSystemResource(file);
    }

    public static String getCsarName(final OnboardApp onboardApp) {
        String[] segments = onboardApp.getCsarDownloadUrl().split("/");
        return segments[segments.length - 1];
    }

    private static File loadCsarFromClasspath(final String fileToLocate) {
        File file = new File(fileToLocate);
        if (!file.exists()) {
            LOGGER.error("The file {} does not exist", file.getAbsolutePath());
            throw new TestRuntimeException(String.format("The file %s does not exist", file.getAbsolutePath()));
        }
        if (file.isFile()) {
            LOGGER.info("Full path to file is: {}", file.getAbsolutePath());
            return file;
        } else {
            LOGGER.info("Searching classpath for {}", fileToLocate);
            ClassLoader classLoader = OnboardingAcceptanceTestSteps.class.getClassLoader();
            LOGGER.info("Full path to file is: {}", file.getAbsolutePath());
            return new File(requireNonNull(classLoader.getResource(fileToLocate).getFile()));
        }
    }

    public static TestPackageData getTestPackageData(String filePath) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            URL resource = Resources.getResource(filePath);
            return mapper.readValue(resource, TestPackageData.class);
        } catch (IOException e) {
            LOGGER.error("Failed parsing of file", e);
            throw new TestRuntimeException(e.getMessage());
        }
    }

    public static String getFileFromResources(String resourceName) {
        try {
            URL url = Resources.getResource(resourceName);
            return Resources.toString(url, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Error loading file", e); // NOSONAR
        }
    }

    public static OnboardApp getOnboardApp(OnboardApp defaultOnboardPackageJson) {
        String externalCsar = System.getProperty(EXTERNAL_CSAR_URI);
        if (Strings.isNullOrEmpty(externalCsar)) {
            return defaultOnboardPackageJson;
        } else {
            try {
                ObjectMapper mapper = new ObjectMapper();
                ResponseEntity<String> remoteOnboardJson = httpGetCall(externalCsar, String.class);
                return mapper.readValue(remoteOnboardJson.getBody(), OnboardApp.class);
            } catch (JsonProcessingException e) {
                throw new TestRuntimeException("Cannot parse OnboardApp object", e);
            }
        }
    }

    private static File getFileOverHttp(String url, String pathToFile) {
        File tempFile;
        try {
            tempFile = new File(pathToFile);
            tempFile.createNewFile(); // NOSONAR
            FileUtils.copyURLToFile(new URL(url), tempFile);
            return tempFile;
        } catch (IOException e) {
            LOGGER.error("Failed to download remote csar file from URL: " + url, e);
            throw new TestRuntimeException(e.getMessage());
        }
    }

    private static String getCsarDownloadingURL(String pathToCsar) {
        String csarDownloadUrl = pathToCsar.substring(pathToCsar.lastIndexOf('/') + 1);
        String jsonAsString = getFileFromResources(TESTING_CSARS_FILE);
        JSONObject jsonObject = new JSONObject(jsonAsString);

        return jsonObject.getString(csarDownloadUrl);
    }

    private static File downloadRemoteCsar(OnboardApp app) {
        String pathToCsar = app.getCsarDownloadUrl();
        String downloadUrl = getCsarDownloadingURL(pathToCsar);
        LOGGER.info("Downloading {} from {}", pathToCsar, downloadUrl);
        return requireNonNull(getFileOverHttp(downloadUrl, pathToCsar));
    }
}
