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
package com.ericsson.amonboardingservice.api;

import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

import static com.ericsson.amonboardingservice.utilities.AccTestUtils.delay;
import static com.ericsson.amonboardingservice.utilities.AccTestUtils.isRunLocal;
import static com.ericsson.amonboardingservice.utilities.CsarFileUtils.getCsarPackage;
import static com.ericsson.amonboardingservice.utilities.CsarFileUtils.getOnboardApp;
import static com.ericsson.amonboardingservice.utilities.RestUtils.COOKIE;
import static com.ericsson.amonboardingservice.utilities.RestUtils.JSESSIONID_TEMPLATE;
import static com.ericsson.amonboardingservice.utilities.RestUtils.TOKEN;
import static com.ericsson.amonboardingservice.utilities.RestUtils.httpDeleteCall;
import static com.ericsson.amonboardingservice.utilities.RestUtils.httpGetCall;
import static com.ericsson.amonboardingservice.utilities.RestUtils.httpPostCall;
import static com.ericsson.amonboardingservice.utilities.RestUtils.httpPutCall;
import static com.ericsson.amonboardingservice.utilities.TestConstants.HOST;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.ericsson.amonboardingservice.model.CreateVnfPkgInfoRequest;
import com.ericsson.amonboardingservice.model.OnboardApp;
import com.ericsson.amonboardingservice.model.VnfPkgInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class VnfPackagesClient {
    public static final String PACKAGE_CONTENT_URI = "/package_content";

    private static final String VNF_PACKAGE_URI = "/api/vnfpkgm/v1/vnf_packages";
    private static final String VNF_PACKAGE_URL = HOST + VNF_PACKAGE_URI;
    private static final String HELMFILE_SUFFIX_URL = "/helmfile";
    private static final String ARTIFACT_URI = "/artifacts";

    private VnfPackagesClient() {
    }

    public static ResponseEntity<List<VnfPkgInfo>> getVnfPackages() {
        return httpGetCall(VNF_PACKAGE_URL, new ParameterizedTypeReference<>() {
        });
    }

    public static ResponseEntity<VnfPkgInfo> getVnfPackage(String vnfPackageId) {
        final String vnfPackageUrl = VNF_PACKAGE_URL + File.separator + vnfPackageId;
        return httpGetCall(vnfPackageUrl, VnfPkgInfo.class);
    }

    public static ResponseEntity<byte[]> getHelmfile(String vnfPackageId) {
        final String vnfPackageUrl = VNF_PACKAGE_URL + File.separator + vnfPackageId + HELMFILE_SUFFIX_URL;
        return httpGetCall(vnfPackageUrl, byte[].class);
    }

    public static VnfPkgInfo postVnfPackage(CreateVnfPkgInfoRequest createVnfPkgInfoRequest) {
        return httpPostCall(VNF_PACKAGE_URL, createVnfPkgInfoRequest, VnfPkgInfo.class);
    }


    public static ResponseEntity<String> putVnfPackageUsingInputStream(String vnfPkgId, OnboardApp etsiA1PackageToOnboard) {
        OnboardApp onboardApp = getOnboardApp(etsiA1PackageToOnboard);
        FileSystemResource csarPackage = getCsarPackage(onboardApp);
        LOGGER.info("CSAR package loaded from {}.", csarPackage.getPath());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(APPLICATION_OCTET_STREAM);
        if (isRunLocal()) {
            httpHeaders.set(COOKIE, String.format(JSESSIONID_TEMPLATE, TOKEN));
        }
        final String onboardingUri = VNF_PACKAGE_URL + File.separator + vnfPkgId + PACKAGE_CONTENT_URI;
        LOGGER.info("Onboarding Csar {} to {}", csarPackage.getFilename(), onboardingUri);
        HttpEntity<FileSystemResource> requestEntity = new HttpEntity<>(csarPackage, httpHeaders);
        return httpPutCall(onboardingUri, requestEntity, String.class);
    }

    public static ResponseEntity<String> putVnfPackage(String vnfPkgId, OnboardApp etsiA1PackageToOnboard, boolean isLocalResource) {
        OnboardApp onboardApp = getOnboardApp(etsiA1PackageToOnboard);
        FileSystemResource csarPackage = getCsarPackage(onboardApp, isLocalResource);
        LOGGER.info("CSAR package loaded from {}.", csarPackage.getPath());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MULTIPART_FORM_DATA);
        if (isRunLocal()) {
            httpHeaders.set(COOKIE, String.format(JSESSIONID_TEMPLATE, TOKEN));
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", csarPackage);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, httpHeaders);

        return httpPutCall(VNF_PACKAGE_URL + File.separator + vnfPkgId +
                PACKAGE_CONTENT_URI, requestEntity, String.class);
    }

    public static ResponseEntity<String> putVnfPackage(String vnfPkgId, OnboardApp etsiA1PackageToOnboard) {
        return putVnfPackage(vnfPkgId, etsiA1PackageToOnboard, false);
    }


    public static ResponseEntity<String> deleteVnfPackage(VnfPkgInfo pkgInfo) {
        final String packageDeletionUrl = VNF_PACKAGE_URL + File.separator + pkgInfo.getId();
        ResponseEntity<String> deleteResponse = httpDeleteCall(packageDeletionUrl, String.class);

        delay(6000);
        return deleteResponse;
    }

    public static ResponseEntity<String> getArtifact(String vnfPackageId, String artifactPath, MediaType acceptType) {
        final String fullArtifactUrl = VNF_PACKAGE_URL + File.separator + vnfPackageId + ARTIFACT_URI + File.separator + artifactPath;
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAccept(Collections.singletonList(acceptType));
        return httpGetCall(fullArtifactUrl, httpHeaders, String.class);
    }
}
