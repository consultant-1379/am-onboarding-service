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
package com.ericsson.amonboardingservice.presentation.services.vnfpackageservice;

import com.ericsson.amonboardingservice.presentation.services.mapper.AppPackageMapper;
import com.ericsson.amonboardingservice.model.VnfPkgInfo;
import com.ericsson.amonboardingservice.model.VnfPkgInfoV2;
import com.ericsson.amonboardingservice.presentation.exceptions.FailedOnboardingException;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.services.filter.EtsiVnfPackageQuery;
import com.google.common.base.Strings;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.ericsson.amonboardingservice.presentation.services.mapper.AppPackageMapper.toResourcePage;

@Slf4j
@Service
public class VnfPackageServiceImpl implements VnfPackageService {
    @Autowired
    private AppPackageRepository appPackageRepository;

    @Autowired
    private AppPackageMapper appPackageMapper;

    @Autowired
    private EtsiVnfPackageQuery etsiVnfPackageQuery;

    @Override
    @Transactional
    public Optional<VnfPkgInfo> getVnfPackage(final String vnfPkgId) {
        Optional<AppPackage> appPackage = appPackageRepository.findByPackageIdNotBeingDeleted(vnfPkgId);
        if (appPackage.isPresent() && !Strings.isNullOrEmpty(appPackage.get().getErrorDetails())) {
            throw new FailedOnboardingException(appPackage.get().getErrorDetails());
        }
        return appPackage.map(appPackageMapper::toVnfPkgInfo);
    }

    @Override
    @Transactional
    public Page<VnfPkgInfo> listVnfPackages(Pageable pageable) {
        Page<AppPackage> appPackages = appPackageRepository.findAllIsSetForDeletionFalseAndErrorDetailsNull(pageable);

        return toResourcePage(appPackages, appPackageMapper::toVnfPkgInfo);
    }

    @Override
    @Transactional
    public Page<VnfPkgInfo> listVnfPackages(final String filter, Pageable pageable) {
        LOGGER.info("Getting all packages matching filter {}", filter);

        Page<AppPackage> packagePage = etsiVnfPackageQuery.getPageWithFilter(filter, pageable);

        return toResourcePage(packagePage, appPackageMapper::toVnfPkgInfo);
    }

    @Override
    @Transactional
    public Optional<VnfPkgInfoV2> getVnfPackageV2(String vnfPkgId) {
        Optional<AppPackage> appPackage = appPackageRepository.findByPackageIdNotBeingDeleted(vnfPkgId);
        return appPackage.map(appPackageMapper::toVnfPkgInfoV2);
    }

    @Override
    @Transactional
    public Page<VnfPkgInfoV2> listVnfPackagesV2(Pageable pageable) {
        Page<AppPackage> appPackagePage = appPackageRepository.findAllIsSetForDeletionFalse(pageable);

        return toResourcePage(appPackagePage, appPackageMapper::toVnfPkgInfoV2);
    }

    @Override
    @Transactional
    public Page<VnfPkgInfoV2> listVnfPackagesV2(String filter, Pageable pageable) {
        LOGGER.info("Getting all package matching filter {}", filter);

        Page<AppPackage> packagePage = etsiVnfPackageQuery.getPageWithFilter(filter, pageable);

        return toResourcePage(packagePage, appPackageMapper::toVnfPkgInfoV2);

    }

}
