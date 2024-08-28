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

import com.ericsson.amonboardingservice.api.PackagesApi;
import com.ericsson.amonboardingservice.model.AdditionalPropertyResponse;
import com.ericsson.amonboardingservice.model.AppPackageList;
import com.ericsson.amonboardingservice.model.AppPackageResponse;
import com.ericsson.amonboardingservice.model.OperationDetailResponse;
import com.ericsson.amonboardingservice.model.ServiceModelRecordResponse;
import com.ericsson.amonboardingservice.presentation.exceptions.DataNotFoundException;
import com.ericsson.amonboardingservice.presentation.exceptions.ServiceModelRecordNotFoundException;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.services.PageableFactory;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageService;
import com.ericsson.amonboardingservice.presentation.services.servicemodelservice.ServiceModelService;
import com.ericsson.amonboardingservice.presentation.services.supportedoperationservice.SupportedOperationService;
import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.ericsson.amonboardingservice.presentation.services.servicemodelservice.ServiceModelServiceImpl.SERVICE_MODEL_NOT_FOUND_MESSAGE;
import static com.ericsson.amonboardingservice.utils.UrlUtils.createPaginationHeaders;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "packages", description = "The packages API")
public class PackageControllerImpl implements PackagesApi {

    @Autowired
    private PackageService packageService;
    @Autowired
    private ServiceModelService serviceModelService;
    @Autowired
    private SupportedOperationService supportedOperationService;
    @Autowired
    private PageableFactory pageableFactory;

    @Override
    public ResponseEntity<AppPackageList> packagesGet(String filter, String nextpageOpaqueMarker, Integer size) {
        Pageable pageable = pageableFactory.createPageable(nextpageOpaqueMarker, size);
        Page<AppPackageResponse> appPackageResponsePage;
        if (Strings.isNullOrEmpty(filter)) {
            appPackageResponsePage = packageService.listPackages(pageable);
        } else {
            appPackageResponsePage = packageService.listPackages(filter, pageable);
        }

        for (AppPackageResponse pkg : appPackageResponsePage.getContent()) {
            setSupportedOperations(pkg);
        }
        AppPackageList appPackageList = new AppPackageList();
        appPackageList.setPackages(appPackageResponsePage.getContent());

        return new ResponseEntity<>(appPackageList, createPaginationHeaders(appPackageResponsePage), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<AppPackageResponse> packagesIdGet(@PathVariable("id") final String id) {
        AppPackageResponse appPackageResponse = packageService
                .getPackage(id)
                .orElseThrow(() -> new DataNotFoundException("Package with id: \"" + id + "\" not found"));
        appPackageResponse.setSupportedOperations(
                supportedOperationService.getSupportedOperations(appPackageResponse.getAppPkgId())
        );
        return new ResponseEntity<>(appPackageResponse, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<AdditionalPropertyResponse>> packagesIdOperationAdditionalParametersGet(final String id,
                                                                                                       final String operation,
                                                                                                       final String targetDescriptorId) {
        List<AdditionalPropertyResponse> additionalPropertyList =
                packageService.getAdditionalParamsForOperationType(id, operation, targetDescriptorId);
        return new ResponseEntity<>(additionalPropertyList, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> packagesIdDelete(final String id) {
        return null;
    }

    @Override
    public ResponseEntity<String> packagesIdStatusGet(final String id) {
        return null;
    }

    @Override
    public ResponseEntity<List<OperationDetailResponse>> packagesIdSupportedOperationsGet(final String id) {
        List<OperationDetailResponse> supportedOperations = supportedOperationService.getSupportedOperations(id);
        return new ResponseEntity<>(supportedOperations, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ServiceModelRecordResponse> packagesIdServiceModelGet(final String id) {
        ServiceModelRecordResponse serviceModelRecordResponse = serviceModelService.getServiceModelResponseByPackageId(id)
                .orElseThrow(() -> new ServiceModelRecordNotFoundException(String.format(SERVICE_MODEL_NOT_FOUND_MESSAGE, id)));

        return new ResponseEntity<>(serviceModelRecordResponse, HttpStatus.OK);
    }

    private void setSupportedOperations(final AppPackageResponse pkg) {
        if (isOnboardedState(pkg)) {
            pkg.setSupportedOperations(supportedOperationService.getSupportedOperations(pkg.getAppPkgId()));
        }
    }

    private static boolean isOnboardedState(final AppPackageResponse pkg) {
        return AppPackage.OnboardingStateEnum.ONBOARDED.toString().equals(pkg.getOnboardingState());
    }
}
