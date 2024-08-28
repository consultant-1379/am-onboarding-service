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
package com.ericsson.amonboardingservice.api;

import com.ericsson.amonboardingservice.model.ImageResponse;
import org.springframework.http.ResponseEntity;

import static com.ericsson.amonboardingservice.utilities.RestUtils.httpGetCall;
import static com.ericsson.amonboardingservice.utilities.TestConstants.BASE_URI;
import static com.ericsson.amonboardingservice.utilities.TestConstants.HOST;

public final class ImagesClient {
    private static final String IMAGES_URI = "/images";
    private static final String ALL_IMAGE_URI = HOST + BASE_URI + IMAGES_URI;

    private ImagesClient() {
    }

    public static ResponseEntity<ImageResponse> getAllImages() {
        return httpGetCall(ALL_IMAGE_URI, ImageResponse.class);
    }
}
