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
package com.ericsson.amonboardingservice.presentation.services.filestorage;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.ericsson.amonboardingservice.presentation.services.fileservice.FileValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.amonboardingservice.presentation.services.FileTypeDetector;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileSystemService;
import com.ericsson.amonboardingservice.presentation.services.objectstorage.ObjectStorageServiceImpl;
import com.ericsson.amonboardingservice.utils.executor.ProcessExecutor;
import com.google.common.io.Resources;

@SpringBootTest(classes = {
      FileStorageServiceImpl.class,
      FileSystemService.class,
      FileValidator.class
})
@MockBean({
      FileTypeDetector.class,
      ProcessExecutor.class
})
@ActiveProfiles("test")
@TestPropertySource(properties = "onboarding.highAvailabilityMode=true")
public class FileStorageServiceImplHAmodeTest {

    private static final String ZIP_CONTENTS = "csar.zip";

    @Autowired
    private FileStorageService fileStorageService;
    @MockBean
    private ObjectStorageServiceImpl objectStorageService;


    @Test
    public void testDeleteFileFromObjectStorage() {
        String filePath = "some/dummy/path/dummy.csar";
        Path dummyPath = Paths.get(filePath);
        fileStorageService.deleteFileFromObjectStorage(dummyPath);
        verify(objectStorageService, times(1)).deleteFile(filePath);
    }

    @Test
    public void testDeleteFileFromObjectStorageWithNull() {
        assertThrows(NullPointerException.class, () -> fileStorageService.deleteFileFromObjectStorage(null));
    }

    @Test
    public void testDeleteFileFromObjectStorageWithEmptyObject() {
        String filePath = "";
        Path dummyPath = Paths.get(filePath);
        fileStorageService.deleteFileFromObjectStorage(dummyPath);
        verify(objectStorageService, times(1)).deleteFile(filePath);
    }


    @Test
    public void testStoreFileWithObjectStorage() throws IOException, URISyntaxException {
        Path dummyDirectoryPath = Paths.get(Resources.getResource(ZIP_CONTENTS).toURI()).getParent();

        MultipartFile mockedMultipartFile = mock(MultipartFile.class);
        String dummyFileName = "dummyName.txt";

        Path result = fileStorageService.storeFile(mockedMultipartFile, dummyDirectoryPath, dummyFileName);
        verify(mockedMultipartFile, times(1)).transferTo(any(File.class));
        verify(objectStorageService, times(1)).uploadFile(any(File.class), any(String.class));

        result.toFile().delete();
    }
}