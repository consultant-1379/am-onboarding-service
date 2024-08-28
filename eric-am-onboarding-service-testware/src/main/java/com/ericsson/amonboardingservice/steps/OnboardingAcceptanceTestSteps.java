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
package com.ericsson.amonboardingservice.steps;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static com.ericsson.amonboardingservice.api.DockerRegistryClient.getManifest;
import static com.ericsson.amonboardingservice.api.HelmChartsClient.getHelmChart;
import static com.ericsson.amonboardingservice.api.HelmChartsClient.getHelmCharts;
import static com.ericsson.amonboardingservice.api.PackageClient.SERVICE_MODEL_KEY;
import static com.ericsson.amonboardingservice.api.PackageClient.getAdditionalParameters;
import static com.ericsson.amonboardingservice.api.PackageClient.getSupportedOperations;
import static com.ericsson.amonboardingservice.api.PackageClient.getUri;
import static com.ericsson.amonboardingservice.api.VnfPackagesClient.getArtifact;
import static com.ericsson.amonboardingservice.api.VnfPackagesClient.getHelmfile;
import static com.ericsson.amonboardingservice.api.VnfPackagesClient.getVnfPackage;
import static com.ericsson.amonboardingservice.api.VnfPackagesClient.getVnfPackages;
import static com.ericsson.amonboardingservice.steps.delete.DeletePackageCommonSteps.deletePackage;
import static com.ericsson.amonboardingservice.steps.delete.DeletePackageVerifier.verifySingleDeletedPackage;
import static com.ericsson.amonboardingservice.utilities.CsarFileUtils.getFileFromResources;
import static com.ericsson.amonboardingservice.utilities.RestUtils.getRequestObjFromJson;
import static com.ericsson.amonboardingservice.utilities.RestUtils.httpGetCallMap;
import static com.ericsson.amonboardingservice.utilities.TestConstants.CREATE_VNFPACKAGE_SKIP_IMAGE_UPLOAD_JSON;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.ericsson.amonboardingservice.api.ImagesClient;
import com.ericsson.amonboardingservice.api.VnfPackagesClient;
import com.ericsson.amonboardingservice.exceptions.TestRuntimeException;
import com.ericsson.amonboardingservice.model.AdditionalPropertyResponse;
import com.ericsson.amonboardingservice.model.CreateVnfPkgInfoRequest;
import com.ericsson.amonboardingservice.model.DockerImage;
import com.ericsson.amonboardingservice.model.HelmPackage;
import com.ericsson.amonboardingservice.model.ImageResponse;
import com.ericsson.amonboardingservice.model.OnboardApp;
import com.ericsson.amonboardingservice.model.OperationDetailResponse;
import com.ericsson.amonboardingservice.model.OperationInfo;
import com.ericsson.amonboardingservice.model.TestChartInfo;
import com.ericsson.amonboardingservice.model.VnfPkgInfo;
import com.ericsson.amonboardingservice.steps.delete.DeletePackageCommonSteps;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class OnboardingAcceptanceTestSteps {

    private static final TestChartInfo SAMPLE_DESCRIPTOR_CHART_0_0_1_223 = new TestChartInfo(
            "sampledescriptor-0.0.1-223.tgz",
            "sampledescriptor",
            "0.0.1-223",
            "CNF"
    );
    private static final TestChartInfo TEST_SCALE_CHART_0_1_0 = new TestChartInfo(
            "test-scale-chart-0.1.0.tgz",
            "test-scale-chart",
            "0.1.0",
            "CNF"
    );
    private static final TestChartInfo TEST_SCALE_CHART_0_2_0 = new TestChartInfo(
            "test-scale-chart-0.2.0.tgz",
            "test-scale-chart",
            "0.2.0",
            "CNF"
    );
    private static final TestChartInfo SPIDER_APP_CHART_INFO_2_207_9 = new TestChartInfo(
            "spider-app-2.207.9.tgz",
            "spider-app",
            "2.207.9",
            "CNF"
    );
    private static final TestChartInfo SPIDER_APP_CHART_INFO_2_208_2 = new TestChartInfo(
            "spider-app-2.208.2.tgz",
            "spider-app",
            "2.208.2",
            "CNF"
    );
    private static final TestChartInfo ERIC_SEC_SIP_TLS_CRD_MULTI_A_2_3_0_7 = new TestChartInfo(
            "eric-sec-sip-tls-crd-multi-a-2.3.0-7.tgz",
            "eric-sec-sip-tls-crd-multi-a",
            "2.3.0-7",
            "CRD"
    );
    private static final TestChartInfo ERIC_SEC_CERTM_CRD_MULTI_A_2_8_0_72 = new TestChartInfo(
            "eric-sec-certm-crd-multi-a-2.8.0-72.tgz",
            "eric-sec-certm-crd-multi-a",
            "2.8.0-72",
            "CRD"
    );
    private static final TestChartInfo SCALE_CRD_1_0_0 = new TestChartInfo(
            "scale-crd-1.0.0.tgz",
            "scale-crd",
            "1.0.0",
            "CRD"
    );

    private OnboardingAcceptanceTestSteps() {
    }

    @Step("List Images with name")
    public static void verifyAllImageUploaded(OnboardApp app) {
        ResponseEntity<ImageResponse> responseEntityImage = ImagesClient.getAllImages();
        LOGGER.info("Response From Server: {}", responseEntityImage.getBody());
        assertEquals(responseEntityImage.getStatusCode(), HttpStatus.OK);
        assertNotNull(responseEntityImage.getBody());
        assertNotNull(responseEntityImage.getBody().getMetadata()); // NOSONAR
        assertEquals(responseEntityImage.getBody().getMetadata().getCount().intValue(), app.getAppImagesCount()); // NOSONAR
    }

    @Step("Verify all onboarded Helm Charts")
    public static void verifyAllHelmCharts(OnboardApp app) {
        LOGGER.info("Executing get all helm charts REST request");
        verifyResponseForHelmCharts(app);
    }

    @Step("Verify successful upload of Helm chart by name")
    public static void verifyHelmChartByName(OnboardApp app) {
        LOGGER.info("Executing get helm chart REST request");
        verifyResponseForHelmChart(app);
    }

    @Step("verify the charts and images have been onboarded")
    public static void verifyUploadedHelmChartsAndImages(final OnboardApp app) {
        verifyAllHelmCharts(app);
        verifyHelmChartByName(app);
        if (!app.isSkipImageValidation()) {
            verifyAllImageUploaded(app);
        } else {
            LOGGER.info("Skipping image validation");
        }
    }

    @Step("Check presence of Docker manifests")
    public static void checkPresenceOfDockerManifests(VnfPkgInfo vnfPkgInfo) {
        vnfPkgInfo.getSoftwareImages().stream()
                .map(image -> new DockerImage(image.getName(), image.getVersion()))
                .forEach(image -> {
                    ResponseEntity<String> responseEntity = getManifest(image);
                    if (HttpStatus.OK != responseEntity.getStatusCode()) {
                        LOGGER.error("Expected presence of docker manifest, but got {}", responseEntity);
                        throw new TestRuntimeException("Docker manifest is not present");
                    }
                });
    }

    @Step("Create a new Vnf Package")
    public static void createVnfPackageAndVerify(final CreateVnfPkgInfoRequest createVnfPkgInfoRequest) {
        final VnfPkgInfo vnfPkgInfo = VnfPackagesClient.postVnfPackage(createVnfPkgInfoRequest);

        assertNotNull(vnfPkgInfo.getId(), "VnfPackageId is null");
        assertEquals(VnfPkgInfo.UsageStateEnum.NOT_IN_USE, vnfPkgInfo.getUsageState());
        assertEquals(VnfPkgInfo.OperationalStateEnum.DISABLED, vnfPkgInfo.getOperationalState());
        assertEquals(VnfPkgInfo.OnboardingStateEnum.CREATED, vnfPkgInfo.getOnboardingState());
        assertNotNull(vnfPkgInfo.getUserDefinedData());
        assertNotNull(vnfPkgInfo.getLinks().getSelf());
    }

    @Step("ETSI onboard of a VNF Package")
    public static void etsiOnboardById(String vnfPkgId, OnboardApp etsiA1PackageToOnboard) {
        etsiOnboardById(vnfPkgId, etsiA1PackageToOnboard, false);
    }

    @Step("ETSI onboard of a VNF Package")
    public static void etsiOnboardById(String vnfPkgId, OnboardApp etsiA1PackageToOnboard, boolean isLocalResource) {
        ResponseEntity<String> responseEntity = VnfPackagesClient.putVnfPackage(vnfPkgId, etsiA1PackageToOnboard, isLocalResource);
        assertThat("Package was not successfully accepted for ETSI onboarding: " + responseEntity.getBody(),
                responseEntity.getStatusCodeValue(), is(202));
    }

    @Step("ETSI onboard of a VNF Package using Input Stream")
    public static void etsiOnboardByIdWithInputStream(String vnfPkgId, OnboardApp etsiA1PackageToOnboard) {
        ResponseEntity<String> responseEntity = VnfPackagesClient.putVnfPackageUsingInputStream(vnfPkgId, etsiA1PackageToOnboard);
        assertThat("Package was not successfully accepted for ETSI onboarding: " + responseEntity.getBody(),
                   responseEntity.getStatusCodeValue(), is(202));
    }

    /**
     * Get onboarded package by packageId and verify charts.
     *
     * @param vnfpkgId The package id of the CSAR to be retrieved and verified.
     */
    @Step("Retrieve onboarded CSAR package details using ETSI get package id and validate package")
    public static VnfPkgInfo retrieveAndVerifyVNFPackageById(final String vnfpkgId) {
        verifyIfOnboarded(vnfpkgId);
        VnfPkgInfo vnfPkgInfo = getVnfPackage(vnfpkgId).getBody(); // NOSONAR
        assertTrue((isOnboarded(vnfpkgId)),
                "ETSI Onboarding was not successful. Onboarding state is: " + vnfPkgInfo.getOnboardingState().name()); // NOSONAR
        for (HelmPackage helmPackage : vnfPkgInfo.getHelmPackageUrls()) {
            assertNotNull(helmPackage.getChartArtifactKey());
        }
        return vnfPkgInfo;
    }

    /**
     * Get onboarded package by packageId.
     *
     * @param vnfpkgId The package id of the CSAR to be retrieved and verified.
     */
    @Step("Retrieve onboarded CSAR package without charts details using ETSI get package id and validate package")
    public static VnfPkgInfo retrieveAndVerifyVNFPackageWithoutChartsById(final String vnfpkgId) {
        verifyIfOnboarded(vnfpkgId);
        VnfPkgInfo vnfPkgInfo = getVnfPackage(vnfpkgId).getBody(); // NOSONAR
        assertTrue((isOnboarded(vnfpkgId)),
                "ETSI Onboarding was not successful. Onboarding state is: " + vnfPkgInfo.getOnboardingState().name()); // NOSONAR
        return vnfPkgInfo;
    }

    /**
     * Verify helmfile by packageId.
     *
     * @param vnfpkgId The package id of the CSAR.
     */
    @Step("Retrieve helmfile of onboarded CSAR package by package id and verify if result exists")
    public static void verifyHelmfileExistenceByPackageId(final String vnfpkgId) {
        LOGGER.info("Attempting to retrieve helmfile with package ID: {} ", vnfpkgId);
        ResponseEntity<byte[]> response = getHelmfile(vnfpkgId);
        assertEquals(response.getStatusCode(), HttpStatus.OK);
        byte[] helmfile = response.getBody();
        assertNotNull(helmfile);
    }

    /**
     * Get onboarded package by packageId.
     *
     * @param vnfpkgId The package id of the CSAR to be retrieved and verified.
     */
    @Step("Retrieve onboarded CSAR package details using ETSI get package id and validate package and verify the image count")
    public static void verifyImageCountOfVNFPackageById(final String vnfpkgId, final int expectedImageCount) {
        VnfPkgInfo vnfPkgInfo = getVnfPackage(vnfpkgId).getBody();
        assertNotNull(vnfPkgInfo);
        assertNotNull(vnfPkgInfo.getSoftwareImages()); // NOSONAR
        assertEquals(expectedImageCount, vnfPkgInfo.getSoftwareImages().size()); // NOSONAR
    }

    @Step("create VNF Package")
    public static VnfPkgInfo createVnfPackage(CreateVnfPkgInfoRequest createVnfPkgInfoRequest) {
        return VnfPackagesClient.postVnfPackage(createVnfPkgInfoRequest);
    }

    @Step("Check Helm chart Urls are saved and present in the get package response")
    public static void checkHelmChartUrlsSaved(final String vnfpkgId) {
        VnfPkgInfo vnfPkgInfo = getVnfPackage(vnfpkgId).getBody(); // NOSONAR
        assertNotNull(vnfPkgInfo);
        LOGGER.info(vnfPkgInfo.toString()); // NOSONAR
        List<HelmPackage> helmPackageUrls = vnfPkgInfo.getHelmPackageUrls();
        checkPrioritiesInOrder(helmPackageUrls, Arrays.asList(1, 2));
        checkHelmPackageProperties(helmPackageUrls.get(0), SAMPLE_DESCRIPTOR_CHART_0_0_1_223);
        checkHelmPackageProperties(helmPackageUrls.get(1), TEST_SCALE_CHART_0_1_0);
    }

    @Step("Check Helm chart Urls are saved and present in the get package response for multi vnfd")
    public static void checkHelmChartUrlsSavedMultipleVnfd(final String vnfpkgId) {
        VnfPkgInfo vnfPkgInfo = getVnfPackage(vnfpkgId).getBody(); // NOSONAR
        assertNotNull(vnfPkgInfo);
        LOGGER.info(vnfPkgInfo.toString()); // NOSONAR
        List<HelmPackage> helmPackageUrls = vnfPkgInfo.getHelmPackageUrls();
        checkPrioritiesInOrder(helmPackageUrls, Arrays.asList(0, 0));
        checkHelmPackageProperties(helmPackageUrls.get(0), TEST_SCALE_CHART_0_1_0);
        checkHelmPackageProperties(helmPackageUrls.get(1), SPIDER_APP_CHART_INFO_2_207_9);
    }

    @Step("Check Helm chart Urls are saved and present in the get package response for CRD csar")
    public static void checkHelmChartUrlsSavedCRDCsar(final String vnfpkgId) {
        VnfPkgInfo vnfPkgInfo = getVnfPackage(vnfpkgId).getBody(); // NOSONAR
        assertNotNull(vnfPkgInfo);
        LOGGER.info(vnfPkgInfo.toString()); // NOSONAR
        List<HelmPackage> helmPackageUrls = vnfPkgInfo.getHelmPackageUrls();
        checkPrioritiesInOrder(helmPackageUrls, Arrays.asList(1, 2, 3, 4, 5));
        checkHelmPackageProperties(helmPackageUrls.get(0), ERIC_SEC_SIP_TLS_CRD_MULTI_A_2_3_0_7);
        checkHelmPackageProperties(helmPackageUrls.get(1), ERIC_SEC_CERTM_CRD_MULTI_A_2_8_0_72);
        checkHelmPackageProperties(helmPackageUrls.get(2), SCALE_CRD_1_0_0);
        checkHelmPackageProperties(helmPackageUrls.get(3), SPIDER_APP_CHART_INFO_2_208_2);
        checkHelmPackageProperties(helmPackageUrls.get(4), TEST_SCALE_CHART_0_2_0);
    }

    public static VnfPkgInfo onboardCsarStep(OnboardApp packageToOnboard) {
        CreateVnfPkgInfoRequest createVnfPkgInfoRequest =
                getRequestObjFromJson(getFileFromResources(CREATE_VNFPACKAGE_SKIP_IMAGE_UPLOAD_JSON), CreateVnfPkgInfoRequest.class);
        VnfPkgInfo vnfPkgInfo = createVnfPackage(createVnfPkgInfoRequest);
        etsiOnboardById(vnfPkgInfo.getId(), packageToOnboard);
        return vnfPkgInfo;
    }

    public static Callable<Boolean> csarIsOnboardedPredicate(final String packageId) {
        VnfPkgInfo vnfPkgInfo = retrieveAndVerifyVNFPackageById(packageId);
        return () -> vnfPkgInfo.getOnboardingState().equals(VnfPkgInfo.OnboardingStateEnum.ONBOARDED);
    }

    public static void checkServiceModel(final String vnfpkgId) {
        LOGGER.info("Service Model checking has been started for package {}", vnfpkgId);
        ResponseEntity<Map<String, String>> responseEntity = httpGetCallMap(getUri(SERVICE_MODEL_KEY, vnfpkgId),
                new ParameterizedTypeReference<>() {
                });

        assertNotNull(responseEntity, "The response of getting Service Model for package should not be empty");
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        Map<String, String> serviceModelResponseMap = responseEntity.getBody();
        assertNotNull(serviceModelResponseMap, "Service model response map should not be empty");
        String detail = serviceModelResponseMap.get("detail"); // NOSONAR
        assertNotNull(detail, "Service model response detail should not be empty");
        assertEquals(String.format("Service model record for package with id: %s not found", vnfpkgId), detail,
                "Service model response detail should equal to expected value");

    }

    public static void checkPackageOperations(String packageId, List<OperationInfo> expectedOperations,
                                              List<String> destinationDescriptorIds) {
        List<OperationDetailResponse> supportedOperations = checkPackageSupportedOperations(packageId, expectedOperations);
        LOGGER.info("Check package {} additional parameters", packageId);
        List<String> supportedOperationsNames = getAllSupportedOperations(supportedOperations);
        expectedOperations.stream()
                .filter(operationInfo -> supportedOperationsNames.contains(operationInfo.getOperationName()))
                .forEach(operationInfo -> checkPackageOperation(packageId, operationInfo, destinationDescriptorIds));
    }

    private static void checkPackageOperation(String packageId, OperationInfo operationInfo,
                                              List<String> destinationDescriptorIds) {
        String operationName = operationInfo.getOperationName();
        List<AdditionalPropertyResponse> additionalParameters
                = getAdditionalParameters(packageId, operationName, destinationDescriptorIds);
        assertNotNull(additionalParameters, "Additional Property response should not be empty");
        validateAdditionalParametersMatchExpected(operationInfo, additionalParameters);
    }

    @Step
    public static void clearPackages() {
        ResponseEntity<List<VnfPkgInfo>> packageListResponse = getVnfPackages();
        assertNotNull(packageListResponse);
        assertNotNull(packageListResponse.getBody());
        getVnfPackages().getBody().forEach((VnfPkgInfo pkg) -> { // NOSONAR
            ResponseEntity<String> response = DeletePackageCommonSteps.deletePackage(pkg);
            assertEquals(response.getStatusCode(), HttpStatus.NO_CONTENT);
        });
    }

    /**
     * Delete a package and verify the package has been deleted.
     *
     * @param vnfPkgInfo The package to be deleted.
     */
    @Step("Delete a package and verify the package has been deleted")
    public static void deletePackageAndVerifyDeleted(final VnfPkgInfo vnfPkgInfo) {
        ResponseEntity<String> deleteResponse = deletePackage(vnfPkgInfo);
        verifySingleDeletedPackage(deleteResponse, vnfPkgInfo);
    }

    @Step("Check presence of artifact")
    public static void checkArtifactPresent(final String vnfPkgId, final String artifactPath, final MediaType acceptType) {
        ResponseEntity<String> artifact = getArtifact(vnfPkgId, artifactPath, acceptType);
        assertEquals(artifact.getStatusCode(), HttpStatus.OK);
        LOGGER.info(artifact.getBody());
        assertNotNull(artifact.getBody(), "artifact is null: " + artifactPath);
    }

    private static void verifyResponseForHelmCharts(OnboardApp app) {
        ResponseEntity<String> responseEntity = getHelmCharts();
        assertThat("Chart not found", responseEntity.getStatusCodeValue(), is(200));
        assertNotNull(responseEntity.getBody(), "Body should be not empty");
        assertTrue(responseEntity.getBody().contains(app.getAppName())); // NOSONAR

    }

    private static void verifyResponseForHelmChart(OnboardApp app) {
        ResponseEntity<String> responseEntity = getHelmChart(app.getAppName());
        assertThat("Chart not found", responseEntity.getStatusCodeValue(), is(200));
        assertNotNull(responseEntity.getBody(), "Body should be not empty");
        assertTrue(responseEntity.getBody().contains(app.getAppName())); // NOSONAR
    }

    private static void checkPrioritiesInOrder(final List<HelmPackage> helmPackageUrls, final List<Integer> expectedPriorities) {
        assertEquals(expectedPriorities.size(), helmPackageUrls.size());
        for (int i = 0; i < helmPackageUrls.size(); ++i) {
            HelmPackage helmPackage = helmPackageUrls.get(i);
            int expectedPriority = expectedPriorities.get(i);
            assertThat(String.format("Priority is incorrect for chart %d", i), helmPackage.getPriority(), is(expectedPriority));
        }
    }

    private static boolean isOnboarded(final String vnfpkgId) {
        ResponseEntity<VnfPkgInfo> vnfPackageResponse = getVnfPackage(vnfpkgId);
        assertNotNull(vnfPackageResponse);
        assertNotNull(vnfPackageResponse.getBody());
        if (vnfPackageResponse.getBody().getOnboardingState() == null) { // NOSONAR
            return false;
        }
        final String onboardingState = vnfPackageResponse.getBody().getOnboardingState().name(); // NOSONAR
        LOGGER.info("Current onboarding state is {}", onboardingState);
        return ("ONBOARDED".equals(onboardingState));
    }

    private static List<OperationDetailResponse> checkPackageSupportedOperations(String packageId, List<OperationInfo> expectedOperations) {
        LOGGER.info("Check package {} supported operations", packageId);
        List<OperationDetailResponse> supportedOperations = getSupportedOperations(packageId);
        assertEquals(expectedOperations.size(), supportedOperations.size(), "Supported Operation list size does not equal to expected value");
        Map<String, OperationDetailResponse> expectedOperationDetailResponses = expectedOperations
                .stream()
                .collect(Collectors
                        .toMap(OperationInfo::getOperationName, OnboardingAcceptanceTestSteps::buildOperationDetailResponse));
        supportedOperations
                .forEach(operation -> Assertions
                        .assertThat(operation)
                        .usingRecursiveComparison().isEqualTo(expectedOperationDetailResponses.get(operation.getOperationName())));
        return supportedOperations;
    }

    private static OperationDetailResponse buildOperationDetailResponse(OperationInfo operationInfo) {
        OperationDetailResponse operationDetailResponse = new OperationDetailResponse();
        operationDetailResponse.setSupported(operationInfo.isSupported());
        operationDetailResponse.setOperationName(operationInfo.getOperationName());
        operationDetailResponse.setError(operationInfo.getOperationErrorMessage());
        return operationDetailResponse;
    }

    private static List<String> getAllSupportedOperations(List<OperationDetailResponse> supportedOperations) {
        return supportedOperations
                .stream()
                .filter(OperationDetailResponse::getSupported)
                .map(OperationDetailResponse::getOperationName)
                .collect(Collectors.toList());
    }

    private static void validateAdditionalParametersMatchExpected(OperationInfo operationInfo,
                                                                  List<AdditionalPropertyResponse> additionalParameters) {
        assertEquals(operationInfo.getPropertyList().size(), additionalParameters.size(),
                "Additional Parameters list size should equals to expected list size");
        additionalParameters
                .forEach(response -> {
                    AdditionalPropertyResponse expectedAdditionalParameter =
                            getMatchingAdditionalParameterFromOperationInfo(operationInfo, response);
                    Assertions.assertThat(response)
                            .usingRecursiveComparison()
                            .ignoringExpectedNullFields()
                            .isEqualTo(expectedAdditionalParameter);
                });
    }

    private static AdditionalPropertyResponse getMatchingAdditionalParameterFromOperationInfo(OperationInfo operationInfo,
                                                                                              AdditionalPropertyResponse response) {
        return operationInfo.getPropertyList()
                .stream()
                .filter(info -> info.getName().equals(response.getName()))
                .findFirst()
                .orElseThrow(() -> new TestRuntimeException(String.format("Additional parameter %s is not found", response.getName())));
    }

    private static void checkHelmPackageProperties(final HelmPackage helmPackage, final TestChartInfo expectedInfo) {
        assertThat("Chart url is incorrect",
                helmPackage.getChartUrl(),
                endsWith(expectedInfo.getChartFileName()));
        assertThat("Chart name is incorrect",
                helmPackage.getChartName(),
                equalTo(expectedInfo.getChartName()));
        assertThat("Chart version is incorrect",
                helmPackage.getChartVersion(),
                equalTo(expectedInfo.getChartVersion()));
        assertThat("Chart type is incorrect",
                helmPackage.getChartType().name(),
                equalTo(expectedInfo.getChartType()));
    }

    private static void verifyIfOnboarded(final String vnfpkgId) {
        LOGGER.info("Attempting to retrieve package with ID: {} ", vnfpkgId);
        long stopTime = System.nanoTime() + TimeUnit.MINUTES.toNanos(10);
        while (stopTime > System.nanoTime()) {
            if (isOnboarded(vnfpkgId)) {
                break;
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) { // NOSONAR
                // Ignore
            }
        }
    }
}
