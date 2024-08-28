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

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ericsson.amonboardingservice.presentation.models.ServiceModelRecordEntity;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;

@Repository
public interface ServiceModelRecordRepository extends JpaRepository<ServiceModelRecordEntity, String> {

    ServiceModelRecordEntity findByServiceModelId(String serviceModelId);

    Optional<ServiceModelRecordEntity> findByAppPackagePackageId(String packageId);

    Optional<ServiceModelRecordEntity> findByAppPackage(AppPackage appPackage);
}
