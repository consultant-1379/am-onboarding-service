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
package com.ericsson.amonboardingservice.presentation.repositories;

import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import java.util.Collections;
import java.util.Map;

@Repository
@Transactional(readOnly = true)
@Slf4j
public class AppPackageWithoutAssociationsRepository extends PartialEntityRepository<AppPackage, String> {
    @Override
    protected Map<String, JoinType> initAssociations(Root<AppPackage> root) {
        return Collections.emptyMap();
    }

    @Override
    protected Path<String> getIdField(Root<AppPackage> root) {
        return root.get("packageId");
    }
}
