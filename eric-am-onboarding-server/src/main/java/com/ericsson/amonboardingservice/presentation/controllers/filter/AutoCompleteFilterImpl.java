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
package com.ericsson.amonboardingservice.presentation.controllers.filter;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ericsson.amonboardingservice.api.AutoCompleteFilterApi;
import com.ericsson.amonboardingservice.model.AutoCompleteResponse;
import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageService;
import com.ericsson.amonboardingservice.utils.Constants;

@RestController
@RequestMapping("/api/v1")
public class AutoCompleteFilterImpl implements AutoCompleteFilterApi {

    private static final Pattern NUMBER_EXPRESSION = Pattern.compile("\\d+(\\.\\d+)?");

    @Autowired
    private PackageService packageService;

    @Override
    public ResponseEntity<AutoCompleteResponse> getAutoCompleteValue(String type,
                                                                     String softwareVersion,
                                                                     String packageVersion,
                                                                     String provider,
                                                                     String pageNumber,
                                                                     String pageSize) {
        int pgSize;
        int pgNumber;
        if (!NUMBER_EXPRESSION.matcher(pageNumber).find()) {
            throw new IllegalArgumentException("pageNumber only supports number value");
        } else {
            pgNumber = Integer.parseInt(pageNumber);
        }
        if (!NUMBER_EXPRESSION.matcher(pageSize).find()) {
            throw new IllegalArgumentException("pageSize only supports number value");
        } else {
            pgSize =  Integer.parseInt(pageSize);
        }
        CompletableFuture<List<String>> allType;
        CompletableFuture<List<String>> allSoftwareVersion;
        CompletableFuture<List<String>> allPackageVersion;
        CompletableFuture<List<String>> allProvider;
        if (type == null && softwareVersion == null && packageVersion == null && provider == null) {
            allType = packageService.getAutoCompleteResponse(Constants.TYPE, "", pgNumber, pgSize);
            allSoftwareVersion = packageService.getAutoCompleteResponse(Constants.SOFTWARE_VERSION, "", pgNumber, pgSize);
            allPackageVersion = packageService.getAutoCompleteResponse(Constants.PACKAGE_VERSION, "", pgNumber, pgSize);
            allProvider = packageService.getAutoCompleteResponse(Constants.PROVIDER, "", pgNumber, pgSize);
        } else {
            allType = packageService.getAutoCompleteResponse(Constants.TYPE, type, pgNumber, pgSize);
            allSoftwareVersion = packageService.getAutoCompleteResponse(Constants.SOFTWARE_VERSION, softwareVersion, pgNumber,
                    pgSize);
            allPackageVersion = packageService.getAutoCompleteResponse(Constants.PACKAGE_VERSION, packageVersion, pgNumber,
                    pgSize);
            allProvider = packageService.getAutoCompleteResponse(Constants.PROVIDER, provider, pgNumber, pgSize);
        }
        CompletableFuture.allOf(allType, allSoftwareVersion, allPackageVersion, allProvider).join();
        AutoCompleteResponse autoCompleteResponse = new AutoCompleteResponse();
        try {
            autoCompleteResponse.setType(allType.get());
            autoCompleteResponse.setPackageVersion(allPackageVersion.get());
            autoCompleteResponse.setProvider(allProvider.get());
            autoCompleteResponse.setSoftwareVersion(allSoftwareVersion.get());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            throw new InternalRuntimeException(ex);
        }
        return new ResponseEntity<>(autoCompleteResponse, HttpStatus.OK);
    }
}
