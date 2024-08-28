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
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackageInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface AppPackageInstanceRepository extends JpaRepository<AppPackageInstance, String> {

    Optional<AppPackageInstance> findByAppPackageAndInstanceId(AppPackage appPackage, String vnfInstanceId);

    @Transactional
    long deleteByAppPackageAndInstanceId(AppPackage appPackage, String vnfInstanceId);
}
