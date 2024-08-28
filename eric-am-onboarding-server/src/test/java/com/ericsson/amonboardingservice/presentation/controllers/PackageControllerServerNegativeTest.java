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
package com.ericsson.amonboardingservice.presentation.controllers;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.ericsson.amonboardingservice.infrastructure.client.RestClient;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.Files.newInputStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class PackageControllerServerNegativeTest extends AbstractDbSetupTest {

    private static final String PACKAGE_CONTENT_URI = "/api/v1/packages";
    private static final String PACKAGE_CONTENTS = "PackageContents";
    private static final String ZIP_CONTENTS = "csar.zip";

    @TempDir
    public Path folder;

    @MockBean
    private FileService fileService;

    @Autowired
    private MockMvc mockMvc;

    @Mock
    RestClient restClient;

    private MvcResult mvcResult;

    private MockMultipartFile packageContents;

    @BeforeEach
    public void setUp() throws Exception {
        packageContents = new MockMultipartFile(PACKAGE_CONTENTS, createInputStream(ZIP_CONTENTS));
    }

    @Test
    public void handleFailureToCreateDirectory() throws Exception {
        when(fileService.createDirectory(anyString())).thenThrow(new InternalRuntimeException("Failed to create "
                + "directory"));
        getMvcResultWithExpectedStatus(packageContents, 500);
    }

    @Test
    public void handleFailureToStorePackage() throws Exception {
        final Path directory = folder;
        when(fileService.createDirectory(anyString())).thenReturn(directory);
        when(fileService.storeFile(any(MultipartFile.class), any(), any())).thenThrow(new InternalRuntimeException("Failed to store "
                + "package"));
        getMvcResultWithExpectedStatus(packageContents, 500);
    }

    private MvcResult getMvcResultWithExpectedStatus(MockMultipartFile vnfPackage, int httpStatus) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders
                .multipart(PACKAGE_CONTENT_URI).file(vnfPackage))
                      .andExpect(status().is(httpStatus)).andReturn();
    }

    private static InputStream createInputStream(String fileName) throws URISyntaxException, IOException {
        return newInputStream(getResource(fileName));
    }

    private static Path getResource(String resourceName) throws URISyntaxException {
        return Paths.get(Resources
                .getResource(resourceName).toURI());
    }
}
