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

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.ericsson.amonboardingservice.model.AppUsageStateRequest;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackageInstance;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AppPackageInstanceMapper {

    @Mapping(target = "instanceId", source = "vnfId")
    AppPackageInstance toAppPackageInstance(AppUsageStateRequest appUsageStateRequest);

}
