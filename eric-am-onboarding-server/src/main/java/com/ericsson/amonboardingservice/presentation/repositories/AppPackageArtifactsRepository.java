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
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackageArtifacts;

public interface AppPackageArtifactsRepository extends JpaRepository<AppPackageArtifacts, String> {

    List<AppPackageArtifacts> findByAppPackage(AppPackage appPackage);

    Optional<AppPackageArtifacts> findByAppPackageAndArtifactPath(AppPackage appPackage, String artifactPath);
}
