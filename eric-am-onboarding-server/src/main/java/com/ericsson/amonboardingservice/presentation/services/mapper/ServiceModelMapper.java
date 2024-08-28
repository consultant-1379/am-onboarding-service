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

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.ericsson.amonboardingservice.model.ServiceModelRecordResponse;
import com.ericsson.amonboardingservice.presentation.models.ServiceModelRecordEntity;
import com.google.common.base.Strings;

@Mapper(componentModel = "spring")
public interface ServiceModelMapper {

    @Mapping(target = "packageId", source = "appPackage.packageId")
    ServiceModelRecordResponse toServiceModelRecordResponse(ServiceModelRecordEntity serviceModelRecordEntity);

    @AfterMapping
    default void toServiceModelRecordResponse(ServiceModelRecordEntity serviceModelRecordEntity,
                                              @MappingTarget ServiceModelRecordResponse serviceModelRecordResponse) {
        serviceModelRecordResponse.setPackageId(Strings.emptyToNull(serviceModelRecordEntity.getAppPackage().getPackageId()));
    }
}
