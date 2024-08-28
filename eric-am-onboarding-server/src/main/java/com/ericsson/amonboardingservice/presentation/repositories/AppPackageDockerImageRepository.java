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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackageDockerImage;

@Repository
public interface AppPackageDockerImageRepository extends JpaRepository<AppPackageDockerImage, Integer> {

    @Query(value = "SELECT image_id FROM app_docker_images "
            + "JOIN app_packages ON app_docker_images.package_id = app_packages.package_id "
            + "WHERE image_id IN "
            + "    (SELECT image_id FROM app_packages "
            + "     JOIN app_docker_images ON app_packages.package_id = app_docker_images.package_id"
            + "     WHERE app_packages.package_id = :packageId) "
            + "GROUP BY app_docker_images.image_id "
            + "HAVING count(app_docker_images.package_id) = 1", nativeQuery = true)
    List<String> findAllRemovableImagesByPackageId(@Param("packageId") String packageId);

    List<AppPackageDockerImage> findByAppPackage(AppPackage appPackage);
}