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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.SetBucketLifecycleConfigurationRequest;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.ericsson.amonboardingservice.presentation.exceptions.ObjectStorageException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ObjectStorageServiceImpl implements ObjectStorageService {

    private static final int PART_SIZE = 10 * 1024 * 1024; // 10MB

    @Autowired(required = false)
    private AmazonS3 amazonS3;

    @Value("${objectStorage.bucket}")
    private String bucket;

    @Value("${objectStorage.transfer.parallelism}")
    private int transferParallelism;

    @Override
    public void checkAndMakeBucket() {
        LOGGER.info("Checking whether bucket {} exist.", bucket);
        boolean exist;
        exist = amazonS3.doesBucketExistV2(bucket);
        if (!exist) {
            LOGGER.info("Bucket {} does not exist, creating it.", bucket);
            BucketLifecycleConfiguration configuration = new BucketLifecycleConfiguration();
            BucketLifecycleConfiguration.Rule rule = new BucketLifecycleConfiguration.Rule();
            rule.withExpirationInDays(1);
            configuration.setRules(Collections.singletonList(rule));
            SetBucketLifecycleConfigurationRequest request = new SetBucketLifecycleConfigurationRequest(bucket, configuration);

            amazonS3.createBucket(bucket);
            amazonS3.setBucketLifecycleConfiguration(request);
            LOGGER.info("Created bucket {} with lifecycle rule: 1 day expiration date for Objects.", bucket);
        } else {
            LOGGER.info("Bucket: {} already exists.", bucket);
        }
    }

    @Override
    public void uploadFile(InputStream inputStream, String filePath) {
        String uploadId = null;
        try {
            LOGGER.info("Uploading file {} to Object Storage bucket {}.", filePath, bucket);
            InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucket, filePath);
            InitiateMultipartUploadResult initResponse = amazonS3.initiateMultipartUpload(initRequest);
            uploadId = initResponse.getUploadId();

            List<PartETag> partETags = uploadParts(inputStream, filePath, uploadId);

            CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest(bucket, filePath, uploadId, partETags);
            amazonS3.completeMultipartUpload(completeMultipartUploadRequest);
            LOGGER.info("Completed uploading file {} to Object Storage bucket {}.", filePath, bucket);
        } catch (Exception e) {
            if (Objects.nonNull(uploadId)) {
                amazonS3.abortMultipartUpload(new AbortMultipartUploadRequest(bucket, filePath, uploadId));
            }
            throw new ObjectStorageException(String.format("Error while uploading file %s to Object Storage bucket %s due to %s.",
                                                           filePath, bucket, e.getMessage()));
        }
    }

    @Override
    public void uploadFile(File file, String filePath) {
        LOGGER.info("Uploading file {} to Object Storage bucket {}.", filePath, bucket);
        TransferManager transferManager = null;
        Upload upload = null;
        try {
            transferManager = buildTransferManager();
            PutObjectRequest request = new PutObjectRequest(bucket, filePath, file);
            ProgressListener listener = progressEvent ->
                    LOGGER.debug("Uploading bytes to Object Storage for package {}: {}.", filePath, progressEvent.getBytesTransferred());
            request.setGeneralProgressListener(listener);
            upload = transferManager.upload(request);
            upload.waitForCompletion();
            LOGGER.info("Completed uploading file {} to Object Storage bucket {}.", filePath, bucket);
        } catch (Exception e) { //NOSONAR
            if (Objects.nonNull(upload)) {
                upload.abort();
            }
            throw new ObjectStorageException(String.format("Error while uploading file %s to Object Storage bucket %s due to %s.",
                                                           filePath, bucket, e.getMessage()));
        } finally {
            if (Objects.nonNull(transferManager)) {
                transferManager.shutdownNow(false);
            }
        }
    }

    @Override
    public void downloadFile(File storeTo, String filePath) {
        LOGGER.info("Downloading file {} from bucket {} to {}.", filePath, bucket, storeTo.getAbsoluteFile());
        TransferManager transferManager = null;
        Download download = null;
        try {
            transferManager = buildTransferManager();
            ProgressListener listener = progressEvent ->
                    LOGGER.debug("Transferred bytes from Object Storage for package {}: {}.", filePath, progressEvent.getBytesTransferred());
            GetObjectRequest request = new GetObjectRequest(bucket, filePath);
            request.setGeneralProgressListener(listener);
            download = transferManager.download(request, storeTo);
            download.waitForCompletion();
            LOGGER.info("Completed downloading file {} from Object Storage bucket {} to {}.", filePath, bucket, storeTo.getAbsoluteFile());
        } catch (Exception e) { //NOSONAR
            tryAbort(download, filePath);
            throw new ObjectStorageException(String.format("Error while downloading file %s from Object Storage bucket %s due to %s",
                                                           filePath, bucket, e.getMessage()));
        } finally {
            if (Objects.nonNull(transferManager)) {
                transferManager.shutdownNow(false);
            }
        }
    }

    @Override
    public void deleteFile(String filePath) {
        LOGGER.info("Checking whether file {} exists in Object Storage.", filePath);
        if (amazonS3.doesObjectExist(bucket, filePath)) {
            LOGGER.info("Removing file {} from Object Storage bucket {}.", filePath, bucket);
            DeleteObjectRequest request = new DeleteObjectRequest(bucket, filePath);
            amazonS3.deleteObject(request);
            LOGGER.info("Completed removing file {} from Object Storage bucket {}.", filePath, bucket);
        } else {
            LOGGER.warn("File {} does not exist in Object Storage, nothing to remove. If key is lost and file is still in Object Storage,"
                                + "it will be automatically removed in 1 day.",
                        filePath);
        }
    }

    TransferManager buildTransferManager() {
        return TransferManagerBuilder.standard()
                .withS3Client(amazonS3)
                .withMultipartUploadThreshold((long) PART_SIZE)
                .withExecutorFactory(() -> Executors.newFixedThreadPool(transferParallelism))
                .build();
    }

    private List<PartETag> uploadParts(InputStream inputStream, String filePath, String uploadId) throws IOException {
        List<PartETag> partETags = new ArrayList<>();
        byte[] buffer = new byte[PART_SIZE];
        int partNumber = 1;
        int bytesRead;
        while ((bytesRead = fillBuffer(inputStream, buffer)) > 0) {
            try (ByteArrayInputStream byteStream = new ByteArrayInputStream(buffer, 0, bytesRead)) {
                UploadPartRequest uploadRequest = new UploadPartRequest()
                        .withBucketName(bucket)
                        .withKey(filePath)
                        .withUploadId(uploadId)
                        .withPartNumber(partNumber++)
                        .withPartSize(bytesRead)
                        .withInputStream(byteStream);
                UploadPartResult uploadResult = amazonS3.uploadPart(uploadRequest);
                partETags.add(uploadResult.getPartETag());
            }
        }
        return partETags;
    }

    private void tryAbort(Download download, String fileName) {
        try {
            if (Objects.nonNull(download)) {
                download.abort();
            }
        } catch (IOException ex) {
            LOGGER.error("Failed to abort download of {} due to {}", fileName, ex.getMessage());
        }
    }

    private int fillBuffer(InputStream inputStream, byte[] buffer) throws IOException {
        int totalBytesRead = 0;
        int bytesReadLength;
        boolean canBeRead = true;
        while (canBeRead) {
            bytesReadLength = inputStream.read(buffer, totalBytesRead, buffer.length - totalBytesRead);
            if (bytesReadLength > 0) {
                totalBytesRead += bytesReadLength;
            }
            canBeRead = (buffer.length != totalBytesRead && bytesReadLength > 0);
        }
        return totalBytesRead;
    }
}
