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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import com.amazonaws.services.s3.AmazonS3;
@SpringBootTest(classes = {
        ObjectStorageEventListener.class,
        ObjectStorageServiceImpl.class
})
@TestPropertySource(properties = {
        "onboarding.highAvailabilityMode=false",
        "objectStorage.bucket=cvnfm-test-onboarding" })
public class ObjectStorageEventListenerNonHAmodeTest {

    private static final String BUCKET = "cvnfm-test-onboarding";

    @MockBean
    private AmazonS3 amazonS3;

    @Test()
    public void checkAndMakeBucket() {
        verify(amazonS3, never()).doesBucketExistV2(BUCKET);
        verify(amazonS3, never()).createBucket(BUCKET);
    }
}
