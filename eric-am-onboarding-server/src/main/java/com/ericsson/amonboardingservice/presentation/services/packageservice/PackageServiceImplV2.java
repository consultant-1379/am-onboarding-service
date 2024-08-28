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

import com.ericsson.amonboardingservice.presentation.repositories.AppPackageWithoutAssociationsRepository;
import com.ericsson.amonboardingservice.presentation.services.mapper.AppPackageMapper;
import com.ericsson.amonboardingservice.model.AppPackageResponseV2;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.services.filter.VnfPackageQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.ericsson.amonboardingservice.presentation.services.mapper.AppPackageMapper.toResourcePage;

@Slf4j
@Service
public class PackageServiceImplV2 implements PackageServiceV2 {
    private static final List<String> FIELDS_TO_EXCLUDE_FROM_UI =
            List.of("descriptorModel", "files", "helmfile", "vnfdZip", "chartsRegistryUrl",
                    "appPackageDockerImages", "userDefinedData", "serviceModelRecordEntity",
                    "operationDetails", "onboardingDetail", "appPackageArtifacts");

    @Autowired
    private AppPackageRepository appPackageRepository;

    @Autowired
    private AppPackageMapper appPackageMapper;

    @Autowired
    private VnfPackageQuery vnfPackageQuery;

    @Autowired
    private AppPackageWithoutAssociationsRepository appPackageWithoutAssociationsRepository;

    @Override
    @Transactional
    public Page<AppPackageResponseV2> listPackagesV2(Pageable pageable) {
        return listPackagesV2(PackageResponseVerbosity.DEFAULT, pageable);
    }

    @Override
    @Transactional
    public Page<AppPackageResponseV2> listPackagesV2(PackageResponseVerbosity verbosity, Pageable pageable) {
        LOGGER.debug("Getting all packages with verbosity level {}", verbosity);

        Page<AppPackage> packagePage;
        if (verbosity == PackageResponseVerbosity.UI) {
            packagePage = appPackageWithoutAssociationsRepository.selectPageExcludeFields(
                    AppPackage.class, FIELDS_TO_EXCLUDE_FROM_UI, vnfPackageQuery.isNotSetForDeletion(), pageable);
        } else {
            packagePage = appPackageRepository.findAllIsSetForDeletionFalse(pageable);
        }

        return toResourcePage(packagePage, appPackageMapper::toAppPackageResponseV2);
    }

    @Override
    @Transactional
    public Page<AppPackageResponseV2> listPackagesV2(String filter, Pageable pageable) {
        return listPackagesV2(filter, PackageResponseVerbosity.DEFAULT, pageable);
    }

    @Override
    @Transactional
    public Page<AppPackageResponseV2> listPackagesV2(String filter,
                                                     PackageResponseVerbosity verbosity,
                                                     Pageable pageable) {
        LOGGER.debug("Getting all package matching filter {} with verbosity level {}", filter, verbosity);

        Page<AppPackage> appPackagePage;
        if (verbosity == PackageResponseVerbosity.UI) {
            appPackagePage = appPackageWithoutAssociationsRepository
                    .selectPageExcludeFields(
                            AppPackage.class,
                            FIELDS_TO_EXCLUDE_FROM_UI,
                            getSpecification(filter),
                            pageable);
        } else {
            appPackagePage = vnfPackageQuery.getPageWithFilter(filter, pageable);
        }

        return toResourcePage(appPackagePage, appPackageMapper::toAppPackageResponseV2);
    }

    @Override
    @Transactional
    public Optional<AppPackageResponseV2> getPackageV2(String id) {
        LOGGER.debug("Getting package details for id {}", id);

        return appPackageRepository.findByPackageIdNotBeingDeleted(id).map(appPackageMapper::toAppPackageResponseV2);
    }

    private Specification<AppPackage> getSpecification(String filter) {
        return vnfPackageQuery.getSpecification(filter)
                .and(vnfPackageQuery.isNotSetForDeletion());
    }

}
