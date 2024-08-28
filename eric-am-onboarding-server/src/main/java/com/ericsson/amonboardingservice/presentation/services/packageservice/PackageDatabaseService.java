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

import static java.lang.String.format;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.amonboardingservice.presentation.exceptions.PackageNotFoundException;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.utils.Constants;

@Component
public class PackageDatabaseService {

    @Autowired
    private AppPackageRepository appPackageRepository;

    public AppPackage getAppPackageById(String packageId) {
        return appPackageRepository.findByPackageId(packageId)
                .orElseThrow(() -> new PackageNotFoundException(format(Constants.PACKAGE_NOT_PRESENT_ERROR_MESSAGE, packageId)));
    }

    public AppPackage getAppPackageByIdNotBeingDeleted(String packageId) {
        return appPackageRepository.findByPackageIdNotBeingDeleted(packageId)
                .orElseThrow(() -> new PackageNotFoundException(format(Constants.PACKAGE_NOT_PRESENT_ERROR_MESSAGE, packageId)));
    }

    public AppPackage save(AppPackage appPackage) {
        return appPackageRepository.save(appPackage);
    }
}
