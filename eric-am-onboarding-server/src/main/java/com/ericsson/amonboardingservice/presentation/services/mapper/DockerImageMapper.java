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
package com.ericsson.amonboardingservice.presentation.services.mapper;

import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackageDockerImage;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DockerImageMapper {

    List<Object> toImagesURL(List<AppPackageDockerImage> appPackageDockerImages);

    default Object toImagesURL(AppPackageDockerImage appPackageDockerImage) {
        return appPackageDockerImage.getImageId();
    }
}
