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
package com.ericsson.amonboardingservice.presentation.services.packageinstanceservice;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.amonboardingservice.model.AppUsageStateRequest;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackageInstance;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageInstanceRepository;
import com.ericsson.amonboardingservice.presentation.services.mapper.AppPackageInstanceMapper;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageDatabaseService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AppPackageInstanceServiceImpl implements AppPackageInstanceService {

    @Autowired
    private AppPackageInstanceMapper appPackageInstanceMapper;

    @Autowired
    private PackageDatabaseService databaseService;

    @Autowired
    private AppPackageInstanceRepository appPackageInstanceRepository;

    @Override
    public Optional<AppPackageInstance> getPackageInstanceByAppPackageAndInstanceId(AppPackage appPackage, String vnfInstanceId) {
        return appPackageInstanceRepository.findByAppPackageAndInstanceId(appPackage, vnfInstanceId);
    }

    @Override
    public AppPackageInstance savePackageInstance(AppPackageInstance packageInstance) {
        return appPackageInstanceRepository.save(packageInstance);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updatePackageUsageState(String pkgId, AppUsageStateRequest appUsageStateRequest) {
        LOGGER.info("App package Id : {}, is package state 'IN_USE' : {} ", pkgId, appUsageStateRequest.getIsInUse());

        final AppPackage appPackage = databaseService.getAppPackageByIdNotBeingDeleted(pkgId);

        if (appUsageStateRequest.getIsInUse()) {
            Optional<AppPackageInstance> appPackageInstance =
                    getPackageInstanceByAppPackageAndInstanceId(appPackage, appUsageStateRequest.getVnfId());
            if (appPackageInstance.isPresent()) {
                LOGGER.info("Package instance is already present, " +
                        "updating association between package and VNF instance will be skip");
                return;
            }

            AppPackageInstance packageInstance = appPackageInstanceMapper.toAppPackageInstance(appUsageStateRequest);

            packageInstance.setAppPackage(appPackage);
            final AppPackageInstance result = savePackageInstance(packageInstance);

            LOGGER.info("Association between package {} and VNF instance {} was created",
                    result.getAppPackage().getPackageId(),
                    result.getInstanceId());
        } else {
            LOGGER.info("Delete the vnfInstanceId : {} for packageId : {} ", appUsageStateRequest.getVnfId(), pkgId);
            deleteAssociationBetweenPackageAndVnfInstance(appPackage, appUsageStateRequest.getVnfId());
        }
    }

    private void deleteAssociationBetweenPackageAndVnfInstance(AppPackage appPackage, String instanceId) {
        appPackageInstanceRepository.deleteByAppPackageAndInstanceId(appPackage, instanceId);
    }
}
