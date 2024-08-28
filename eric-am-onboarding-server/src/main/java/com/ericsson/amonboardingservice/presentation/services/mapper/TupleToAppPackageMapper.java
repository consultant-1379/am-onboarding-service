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

import com.ericsson.amonboardingservice.presentation.models.vnfd.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.Root;
import java.util.*;

@Service
@Slf4j
public class TupleToAppPackageMapper implements TupleToEntityMapper<AppPackage> {
    @Override
    public List<AppPackage> mapAll(final List<Tuple> tuples, Root<AppPackage> root) {
        return tuples.stream().map(el -> map(el, root)).toList();
    }

    @Override
    public AppPackage map(final Tuple tuple, Root<AppPackage> root) {
        AppPackage pkg = new AppPackage();
        pkg.setPackageId(tuple.get(root.get("packageId")));
        pkg.setDescriptorId(tuple.get(root.get("descriptorId")));
        pkg.setDescriptorVersion(tuple.get(root.get("descriptorVersion")));
        pkg.setProvider(tuple.get(root.get("provider")));
        pkg.setProductName(tuple.get(root.get("productName")));
        pkg.setSoftwareVersion(tuple.get(root.get("softwareVersion")));
        pkg.setChecksum(tuple.get(root.get("checksum")));
        pkg.setOnboardingState(tuple.get(root.get("onboardingState")));
        pkg.setUsageState(tuple.get(root.get("usageState")));
        pkg.setOperationalState(tuple.get(root.get("operationalState")));
        pkg.setErrorDetails(tuple.get(root.get("errorDetails")));
        pkg.setMultipleVnfd(tuple.get(root.get("isMultipleVnfd")));
        pkg.setForDeletion(tuple.get(root.get("isSetForDeletion")));
        pkg.setPackageSecurityOption(tuple.get(root.get("packageSecurityOption")));
        return pkg;
    }
}
