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

import java.util.List;

import com.ericsson.amonboardingservice.presentation.services.PageableFactory;
import com.ericsson.amonboardingservice.utils.Constants;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ericsson.amonboardingservice.api.VnfPackagesV2Api;
import com.ericsson.amonboardingservice.model.VnfPkgInfoV2;
import com.ericsson.amonboardingservice.presentation.exceptions.PackageNotFoundException;
import com.ericsson.amonboardingservice.presentation.services.vnfpackageservice.VnfPackageService;
import com.google.common.base.Strings;

import lombok.extern.slf4j.Slf4j;

import static com.ericsson.amonboardingservice.utils.UrlUtils.createPaginationHeaders;

@Slf4j
@RestController
@RequestMapping("/api/vnfpkgm/v2")
@Tag(name = "vnf_packages", description = "The VNF packages API")
public class VnfPackageControllerV2Impl implements VnfPackagesV2Api {

    @Autowired
    private VnfPackageService vnfPackageService;
    @Autowired
    private PageableFactory pageableFactory;

    @Override
    public ResponseEntity<List<VnfPkgInfoV2>> vnfPackagesGet(final String accept,
                                                             final String filter,
                                                             final String allFields,
                                                             final String fields,
                                                             final String excludeFields,
                                                             final Boolean excludeDefault,
                                                             final String nextpageOpaqueMarker,
                                                             final Integer size) {
        Pageable pageable = pageableFactory.createPageable(nextpageOpaqueMarker, size);
        Page<VnfPkgInfoV2> vnfPkgInfoPage;
        if (Strings.isNullOrEmpty(filter)) {
            vnfPkgInfoPage = vnfPackageService.listVnfPackagesV2(pageable);
        } else {
            vnfPkgInfoPage = vnfPackageService.listVnfPackagesV2(filter, pageable);
        }
        return new ResponseEntity<>(vnfPkgInfoPage.getContent(), createPaginationHeaders(vnfPkgInfoPage), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<VnfPkgInfoV2> vnfPackagesVnfPkgIdGet(final String vnfPkgId, final String accept) {
        VnfPkgInfoV2 vnfPkgInfo = vnfPackageService.getVnfPackageV2(vnfPkgId)
                .orElseThrow(() -> new PackageNotFoundException(String.format(Constants.VNF_PACKAGE_WITH_ID_DOES_NOT_EXIST, vnfPkgId)));
        return new ResponseEntity<>(vnfPkgInfo, HttpStatus.OK);
    }
}
