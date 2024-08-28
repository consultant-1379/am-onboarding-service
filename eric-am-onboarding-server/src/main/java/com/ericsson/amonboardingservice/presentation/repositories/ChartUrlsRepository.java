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

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.amonboardingservice.presentation.models.ChartUrlsEntity;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;

@Repository
public interface ChartUrlsRepository extends JpaRepository<ChartUrlsEntity, Integer> {
    List<ChartUrlsEntity> findByChartsRegistryUrl(String chartsRegistryUrl);

    @Transactional
    @Modifying
    int deleteByAppPackage(AppPackage appPackage);

    List<ChartUrlsEntity> findByAppPackage(AppPackage appPackage);
}

