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
package com.ericsson.amonboardingservice.contracts.base;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.presentation.controllers.filter.AutoCompleteFilterImpl;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageService;
import com.ericsson.amonboardingservice.utils.Constants;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.BDDMockito.given;

@ExtendWith(SpringExtension.class)
public class AutocompletePositiveBase extends AbstractDbSetupTest {

    @Mock
    private PackageService packageService;

    @InjectMocks
    private AutoCompleteFilterImpl autoCompleteFilter;

    @BeforeEach
    public void setup() {
        mockAllType("");
        mockAllPackageVersion("");
        mockAllProvider("");
        mockAllSoftwareVersion("");
        mockAllType("S");
        mockAllPackageVersion(null);
        mockAllProvider(null);
        mockAllSoftwareVersion(null);
        RestAssuredMockMvc.standaloneSetup(autoCompleteFilter);
    }

    private void mockAllType(String value) {
        List<String> allType = new ArrayList<>();
        allType.add("SGSN-MME");
        allType.add("SAPC");
        allType.add("SASN");
        if (value == null) {
            given(packageService.getAutoCompleteResponse(Constants.TYPE, null, 0, 5)).willReturn(
                    CompletableFuture.completedFuture(new ArrayList<>()));
        } else {
            given(packageService.getAutoCompleteResponse(Constants.TYPE, value, 0, 5)).willReturn(
                    CompletableFuture.completedFuture(allType));
        }
    }

    private void mockAllSoftwareVersion(String value) {
        List<String> allSoftwareVersion = new ArrayList<>();
        allSoftwareVersion.add("1.20 (CXS101289_R81E08)");
        if (value == null) {
            given(packageService.getAutoCompleteResponse(Constants.SOFTWARE_VERSION, null, 0, 5))
                    .willReturn(CompletableFuture.completedFuture(new ArrayList<>()));
        } else {
            given(packageService.getAutoCompleteResponse(Constants.SOFTWARE_VERSION, value, 0, 5))
                    .willReturn(CompletableFuture.completedFuture(allSoftwareVersion));
        }
    }

    private void mockAllProvider(String value) {
        List<String> allProvider = new ArrayList<>();
        allProvider.add("Ericsson");
        allProvider.add("Ericsson1");
        allProvider.add("Ericsson2");
        allProvider.add("Ericsson3");
        if (value == null) {
            given(packageService.getAutoCompleteResponse(Constants.PROVIDER, null, 0, 5)).willReturn(
                    CompletableFuture.completedFuture(new ArrayList<>()));
        } else {
            given(packageService.getAutoCompleteResponse(Constants.PROVIDER, value, 0, 5)).willReturn(
                    CompletableFuture.completedFuture(allProvider));
        }
    }

    private void mockAllPackageVersion(String value) {
        List<String> allProvider = new ArrayList<>();
        allProvider.add("cxp9025898_4r81e08");
        allProvider.add("cxp9025898_4r81e09");
        allProvider.add("cxp9025898_4r81e10");
        allProvider.add("cxp9025898_4r81e11");
        if (value == null) {
            given(packageService.getAutoCompleteResponse(Constants.PACKAGE_VERSION, null, 0, 5)).willReturn(
                    CompletableFuture.completedFuture(new ArrayList<>()));
        } else {
            given(packageService.getAutoCompleteResponse(Constants.PACKAGE_VERSION, value, 0, 5)).willReturn(
                    CompletableFuture.completedFuture(allProvider));
        }
    }
}
