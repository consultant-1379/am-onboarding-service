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

import com.ericsson.amonboardingservice.api.ImagesApi;
import com.ericsson.amonboardingservice.model.ImageResponse;
import com.ericsson.amonboardingservice.presentation.services.imageservice.ImageService;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@Tag(name = "images", description = "The images API")
public class ImageControllerImpl implements ImagesApi {

    @Autowired
    ImageService imageService;

    @Value("${container.registry.enabled}")
    private boolean isContainerRegistryEnabled;

    @Override
    public ResponseEntity<ImageResponse> imagesGet() {
        if (!isContainerRegistryEnabled) {
            containerRegistryDisabled();
        }
        LOGGER.debug("Inside get all images");
        ImageResponse imageResponse = imageService.getAllImages();
        LOGGER.debug("Image Response from Server :: {}", imageResponse);
        return new ResponseEntity<>(imageResponse, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ImageResponse> imagesImageNameGet(@PathVariable("imageName") String imageName) {
        if (!isContainerRegistryEnabled) {
            containerRegistryDisabled();
        }
        LOGGER.debug("Image name provided is :: {} ", imageName);
        ImageResponse imageResponse = imageService.getAllImagesByName(imageName);
        LOGGER.debug("Image Response from Server :: {}, with image name ::{}", imageResponse, imageName);
        return new ResponseEntity<>(imageResponse, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ImageResponse> imagesImageNameImageTagGet(@PathVariable("imageTag") String imageTag,
                                                              @PathVariable("imageName")  String imageName) {
        if (!isContainerRegistryEnabled) {
            containerRegistryDisabled();
        }
        LOGGER.debug("Image name provided is :: {} and Tag is ::{}", imageName, imageTag);
        ImageResponse imageResponse = imageService.getAllImagesByNameAndTag(imageName, imageTag);
        LOGGER.debug("Image Response from Server :: {}, with image name ::{} and Image tag :: {}",
                imageResponse, imageName, imageTag);
        return new ResponseEntity<>(imageResponse, HttpStatus.OK);
    }

    private static void containerRegistryDisabled() {
        LOGGER.debug(
            "Container registry has been disabled. Request unsupported. Please check installation configuration.");
        throw new UnsupportedOperationException(
            "Container registry has been disabled. Request unsupported. Please check installation configuration.");
    }
}
