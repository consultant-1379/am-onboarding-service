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
package com.ericsson.amonboardingservice.presentation.services.packageservice;

import com.ericsson.am.shared.vnfd.model.VnfDescriptorDetails;
import com.ericsson.amonboardingservice.model.AdditionalPropertyResponse;
import com.ericsson.amonboardingservice.model.AppPackageResponse;
import com.ericsson.amonboardingservice.model.CreateVnfPkgInfoRequest;
import com.ericsson.amonboardingservice.model.VnfPkgInfo;
import com.ericsson.amonboardingservice.presentation.models.ChartUrlsEntity;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for managing Onboarded package information.
 */
public interface PackageService {

    /**
     * Save Package information.
     *
     * @param appPackage Package details to be saved.
     * @return the package Id
     */
    AppPackage savePackage(AppPackage appPackage);

    /**
     * List a page of Packages that have been onboarded.
     */
    Page<AppPackageResponse> listPackages(Pageable pageable);

    /**
     * List all Packages that matches the filter.
     */
    Page<AppPackageResponse> listPackages(String filter, Pageable pageable);

    /**
     * Retrieve details for a specific onboarded package.
     *
     * @param id
     * @return
     */
    Optional<AppPackageResponse> getPackage(String id);

    /**
     * Save the  package details to the database.
     *
     * @param vnfDescriptorDetails Details from the VNF Descriptor.
     * @param chartUri             A list of chart URIs to the charts in the chart repository.
     * @return the saved package
     */
    AppPackage savePackageDetails(VnfDescriptorDetails vnfDescriptorDetails, List<ChartUrlsEntity> chartUri);

    /**
     * Delete an onboarded package by package id.
     *
     * @param id
     * @return
     */
    void deletePackage(String id);

    void deleteChartsByPackageResponse(Optional<AppPackageResponse> appPackageResponse);

    CompletableFuture<List<String>> getAutoCompleteResponse(String parameterName, String value, int pageNumber, int pageSize);

    /***
     * To create a new Vnf Package
     *
     * @param createVnfPkgInfoRequest
     * @return
     */
    VnfPkgInfo createVnfPackage(CreateVnfPkgInfoRequest createVnfPkgInfoRequest);

    /**
     * Returns a AppPackage instance with the updated state
     *
     * @param onboardingPackageState
     * @param appPackage
     * @return AppPackage
     */
    AppPackage updatePackageState(OnboardingPackageState onboardingPackageState, AppPackage appPackage);

    /**
     * Updates AppPackage instance on exception handling. Suppresses new exceptions if one happens
     *
     * @param onboardingPackageState
     * @param packageId
     * @param errorDetails
     * @param throwable              that cause packageState update
     * @return AppPackage
     */
    void updatePackageStateOnException(OnboardingPackageState onboardingPackageState, String packageId, String errorDetails,
                                       Throwable throwable);

    /**
     * Lists all packages associated with the specified chartUrl
     *
     * @param chartsRegistryUrl
     * @return list of AppPackage
     */
    List<AppPackage> listPackagesWithChartUrl(String chartsRegistryUrl);

    /**
     * This method is used to onboard a VNF package
     */
    void packageUpload(String vnfPkgId, MultipartFile file);

    void packageUpload(String vnfPkgId, InputStream inputStream, long fileSize);

    /**
     * This method is used to onboard a VNF package asynchronously
     *
     * @param originalFilename
     * @param packageContents
     * @param packageId
     * @param timeoutDate
     */
    void asyncPackageUpload(String originalFilename, Path packageContents, String packageId, LocalDateTime timeoutDate);

    void asyncPackageUploadFromObjectStorage(String filename, String originalFilename, String packageId, LocalDateTime localDateTime);

    /**
     * Store the uploaded file onto the filesystem
     *
     * @param file the file being uploaded
     * @return the Path to the stored file.
     */
    Path storePackage(MultipartFile file);

    /**
     * Returns the additional parameters from the package for the given operation type
     *
     * @param id                 Package Id
     * @param operation          Operation type
     * @param targetDescriptorId ID of VNFD which is target of rollback operation
     * @return Addition properties
     */
    List<AdditionalPropertyResponse> getAdditionalParamsForOperationType(String id, String operation, String targetDescriptorId);

    /**
     * Get helmfile content by package id
     *
     * @param vnfPkgId id of package
     * @return helmfile content
     */
    byte[] getHelmfileContentByPackageId(String vnfPkgId);

    /**
     * By current package check if container registry is enabled or onboarding health status is in UP state,
     * otherwise fails to delete package
     *
     * @param packageSetForDeletionId Id of the AppPackage with flag isSetForDeletion set to true
     */
    void removeAppPackageWithResources(String packageSetForDeletionId);

    List<AppPackage> getPackagesSetForDeletion();
}
