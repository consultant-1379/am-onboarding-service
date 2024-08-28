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
package com.ericsson.amonboardingservice.presentation.services;

import com.ericsson.amonboardingservice.presentation.services.mapper.AppPackageMapper;
import com.ericsson.amonboardingservice.model.AppPackageResponse;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage.OperationalStateEnum.DISABLED;

@Slf4j
@Service
public class DeleteAppPackageHelper {

    @Autowired
    private AppPackageMapper appPackageMapper;

    @Autowired
    private AppPackageRepository appPackageRepository;

    @Transactional
    public void setAndSaveAppPackageForDeletion(String packageId) {
        Optional<AppPackage> appPackageOptional = appPackageRepository.findByPackageId(packageId);

        appPackageOptional.ifPresent(appPackageForDeletion -> {
            appPackageForDeletion.setForDeletion(true);
            appPackageForDeletion.setOperationalState(DISABLED);
            appPackageRepository.save(appPackageForDeletion);
        });
    }

    @Transactional
    public Optional<AppPackageResponse> getAppPackageResponseById(String id) {
        LOGGER.info("Getting package details for id {}", id);
        return appPackageRepository.findByPackageId(id).map(appPackageMapper::toAppPackageResponse);
    }

}
