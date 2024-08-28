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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppPackageRepository extends JpaRepository<AppPackage, Integer>, JpaSpecificationExecutor<AppPackage> {

    Optional<AppPackage> findByPackageId(String packageId);

    @Query("SELECT appPackage FROM AppPackage appPackage "
            + "WHERE "
            + "appPackage.packageId = :packageId "
            + "AND (appPackage.isSetForDeletion is null "
            + "OR appPackage.isSetForDeletion = false)")
    Optional<AppPackage> findByPackageIdNotBeingDeleted(@Param("packageId") String packageId);

    @Query("SELECT appPackage FROM AppPackage appPackage "
            + "WHERE "
            + "appPackage.onboardingState IN ("
            + "    com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage$OnboardingStateEnum.UPLOADING,"
            + "    com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage$OnboardingStateEnum.PROCESSING) "
            + "AND "
            + "appPackage.onboardingDetail.expiredOnboardingTime < :expiredOnboardingTime")
    List<AppPackage> findAllNotFinishedWithExpirationBefore(@Param("expiredOnboardingTime") LocalDateTime expiredOnboardingTime);

    @Query("SELECT appPackage FROM AppPackage appPackage "
            + "WHERE "
            + "appPackage.isSetForDeletion is null "
            + "OR appPackage.isSetForDeletion = false")
    Page<AppPackage> findAllIsSetForDeletionFalse(Pageable pageable);

    @Query("SELECT appPackage FROM AppPackage appPackage "
            + "WHERE "
            + "(appPackage.isSetForDeletion is null "
            + "OR appPackage.isSetForDeletion = false) "
            + "AND appPackage.errorDetails is null")
    Page<AppPackage> findAllIsSetForDeletionFalseAndErrorDetailsNull(Pageable pageable);

    @Query("SELECT DISTINCT pk.productName FROM AppPackage pk WHERE pk.productName LIKE concat('%',?1,'%')")
    List<String> findDistinctProductName(String productName, Pageable pageable);

    @Query("SELECT DISTINCT pk.provider FROM AppPackage pk WHERE pk.provider LIKE concat('%',?1,'%')")
    List<String> findDistinctProvider(String provider, Pageable pageable);

    @Query("SELECT DISTINCT pk.descriptorVersion FROM AppPackage pk WHERE pk.descriptorVersion LIKE concat('%',?1,'%')")
    List<String> findDistinctDescriptorVersion(String descriptorVersion, Pageable pageable);

    @Query("SELECT DISTINCT pk.softwareVersion FROM AppPackage pk WHERE pk.softwareVersion LIKE concat('%',?1,'%')")
    List<String> findDistinctSoftwareVersion(String softwareVersion, Pageable pageable);

    @Query("SELECT DISTINCT pk.isMultipleVnfd FROM AppPackage pk WHERE pk.packageId = ?1")
    boolean findIsMultipleVnfdFileForPackageId(String packageId);

    @Query("SELECT DISTINCT pk.vnfdZip FROM AppPackage pk WHERE pk.packageId = ?1")
    byte[] findVnfdZipForPackageId(String packageId);

    @Query("SELECT appPackage FROM AppPackage appPackage left join appPackage.onboardingDetail.onboardingHeartbeat as "
            + "onboardingHeartbeat "
            + "WHERE "
            + "appPackage.onboardingState IN ("
            + "    com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage$OnboardingStateEnum.UPLOADING,"
            + "    com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage$OnboardingStateEnum.PROCESSING) "
            + "AND "
            + "(onboardingHeartbeat IS NULL "
            + "OR "
            + "onboardingHeartbeat.latestUpdateTime < :permittedLastUpdateTime)")
    List<AppPackage> findAllNotFinishedWithoutProcessing(@Param("permittedLastUpdateTime") LocalDateTime permittedLastUpdateTime);

    List<AppPackage> findAllByIsSetForDeletionIsTrue();

    @Transactional
    void deleteByPackageId(String packageId);

    boolean existsByIsSetForDeletion(boolean isSetForDeletion);
    boolean existsByOnboardingState(AppPackage.OnboardingStateEnum onboardingState);
}
