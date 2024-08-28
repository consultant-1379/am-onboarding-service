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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.amonboardingservice.TestUtils.createInputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.SetBucketLifecycleConfigurationRequest;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.ericsson.amonboardingservice.presentation.exceptions.ObjectStorageException;

@SpringBootTest(classes = {
        ObjectStorageServiceImpl.class,
})
@TestPropertySource(properties = {
        "objectStorage.bucket=cvnfm-test-onboarding" })
public class ObjectStorageServiceImplTest {

    private static final String BUCKET = "cvnfm-test-onboarding";

    @SpyBean
    private ObjectStorageServiceImpl objectStorageService;

    @MockBean
    private AmazonS3 amazonS3;

    @Test
    public void testCheckAndMakeBucketOnContextRefresh() {
        when(amazonS3.doesBucketExistV2(BUCKET)).thenReturn(false);
        final ArgumentCaptor<SetBucketLifecycleConfigurationRequest> lifecycleConfigurationRequestArgumentCaptor = ArgumentCaptor.forClass(
                SetBucketLifecycleConfigurationRequest.class);

        objectStorageService.checkAndMakeBucket();

        verify(amazonS3, times(1)).createBucket(BUCKET);
        verify(amazonS3, times(1)).setBucketLifecycleConfiguration(lifecycleConfigurationRequestArgumentCaptor.capture());
        SetBucketLifecycleConfigurationRequest actualLifecycleConfigurationRequest = lifecycleConfigurationRequestArgumentCaptor.getValue();
        assertThat(actualLifecycleConfigurationRequest.getBucketName()).isEqualTo(BUCKET);
        assertThat(actualLifecycleConfigurationRequest.getLifecycleConfiguration()).isNotNull();
        assertThat(actualLifecycleConfigurationRequest.getLifecycleConfiguration().getRules().get(0).getExpirationInDays()).isEqualTo(1);
    }

    @Test
    public void testCheckAndMakeBucketOnContextRefreshWhenBucketIsAlreadyCreated() {
        when(amazonS3.doesBucketExistV2(BUCKET)).thenReturn(true);

        objectStorageService.checkAndMakeBucket();

        verify(amazonS3, never()).createBucket(BUCKET);
        verify(amazonS3, never()).setBucketLifecycleConfiguration(any(SetBucketLifecycleConfigurationRequest.class));
    }

    @Test
    public void testUploadFileFromFileSuccessfully() throws InterruptedException {
        TransferManager transferManagerMock = mock(TransferManager.class);
        Upload upload = mock(Upload.class);
        ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        when(objectStorageService.buildTransferManager()).thenReturn(transferManagerMock);
        when(transferManagerMock.upload(any())).thenReturn(upload);

        objectStorageService.uploadFile(new File("."), "my-file.zip");

        verify(transferManagerMock).upload(putObjectRequestArgumentCaptor.capture());
        verify(transferManagerMock).shutdownNow(false);

        PutObjectRequest putObjectRequest = putObjectRequestArgumentCaptor.getValue();
        assertThat(putObjectRequest.getBucketName()).isEqualTo(BUCKET);
        assertThat(putObjectRequest.getKey()).isEqualTo("my-file.zip");
        assertThat(putObjectRequest.getGeneralProgressListener()).isNotNull();

        verify(upload).waitForCompletion();
    }

    @Test
    public void testUploadFileFromFileWhenFailsToUpload() throws InterruptedException {
        TransferManager transferManagerMock = mock(TransferManager.class);
        Upload upload = mock(Upload.class);
        ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        when(objectStorageService.buildTransferManager()).thenReturn(transferManagerMock);
        when(transferManagerMock.upload(any())).thenThrow(new AmazonServiceException("Failed to upload content."));

        assertThatThrownBy(() -> objectStorageService.uploadFile(new File("."), "my-file.zip"))
                .isInstanceOf(ObjectStorageException.class);

        verify(transferManagerMock).upload(putObjectRequestArgumentCaptor.capture());
        verify(transferManagerMock).shutdownNow(false);

        PutObjectRequest putObjectRequest = putObjectRequestArgumentCaptor.getValue();
        assertThat(putObjectRequest.getBucketName()).isEqualTo(BUCKET);
        assertThat(putObjectRequest.getKey()).isEqualTo("my-file.zip");
        assertThat(putObjectRequest.getGeneralProgressListener()).isNotNull();

        verify(upload, never()).waitForCompletion();
        verify(upload, never()).abort();
    }

    @Test
    public void testUploadFileFromFileWhenFailsToCompleteUpload() throws InterruptedException {
        TransferManager transferManagerMock = mock(TransferManager.class);
        Upload upload = mock(Upload.class);
        ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        when(objectStorageService.buildTransferManager()).thenReturn(transferManagerMock);
        when(transferManagerMock.upload(any())).thenReturn(upload);
        doThrow(new AmazonServiceException("Failed to complete upload.")).when(upload).waitForCompletion();

        assertThatThrownBy(() -> objectStorageService.uploadFile(new File("."), "my-file.zip"))
                .isInstanceOf(ObjectStorageException.class);

        verify(transferManagerMock).upload(putObjectRequestArgumentCaptor.capture());
        verify(transferManagerMock).shutdownNow(false);

        PutObjectRequest putObjectRequest = putObjectRequestArgumentCaptor.getValue();
        assertThat(putObjectRequest.getBucketName()).isEqualTo(BUCKET);
        assertThat(putObjectRequest.getKey()).isEqualTo("my-file.zip");
        assertThat(putObjectRequest.getGeneralProgressListener()).isNotNull();

        verify(upload).waitForCompletion();
        verify(upload).abort();
    }

    @Test
    public void testUploadFileFromInputStreamSuccessfully() throws URISyntaxException, IOException {
        String uploadId = "my-upload-id";
        String fileName = "my-file.zip";
        try (InputStream csarInputStream = createInputStream("csar.zip")) {
            InitiateMultipartUploadResult initiateMultipartUploadResult = mock(InitiateMultipartUploadResult.class);
            UploadPartResult uploadPartResult = mock(UploadPartResult.class);

            ArgumentCaptor<InitiateMultipartUploadRequest> initiateMultipartUploadRequestArgumentCaptor = ArgumentCaptor.forClass(
                    InitiateMultipartUploadRequest.class);
            ArgumentCaptor<UploadPartRequest> uploadPartRequestArgumentCaptor = ArgumentCaptor.forClass(UploadPartRequest.class);
            ArgumentCaptor<CompleteMultipartUploadRequest> completeMultipartUploadRequestArgumentCaptor = ArgumentCaptor.forClass(
                    CompleteMultipartUploadRequest.class);

            when(amazonS3.initiateMultipartUpload(any())).thenReturn(initiateMultipartUploadResult);
            when(amazonS3.uploadPart(any())).thenReturn(uploadPartResult);
            when(initiateMultipartUploadResult.getUploadId()).thenReturn(uploadId);

            objectStorageService.uploadFile(csarInputStream, fileName);

            verify(amazonS3, times(1)).initiateMultipartUpload(initiateMultipartUploadRequestArgumentCaptor.capture());
            // File is less than 5MB so uploadPart() will be invoked only once
            verify(amazonS3, times(1)).uploadPart(uploadPartRequestArgumentCaptor.capture());
            verify(amazonS3, times(1)).completeMultipartUpload(completeMultipartUploadRequestArgumentCaptor.capture());
            verify(amazonS3, never()).abortMultipartUpload(any());

            InitiateMultipartUploadRequest initiateMultipartUploadRequest = initiateMultipartUploadRequestArgumentCaptor.getValue();
            assertThat(initiateMultipartUploadRequest.getBucketName()).isEqualTo(BUCKET);
            assertThat(initiateMultipartUploadRequest.getKey()).isEqualTo(fileName);

            UploadPartRequest uploadPartRequest = uploadPartRequestArgumentCaptor.getValue();
            assertThat(uploadPartRequest.getBucketName()).isEqualTo(BUCKET);
            assertThat(uploadPartRequest.getInputStream()).isNotEmpty();
            assertThat(uploadPartRequest.getPartNumber()).isEqualTo(1);
            assertThat(uploadPartRequest.getPartSize()).isNotZero();
            assertThat(uploadPartRequest.getUploadId()).isEqualTo(uploadId);

            CompleteMultipartUploadRequest completeMultipartUploadRequest = completeMultipartUploadRequestArgumentCaptor.getValue();
            assertThat(completeMultipartUploadRequest.getBucketName()).isEqualTo(BUCKET);
            assertThat(completeMultipartUploadRequest.getKey()).isEqualTo(fileName);
            assertThat(completeMultipartUploadRequest.getUploadId()).isEqualTo(uploadId);
        }
    }

    @Test
    public void testUploadFileFromInputStreamWhenInitialUploadFails() throws URISyntaxException, IOException {
        String fileName = "my-file.zip";
        try (InputStream csarInputStream = createInputStream("csar.zip")) {
            ArgumentCaptor<InitiateMultipartUploadRequest> initiateMultipartUploadRequestArgumentCaptor = ArgumentCaptor.forClass(
                    InitiateMultipartUploadRequest.class);

            when(amazonS3.initiateMultipartUpload(any())).thenThrow(new AmazonServiceException("Could not initialize upload."));

            assertThatThrownBy(() -> objectStorageService.uploadFile(csarInputStream, fileName))
                    .isInstanceOf(ObjectStorageException.class);

            verify(amazonS3, times(1)).initiateMultipartUpload(initiateMultipartUploadRequestArgumentCaptor.capture());
            verify(amazonS3, never()).uploadPart(any());
            verify(amazonS3, never()).completeMultipartUpload(any());
            verify(amazonS3, never()).abortMultipartUpload(any());

            InitiateMultipartUploadRequest initiateMultipartUploadRequest = initiateMultipartUploadRequestArgumentCaptor.getValue();
            assertThat(initiateMultipartUploadRequest.getBucketName()).isEqualTo(BUCKET);
            assertThat(initiateMultipartUploadRequest.getKey()).isEqualTo(fileName);
        }
    }

    @Test
    public void testUploadFileFromInputStreamWhenCompleteUploadFails() throws URISyntaxException, IOException {
        String uploadId = "my-upload-id";
        String fileName = "my-file.zip";
        try (InputStream csarInputStream = createInputStream("csar.zip")) {
            InitiateMultipartUploadResult initiateMultipartUploadResult = mock(InitiateMultipartUploadResult.class);
            UploadPartResult uploadPartResult = mock(UploadPartResult.class);

            ArgumentCaptor<InitiateMultipartUploadRequest> initiateMultipartUploadRequestArgumentCaptor = ArgumentCaptor.forClass(
                    InitiateMultipartUploadRequest.class);
            ArgumentCaptor<UploadPartRequest> uploadPartRequestArgumentCaptor = ArgumentCaptor.forClass(UploadPartRequest.class);
            ArgumentCaptor<CompleteMultipartUploadRequest> completeMultipartUploadRequestArgumentCaptor = ArgumentCaptor.forClass(
                    CompleteMultipartUploadRequest.class);
            ArgumentCaptor<AbortMultipartUploadRequest> abortMultipartUploadRequestArgumentCaptor = ArgumentCaptor.forClass(
                    AbortMultipartUploadRequest.class);

            when(amazonS3.initiateMultipartUpload(any())).thenReturn(initiateMultipartUploadResult);
            when(initiateMultipartUploadResult.getUploadId()).thenReturn(uploadId);
            when(amazonS3.uploadPart(any())).thenReturn(uploadPartResult);
            when(amazonS3.completeMultipartUpload(any())).thenThrow(new AmazonServiceException("Could not finish upload."));

            assertThatThrownBy(() -> objectStorageService.uploadFile(csarInputStream, fileName))
                    .isInstanceOf(ObjectStorageException.class);

            verify(amazonS3, times(1)).initiateMultipartUpload(initiateMultipartUploadRequestArgumentCaptor.capture());
            // File is less than 5MB so uploadPart() will be invoked only once
            verify(amazonS3, times(1)).uploadPart(uploadPartRequestArgumentCaptor.capture());
            verify(amazonS3, times(1)).completeMultipartUpload(completeMultipartUploadRequestArgumentCaptor.capture());
            verify(amazonS3, times(1)).abortMultipartUpload(abortMultipartUploadRequestArgumentCaptor.capture());

            InitiateMultipartUploadRequest initiateMultipartUploadRequest = initiateMultipartUploadRequestArgumentCaptor.getValue();
            assertThat(initiateMultipartUploadRequest.getBucketName()).isEqualTo(BUCKET);
            assertThat(initiateMultipartUploadRequest.getKey()).isEqualTo(fileName);

            UploadPartRequest uploadPartRequest = uploadPartRequestArgumentCaptor.getValue();
            assertThat(uploadPartRequest.getBucketName()).isEqualTo(BUCKET);
            assertThat(uploadPartRequest.getInputStream()).isNotEmpty();
            assertThat(uploadPartRequest.getPartNumber()).isEqualTo(1);
            assertThat(uploadPartRequest.getPartSize()).isNotZero();
            assertThat(uploadPartRequest.getUploadId()).isEqualTo(uploadId);

            CompleteMultipartUploadRequest completeMultipartUploadRequest = completeMultipartUploadRequestArgumentCaptor.getValue();
            assertThat(completeMultipartUploadRequest.getBucketName()).isEqualTo(BUCKET);
            assertThat(completeMultipartUploadRequest.getKey()).isEqualTo(fileName);
            assertThat(completeMultipartUploadRequest.getUploadId()).isEqualTo(uploadId);

            AbortMultipartUploadRequest abortIncompleteMultipartUpload = abortMultipartUploadRequestArgumentCaptor.getValue();
            assertThat(abortIncompleteMultipartUpload.getBucketName()).isEqualTo(BUCKET);
            assertThat(abortIncompleteMultipartUpload.getKey()).isEqualTo(fileName);
            assertThat(abortIncompleteMultipartUpload.getUploadId()).isEqualTo(uploadId);
        }
    }

    @Test
    public void testUploadFileFromInputStreamWhenUploadFails() throws URISyntaxException, IOException {
        String uploadId = "my-upload-id";
        String fileName = "my-file.zip";
        try (InputStream csarInputStream = createInputStream("csar.zip")) {
            InitiateMultipartUploadResult initiateMultipartUploadResult = mock(InitiateMultipartUploadResult.class);

            ArgumentCaptor<InitiateMultipartUploadRequest> initiateMultipartUploadRequestArgumentCaptor = ArgumentCaptor.forClass(
                    InitiateMultipartUploadRequest.class);
            ArgumentCaptor<UploadPartRequest> uploadPartRequestArgumentCaptor = ArgumentCaptor.forClass(UploadPartRequest.class);
            ArgumentCaptor<AbortMultipartUploadRequest> abortMultipartUploadRequestArgumentCaptor = ArgumentCaptor.forClass(
                    AbortMultipartUploadRequest.class);

            when(amazonS3.initiateMultipartUpload(any())).thenReturn(initiateMultipartUploadResult);
            when(initiateMultipartUploadResult.getUploadId()).thenReturn(uploadId);
            when(amazonS3.uploadPart(any())).thenThrow(new AmazonServiceException("Failed to upload part."));

            assertThatThrownBy(() -> objectStorageService.uploadFile(csarInputStream, fileName))
                    .isInstanceOf(ObjectStorageException.class);

            verify(amazonS3, times(1)).initiateMultipartUpload(initiateMultipartUploadRequestArgumentCaptor.capture());
            // File is less than 5MB so uploadPart() will be invoked only once
            verify(amazonS3, times(1)).uploadPart(uploadPartRequestArgumentCaptor.capture());
            verify(amazonS3, never()).completeMultipartUpload(any());
            verify(amazonS3, times(1)).abortMultipartUpload(abortMultipartUploadRequestArgumentCaptor.capture());

            InitiateMultipartUploadRequest initiateMultipartUploadRequest = initiateMultipartUploadRequestArgumentCaptor.getValue();
            assertThat(initiateMultipartUploadRequest.getBucketName()).isEqualTo(BUCKET);
            assertThat(initiateMultipartUploadRequest.getKey()).isEqualTo(fileName);

            UploadPartRequest uploadPartRequest = uploadPartRequestArgumentCaptor.getValue();
            assertThat(uploadPartRequest.getBucketName()).isEqualTo(BUCKET);
            assertThat(uploadPartRequest.getInputStream()).isNotEmpty();
            assertThat(uploadPartRequest.getPartNumber()).isEqualTo(1);
            assertThat(uploadPartRequest.getPartSize()).isNotZero();
            assertThat(uploadPartRequest.getUploadId()).isEqualTo(uploadId);

            AbortMultipartUploadRequest abortIncompleteMultipartUpload = abortMultipartUploadRequestArgumentCaptor.getValue();
            assertThat(abortIncompleteMultipartUpload.getBucketName()).isEqualTo(BUCKET);
            assertThat(abortIncompleteMultipartUpload.getKey()).isEqualTo(fileName);
            assertThat(abortIncompleteMultipartUpload.getUploadId()).isEqualTo(uploadId);
        }
    }

    @Test
    public void testDownloadFileSuccessfully() throws InterruptedException, IOException {
        File mockFile = mock(File.class);
        TransferManager transferManagerMock = mock(TransferManager.class);
        String fileName = "my-file.zip";
        ArgumentCaptor<GetObjectRequest> getObjectRequestArgumentCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);
        Download download = mock(Download.class);

        when(objectStorageService.buildTransferManager()).thenReturn(transferManagerMock);
        when(transferManagerMock.download(any(GetObjectRequest.class), eq(mockFile))).thenReturn(download);

        objectStorageService.downloadFile(mockFile, fileName);

        verify(transferManagerMock).download(getObjectRequestArgumentCaptor.capture(), eq(mockFile));
        verify(download).waitForCompletion();
        verify(download, never()).abort();

        GetObjectRequest getObjectRequest = getObjectRequestArgumentCaptor.getValue();
        assertThat(getObjectRequest.getBucketName()).isEqualTo(BUCKET);
        assertThat(getObjectRequest.getKey()).isEqualTo(fileName);
        assertThat(getObjectRequest.getGeneralProgressListener()).isNotNull();
    }

    @Test
    public void testDownloadFileWhenDownloadFails() throws InterruptedException, IOException {
        File mockFile = mock(File.class);
        TransferManager transferManagerMock = mock(TransferManager.class);
        String fileName = "my-file.zip";
        ArgumentCaptor<GetObjectRequest> getObjectRequestArgumentCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);
        Download download = mock(Download.class);

        when(objectStorageService.buildTransferManager()).thenReturn(transferManagerMock);
        when(transferManagerMock.download(any(GetObjectRequest.class), eq(mockFile))).thenThrow(new AmazonServiceException("Failed to "
                                                                                                                                   +
                                                                                                                                   "download file."));
        assertThatThrownBy(() -> objectStorageService.downloadFile(mockFile, fileName))
                .isInstanceOf(ObjectStorageException.class);

        verify(transferManagerMock).download(getObjectRequestArgumentCaptor.capture(), eq(mockFile));
        verify(download, never()).waitForCompletion();
        verify(download, never()).abort();

        GetObjectRequest getObjectRequest = getObjectRequestArgumentCaptor.getValue();
        assertThat(getObjectRequest.getBucketName()).isEqualTo(BUCKET);
        assertThat(getObjectRequest.getKey()).isEqualTo(fileName);
        assertThat(getObjectRequest.getGeneralProgressListener()).isNotNull();
    }

    @Test
    public void testDownloadFileWhenCompleteDownloadFails() throws InterruptedException, IOException {
        File mockFile = mock(File.class);
        TransferManager transferManagerMock = mock(TransferManager.class);
        String fileName = "my-file.zip";
        ArgumentCaptor<GetObjectRequest> getObjectRequestArgumentCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);
        Download download = mock(Download.class);

        when(objectStorageService.buildTransferManager()).thenReturn(transferManagerMock);
        when(transferManagerMock.download(any(GetObjectRequest.class), eq(mockFile))).thenReturn(download);
        doThrow(new AmazonServiceException("Failed to complete download.")).when(download).waitForCompletion();

        assertThatThrownBy(() -> objectStorageService.downloadFile(mockFile, fileName))
                .isInstanceOf(ObjectStorageException.class);

        verify(transferManagerMock).download(getObjectRequestArgumentCaptor.capture(), eq(mockFile));
        verify(download).waitForCompletion();
        verify(download).abort();

        GetObjectRequest getObjectRequest = getObjectRequestArgumentCaptor.getValue();
        assertThat(getObjectRequest.getBucketName()).isEqualTo(BUCKET);
        assertThat(getObjectRequest.getKey()).isEqualTo(fileName);
        assertThat(getObjectRequest.getGeneralProgressListener()).isNotNull();
    }

    @Test
    public void testDeleteFileSuccessfully() {
        when(amazonS3.doesObjectExist(any(), any())).thenReturn(true);

        objectStorageService.deleteFile("my-file.zip");

        final ArgumentCaptor<DeleteObjectRequest> deleteObjectRequestArgumentCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);

        verify(amazonS3, (times(1))).deleteObject(deleteObjectRequestArgumentCaptor.capture());
        DeleteObjectRequest deleteObjectRequest = deleteObjectRequestArgumentCaptor.getValue();
        assertThat(deleteObjectRequest.getBucketName()).isEqualTo(BUCKET);
        assertThat(deleteObjectRequest.getKey()).isEqualTo("my-file.zip");
    }

    @Test
    public void testIgnoreDeleteFileWhenItDoesNotExist() {
        when(amazonS3.doesObjectExist(any(), any())).thenReturn(false);

        objectStorageService.deleteFile("my-file.zip");

        verify(amazonS3, (never())).deleteObject(any());
    }
}