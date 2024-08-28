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
package com.ericsson.amonboardingservice.presentation.services.fileservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.amonboardingservice.presentation.exceptions.InsufficientDiskSpaceException;
import com.ericsson.amonboardingservice.presentation.exceptions.UnsupportedMediaTypeException;
import com.ericsson.amonboardingservice.presentation.services.FileTypeDetector;
import com.ericsson.amonboardingservice.utils.executor.ProcessExecutor;

@SpringBootTest(classes = {
        FileValidator.class,
        FileTypeDetector.class
})
@MockBean(classes = {ProcessExecutor.class})
public class FileValidatorTest {
    @TempDir
    public Path folder;

    @Autowired
    private FileValidator fileValidator;

    @MockBean
    private FileService fileService;

    @Test
    public void isEnoughSpaceInRootDirectoryShouldReturnTrue() throws IOException {
        MultipartFile file = buildMultipartFileForPreUploadValidation(createEmptyZip(folder.toString()));
        when(fileService.isEnoughSpaceInRootDirectory(eq(file.getSize()))).thenReturn(true);

        boolean actual = fileValidator.isEnoughSpaceInRootDirectory(file);

        assertThat(actual).isTrue();

        verify(fileService).isEnoughSpaceInRootDirectory(eq(file.getSize()));
    }

    @Test
    public void preUploadValidationShouldBeSuccessful() throws IOException {
        MultipartFile file = buildMultipartFileForPreUploadValidation(createEmptyZip(folder.toString()));
        when(fileService.isEnoughSpaceInRootDirectory(eq(file.getSize()))).thenReturn(true);

        assertThatNoException().isThrownBy(() -> fileValidator.validateFileOnPreUpload(file));
    }

    @Test
    public void preUploadValidationShouldFailForUnsupportedMediaTypeException() throws IOException {
        MultipartFile file = buildMultipartFileForPreUploadValidation("dummy-content".getBytes());
        when(fileService.isEnoughSpaceInRootDirectory(eq(file.getSize()))).thenReturn(true);

        assertThatExceptionOfType(UnsupportedMediaTypeException.class)
                .isThrownBy(() -> fileValidator.validateFileOnPreUpload(file))
                .withMessage("Provided Invalid Content type : application/zip. Valid content type that is currently supported is MediaType.ZIP");
    }

    @Test
    public void preUploadValidationInputStreamShouldThrowException() throws Exception {
        MockMultipartFile file = buildMultipartFileForPreUploadValidation(createEmptyZip(folder.toString()));
        InputStream is = file.getInputStream();
        long tooBigFileSize = folder.getRoot().toFile().getFreeSpace();
        assertThatExceptionOfType(InsufficientDiskSpaceException.class)
                .isThrownBy(() -> fileValidator.validateFileOnPreUpload(is, tooBigFileSize))
                .withMessage("There is not enough space on disk to upload file");
    }

    private MockMultipartFile buildMultipartFileForPreUploadValidation(byte[] content) throws IOException {

        return new MockMultipartFile("any", content);
    }

    private byte[] createEmptyZip(String path){
        byte[] emptyZip={80,75,05,06,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00};

        try{
            OutputStream fos = Files.newOutputStream(Path.of(path));
            fos.write(emptyZip, 0, 22);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return emptyZip;
    }
}
