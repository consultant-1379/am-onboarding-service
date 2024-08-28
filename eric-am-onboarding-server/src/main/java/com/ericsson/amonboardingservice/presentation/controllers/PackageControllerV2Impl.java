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
package com.ericsson.amonboardingservice.presentation.controllers;

import com.ericsson.amonboardingservice.api.PackagesV2Api;
import com.ericsson.amonboardingservice.model.AppPackageListV2;
import com.ericsson.amonboardingservice.model.AppPackageResponseV2;
import com.ericsson.amonboardingservice.presentation.exceptions.DataNotFoundException;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.services.PageableFactory;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageResponseVerbosity;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageServiceV2;
import com.ericsson.amonboardingservice.presentation.services.supportedoperationservice.SupportedOperationService;
import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.ericsson.amonboardingservice.utils.UrlUtils.createPaginationHeaders;


@RestController
@RequestMapping("/api/v2")
@Tag(name = "packages", description = "The packages API V2")
public class PackageControllerV2Impl implements PackagesV2Api {

    @Autowired
    private PackageServiceV2 packageService;
    @Autowired
    private SupportedOperationService supportedOperationService;
    @Autowired
    private PageableFactory pageableFactory;

    @Override
    public ResponseEntity<AppPackageListV2> packagesGet(String filter,
                                                        String verbosity,
                                                        String nextpageOpaqueMarker,
                                                        Integer size) {
        Pageable pageable = pageableFactory.createPageable(nextpageOpaqueMarker, size);
        Page<AppPackageResponseV2> appPackageResponsePage;
        if (Strings.isNullOrEmpty(verbosity)) {
            appPackageResponsePage = Strings.isNullOrEmpty(filter)
                    ? packageService.listPackagesV2(pageable)
                    : packageService.listPackagesV2(filter, pageable);
            appPackageResponsePage.forEach(this::setSupportedOperations);
        } else {
            PackageResponseVerbosity responseVerbosity = PackageResponseVerbosity.getByValue(verbosity);
            appPackageResponsePage = Strings.isNullOrEmpty(filter) ?
                    packageService.listPackagesV2(responseVerbosity, pageable) :
                    packageService.listPackagesV2(filter, responseVerbosity, pageable);

            if (!PackageResponseVerbosity.UI.equals(responseVerbosity)) {
                appPackageResponsePage.forEach(this::setSupportedOperations);
            }
        }

        AppPackageListV2 appPackageList = new AppPackageListV2();
        appPackageList.setPackages(appPackageResponsePage.getContent());

        return new ResponseEntity<>(appPackageList, createPaginationHeaders(appPackageResponsePage), HttpStatus.OK);

    }

    @Override
    public ResponseEntity<AppPackageResponseV2> packagesIdGet(String id) {
        AppPackageResponseV2 appPackageResponse = packageService
                .getPackageV2(id).orElseThrow(() -> new DataNotFoundException("Package with id: \"" +
                        id + "\" not found"));
        appPackageResponse.setSupportedOperations(supportedOperationService
                .getSupportedOperations(appPackageResponse.getAppPkgId()));
        return new ResponseEntity<>(appPackageResponse, HttpStatus.OK);
    }

    private void setSupportedOperations(final AppPackageResponseV2 pkg) {
        if (isOnboardedState(pkg)) {
            pkg.setSupportedOperations(supportedOperationService.getSupportedOperations(pkg.getAppPkgId()));
        }
    }

    private boolean isOnboardedState(final AppPackageResponseV2 pkg) {
        return AppPackage.OnboardingStateEnum.ONBOARDED.toString().equals(pkg.getOnboardingState());
    }
}
