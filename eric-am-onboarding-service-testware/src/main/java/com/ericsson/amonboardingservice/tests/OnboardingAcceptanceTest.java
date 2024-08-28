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
package com.ericsson.amonboardingservice.tests;

import static com.ericsson.amonboardingservice.utilities.TestConstants.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import static com.ericsson.amonboardingservice.steps.OnboardingAcceptanceTestSteps.checkArtifactPresent;
import static com.ericsson.amonboardingservice.steps.OnboardingAcceptanceTestSteps.checkHelmChartUrlsSaved;
import static com.ericsson.amonboardingservice.steps.OnboardingAcceptanceTestSteps.checkHelmChartUrlsSavedCRDCsar;
import static com.ericsson.amonboardingservice.steps.OnboardingAcceptanceTestSteps.checkHelmChartUrlsSavedMultipleVnfd;
import static com.ericsson.amonboardingservice.steps.OnboardingAcceptanceTestSteps.checkPackageOperations;
import static com.ericsson.amonboardingservice.steps.OnboardingAcceptanceTestSteps.checkPresenceOfDockerManifests;
import static com.ericsson.amonboardingservice.steps.OnboardingAcceptanceTestSteps.checkServiceModel;
import static com.ericsson.amonboardingservice.steps.OnboardingAcceptanceTestSteps.clearPackages;
import static com.ericsson.amonboardingservice.steps.OnboardingAcceptanceTestSteps.createVnfPackage;
import static com.ericsson.amonboardingservice.steps.OnboardingAcceptanceTestSteps.createVnfPackageAndVerify;
import static com.ericsson.amonboardingservice.steps.OnboardingAcceptanceTestSteps.csarIsOnboardedPredicate;
import static com.ericsson.amonboardingservice.steps.OnboardingAcceptanceTestSteps.deletePackageAndVerifyDeleted;
import static com.ericsson.amonboardingservice.steps.OnboardingAcceptanceTestSteps.etsiOnboardById;
import static com.ericsson.amonboardingservice.steps.OnboardingAcceptanceTestSteps.etsiOnboardByIdWithInputStream;
import static com.ericsson.amonboardingservice.steps.OnboardingAcceptanceTestSteps.onboardCsarStep;
import static com.ericsson.amonboardingservice.steps.OnboardingAcceptanceTestSteps.retrieveAndVerifyVNFPackageById;
import static com.ericsson.amonboardingservice.steps.OnboardingAcceptanceTestSteps.retrieveAndVerifyVNFPackageWithoutChartsById;
import static com.ericsson.amonboardingservice.steps.OnboardingAcceptanceTestSteps.verifyHelmfileExistenceByPackageId;
import static com.ericsson.amonboardingservice.steps.OnboardingAcceptanceTestSteps.verifyImageCountOfVNFPackageById;
import static com.ericsson.amonboardingservice.steps.OnboardingAcceptanceTestSteps.verifyUploadedHelmChartsAndImages;
import static com.ericsson.amonboardingservice.steps.delete.DeletePackageCommonSteps.deleteAndVerifyPackagesWithCommonImages;
import static com.ericsson.amonboardingservice.steps.delete.DeletePackageCommonSteps.deletePackage;
import static com.ericsson.amonboardingservice.steps.delete.DeletePackageVerifier.verifyNotFoundDeletionOfPackageResponse;
import static com.ericsson.amonboardingservice.steps.delete.DeletePackageVerifier.verifySingleDeletedPackage;
import static com.ericsson.amonboardingservice.utilities.CsarFileUtils.getCsarName;
import static com.ericsson.amonboardingservice.utilities.CsarFileUtils.getFileFromResources;
import static com.ericsson.amonboardingservice.utilities.CsarFileUtils.getOnboardApp;
import static com.ericsson.amonboardingservice.utilities.CsarFileUtils.getTestPackageData;
import static com.ericsson.amonboardingservice.utilities.RestUtils.getRequestObjFromJson;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.qameta.allure.Description;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.ericsson.amonboardingservice.model.CreateVnfPkgInfoRequest;
import com.ericsson.amonboardingservice.model.OnboardApp;
import com.ericsson.amonboardingservice.model.OperationInfo;
import com.ericsson.amonboardingservice.model.TestPackageData;
import com.ericsson.amonboardingservice.model.VnfPkgInfo;

import io.qameta.allure.Epic;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class OnboardingAcceptanceTest {
    public static final String VNF_PACKAGE_ID_IS_NULL = "VnfPackageId is null";

    @Epic("https://jira-oss.seli.wh.rnd.internal.ericsson.com/browse/SM-23211")
    @Test
    @Description("Create a new Vnf Package")
    public void createVnfPackageTest() {
        CreateVnfPkgInfoRequest createVnfPkgInfoRequest
                = getRequestObjFromJson(getFileFromResources(CREATE_VNF_PACKAGE), CreateVnfPkgInfoRequest.class);
        createVnfPackageAndVerify(createVnfPkgInfoRequest);
    }

    @Test
    @Description("Upload a CSAR using ETSI standard onboarding")
    public void etsiOnboardCSAR() {
        TestPackageData testPackageData = getTestPackageData(ETSI_A1_PACKAGE_TO_ONBOARD);
        List<OperationInfo> expectedOperations = testPackageData.getOperations();
        String vnfPkgId = onboardCsarStep(testPackageData.getOnboardApp()).getId();
        VnfPkgInfo vnfPkgInfo = retrieveAndVerifyVNFPackageById(vnfPkgId);

        checkPackageOperations(vnfPkgId, expectedOperations, testPackageData.getOnboardApp().getDestinationDescriptorIds());
        deletePackageAndVerifyDeleted(vnfPkgInfo);
    }

    @Test
    @Description("Onboards two lightweight CSARs in parallel using ETSI standard onboarding")
    public void etsiOnboardMultipleCSARsInParallel() throws InterruptedException, ExecutionException {
        TestPackageData testPackageDataFirst = getTestPackageData(LIGHTWEIGHT_SAME_CHART_PACKAGE_1);
        TestPackageData testPackageDataSecond = getTestPackageData(LIGHTWEIGHT_SAME_CHART_PACKAGE_2);
        List<OperationInfo> expectedOperationsFirst = testPackageDataFirst.getOperations();
        List<OperationInfo> expectedOperationsSecond = testPackageDataSecond.getOperations();
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Callable<VnfPkgInfo> onboardFirst = () -> onboardCsarStep(testPackageDataFirst.getOnboardApp());
        Callable<VnfPkgInfo> onboardSecond = () -> onboardCsarStep(testPackageDataSecond.getOnboardApp());
        Future<VnfPkgInfo> firstPkgFuture = executorService.submit(onboardFirst);
        Future<VnfPkgInfo> secondPkgFuture = executorService.submit(onboardSecond);
        VnfPkgInfo firstPkg = firstPkgFuture.get();
        VnfPkgInfo secondPkg = secondPkgFuture.get();
        LOGGER.info("Packages to onboard in parallel: {} and {}", firstPkg.getId(), secondPkg.getId());
        await().atMost(10, TimeUnit.MINUTES).until(csarIsOnboardedPredicate(firstPkg.getId()));
        await().atMost(10, TimeUnit.MINUTES).until(csarIsOnboardedPredicate(secondPkg.getId()));
        checkPackageOperations(firstPkg.getId(), expectedOperationsFirst, testPackageDataFirst.getOnboardApp().getDestinationDescriptorIds());
        checkPackageOperations(secondPkg.getId(), expectedOperationsSecond, testPackageDataSecond.getOnboardApp().getDestinationDescriptorIds());
    }

    @Test
    @Description("Upload a CSAR without images using ETSI standard onboarding")
    public void etsiOnboardCSARWithoutImages() {
        TestPackageData testPackageData = getTestPackageData(ETSI_PACKAGE_WITHOUT_IMAGE);
        OnboardApp onboardApp = testPackageData.getOnboardApp();
        List<OperationInfo> expectedOperations = testPackageData.getOperations();
        CreateVnfPkgInfoRequest createVnfPkgInfoRequest =
                getRequestObjFromJson(getFileFromResources(CREATE_VNFPACKAGE_SKIP_IMAGE_UPLOAD_JSON), CreateVnfPkgInfoRequest.class);
        VnfPkgInfo vnfPkgInfo = createVnfPackage(createVnfPkgInfoRequest);
        etsiOnboardById(vnfPkgInfo.getId(), onboardApp);
        vnfPkgInfo = retrieveAndVerifyVNFPackageById(vnfPkgInfo.getId());
        verifyUploadedHelmChartsAndImages(getOnboardApp(onboardApp));
        verifyImageCountOfVNFPackageById(vnfPkgInfo.getId(), 0);
        checkPackageOperations(vnfPkgInfo.getId(), expectedOperations, testPackageData.getOnboardApp().getDestinationDescriptorIds());

        deletePackageAndVerifyDeleted(vnfPkgInfo);
    }

    @Test
    @Description("Upload a CSAR with helmfile without images and integration charts using ETSI standard onboarding")
    public void etsiOnboardCSARWithHelmfileOnlyWithoutImages() {
        TestPackageData testPackageData = getTestPackageData(ETSI_PACKAGE_WITHOUT_CHARTS);
        OnboardApp onboardApp = testPackageData.getOnboardApp();
        CreateVnfPkgInfoRequest createVnfPkgInfoRequest =
                getRequestObjFromJson(getFileFromResources(CREATE_VNFPACKAGE_SKIP_IMAGE_UPLOAD_JSON), CreateVnfPkgInfoRequest.class);
        VnfPkgInfo vnfPkgInfo = createVnfPackage(createVnfPkgInfoRequest);
        etsiOnboardById(vnfPkgInfo.getId(), onboardApp, true);
        vnfPkgInfo = retrieveAndVerifyVNFPackageWithoutChartsById(vnfPkgInfo.getId());
        verifyHelmfileExistenceByPackageId(vnfPkgInfo.getId());
        deletePackageAndVerifyDeleted(vnfPkgInfo);
    }

    @Test
    @Description("Upload a CSAR with CRD charts without images using ETSI standard onboarding")
    public void etsiOnboardCSARWithCRDWithoutImages() {
        TestPackageData testPackageData = getTestPackageData(ETSI_PACKAGE_WITH_CRD_WITHOUT_IMAGE);
        List<OperationInfo> expectedOperations = testPackageData.getOperations();
        CreateVnfPkgInfoRequest createVnfPkgInfoRequest =
                getRequestObjFromJson(getFileFromResources(CREATE_VNFPACKAGE_SKIP_IMAGE_UPLOAD_JSON), CreateVnfPkgInfoRequest.class);
        VnfPkgInfo vnfPkgInfo = createVnfPackage(createVnfPkgInfoRequest);
        assertNotNull(VNF_PACKAGE_ID_IS_NULL, vnfPkgInfo.getId());
        etsiOnboardById(vnfPkgInfo.getId(), testPackageData.getOnboardApp());
        vnfPkgInfo = retrieveAndVerifyVNFPackageById(vnfPkgInfo.getId());
        checkHelmChartUrlsSavedCRDCsar(vnfPkgInfo.getId());
        checkPackageOperations(vnfPkgInfo.getId(), expectedOperations, testPackageData.getOnboardApp().getDestinationDescriptorIds());
        deletePackageAndVerifyDeleted(vnfPkgInfo);
    }

    @Test
    @Description("Onboard real node PCC packages without images using ETSI standard onboarding")
    public void onboardPCCPackagesWithoutImages() {
        REAL_NODE_PCC_PACKAGES.forEach(OnboardingAcceptanceTest::onboardPackageAndVerifyBasicFieldsAreFilled);
    }

    @Test
    @Description("Onboard real node PCG packages without images using ETSI standard onboarding")
    public void onboardPCGPackagesWithoutImages() {
        REAL_NODE_PCG_PACKAGES.forEach(OnboardingAcceptanceTest::onboardPackageAndVerifyBasicFieldsAreFilled);
    }

    @Test
    @Description("Onboard real node EDA packages without images using ETSI standard onboarding")
    public void onboardEDAPackagesWithoutImages() {
        REAL_NODE_EDA_PACKAGES.forEach(OnboardingAcceptanceTest::onboardPackageAndVerifyBasicFieldsAreFilled);
    }

    @Test
    @Description("Onboard real node Cloud RAN packages without images using ETSI standard onboarding")
    public void onboardCloudRANPackagesWithoutImages() {
        REAL_NODE_CLOUD_RAN_PACKAGES.forEach(OnboardingAcceptanceTest::onboardPackageAndVerifyBasicFieldsAreFilled);
    }

    private static void onboardPackageAndVerifyBasicFieldsAreFilled(final String testPackage) {
        TestPackageData testPackageData = getTestPackageData(testPackage);
        CreateVnfPkgInfoRequest createVnfPkgInfoRequest =
                getRequestObjFromJson(getFileFromResources(CREATE_VNFPACKAGE_SKIP_IMAGE_UPLOAD_JSON), CreateVnfPkgInfoRequest.class);

        VnfPkgInfo vnfPkgInfo = createVnfPackage(createVnfPkgInfoRequest);
        assertNotNull(VNF_PACKAGE_ID_IS_NULL, vnfPkgInfo.getId());

        etsiOnboardById(vnfPkgInfo.getId(), testPackageData.getOnboardApp(), true);
        vnfPkgInfo = retrieveAndVerifyVNFPackageById(vnfPkgInfo.getId());
        assertNotNull(vnfPkgInfo);
        assertNotNull(vnfPkgInfo.getHelmPackageUrls());
        assertNotNull(vnfPkgInfo.getAdditionalArtifacts());
        LOGGER.info(vnfPkgInfo.toString());

        deletePackageAndVerifyDeleted(vnfPkgInfo);
    }

    @Test
    @Description("Upload a Signed CSAR with Option 1 and Option 2 using ETSI standard onboarding")
    public void etsiOnboardSignedCSAROption1AndOption2() {
        TestPackageData testPackageData = getTestPackageData(ETSI_SIGNED_A1_PACKAGE_TO_ONBOARD_OPTION1_AND_OPTION2);
        List<OperationInfo> expectedOperations = testPackageData.getOperations();
        CreateVnfPkgInfoRequest createVnfPkgInfoRequest =
                getRequestObjFromJson(getFileFromResources(CREATE_VNF_PACKAGE), CreateVnfPkgInfoRequest.class);
        VnfPkgInfo vnfPkgInfo = createVnfPackage(createVnfPkgInfoRequest);
        etsiOnboardById(vnfPkgInfo.getId(), testPackageData.getOnboardApp());
        vnfPkgInfo = retrieveAndVerifyVNFPackageById(vnfPkgInfo.getId());
        assertSame(vnfPkgInfo.getPackageSecurityOption(), VnfPkgInfo.PackageSecurityOptionEnum.OPTION_2,
                "Package signing option is not as expected " + vnfPkgInfo.getPackageSecurityOption());
        checkPackageOperations(vnfPkgInfo.getId(), expectedOperations, testPackageData.getOnboardApp().getDestinationDescriptorIds());
        deletePackageAndVerifyDeleted(vnfPkgInfo);
    }

    @Test
    @Description("Upload a Signed CSAR with Option 1 using ETSI standard onboarding")
    public void etsiOnboardSignedCSAROption1() {
        TestPackageData testPackageData = getTestPackageData(ETSI_SIGNED_A1_PACKAGE_TO_ONBOARD_OPTION1);
        List<OperationInfo> expectedOperations = testPackageData.getOperations();
        CreateVnfPkgInfoRequest createVnfPkgInfoRequest =
                getRequestObjFromJson(getFileFromResources(CREATE_VNF_PACKAGE), CreateVnfPkgInfoRequest.class);
        VnfPkgInfo vnfPkgInfo = createVnfPackage(createVnfPkgInfoRequest);
        etsiOnboardById(vnfPkgInfo.getId(), testPackageData.getOnboardApp());
        vnfPkgInfo = retrieveAndVerifyVNFPackageById(vnfPkgInfo.getId());
        assertSame(vnfPkgInfo.getPackageSecurityOption(), VnfPkgInfo.PackageSecurityOptionEnum.OPTION_1,
                "Package signing option is not as expected " + vnfPkgInfo.getPackageSecurityOption());
        checkPackageOperations(vnfPkgInfo.getId(), expectedOperations, testPackageData.getOnboardApp().getDestinationDescriptorIds());
        deletePackageAndVerifyDeleted(vnfPkgInfo);
    }

    @Test
    @Description("Upload a Signed CSAR with Option 2 using ETSI standard onboarding")
    public void etsiOnboardSignedCSAROption2() {
        TestPackageData testPackageData = getTestPackageData(ETSI_SIGNED_A1_PACKAGE_TO_ONBOARD_OPTION2);
        List<OperationInfo> expectedOperations = testPackageData.getOperations();
        CreateVnfPkgInfoRequest createVnfPkgInfoRequest =
                getRequestObjFromJson(getFileFromResources(CREATE_VNF_PACKAGE), CreateVnfPkgInfoRequest.class);
        VnfPkgInfo vnfPkgInfo = createVnfPackage(createVnfPkgInfoRequest);
        etsiOnboardById(vnfPkgInfo.getId(), testPackageData.getOnboardApp());
        vnfPkgInfo = retrieveAndVerifyVNFPackageById(vnfPkgInfo.getId());
        assertSame(vnfPkgInfo.getPackageSecurityOption(), VnfPkgInfo.PackageSecurityOptionEnum.OPTION_2,
                "Package signing option is not as expected " + vnfPkgInfo.getPackageSecurityOption());
        checkPackageOperations(vnfPkgInfo.getId(), expectedOperations, testPackageData.getOnboardApp().getDestinationDescriptorIds());
        deletePackageAndVerifyDeleted(vnfPkgInfo);
    }

    @Test
    @Description("Upload a CSAR with multiple packages using ETSI standard onboarding")
    public void etsiOnboardCSARMultipleCharts() {
        TestPackageData testPackageData = getTestPackageData(PACKAGE_WITH_MULTIPLE_CHARTS);
        List<OperationInfo> expectedOperations = testPackageData.getOperations();
        CreateVnfPkgInfoRequest createVnfPkgInfoRequest =
                getRequestObjFromJson(getFileFromResources(CREATE_VNF_PACKAGE), CreateVnfPkgInfoRequest.class);
        VnfPkgInfo vnfPkgInfo = createVnfPackage(createVnfPkgInfoRequest);
        assertNotNull(vnfPkgInfo.getId(), VNF_PACKAGE_ID_IS_NULL);
        etsiOnboardById(vnfPkgInfo.getId(), testPackageData.getOnboardApp());
        vnfPkgInfo = retrieveAndVerifyVNFPackageById(vnfPkgInfo.getId());
        checkHelmChartUrlsSaved(vnfPkgInfo.getId());
        checkPackageOperations(vnfPkgInfo.getId(), expectedOperations, testPackageData.getOnboardApp().getDestinationDescriptorIds());
        deletePackageAndVerifyDeleted(vnfPkgInfo);
    }

    @Disabled("Not valid as nested vnfd is unsupported")
    @Test
    @Description("Upload a CSAR with multiple packages using ETSI standard onboarding with multiple vnfd structure")
    public void etsiOnboardCSARMultipleChartsMultipleVnfd() {
        TestPackageData testPackageData = getTestPackageData(PACKAGE_WITH_MULTIPLE_VNFDS);
        CreateVnfPkgInfoRequest createVnfPkgInfoRequest =
                getRequestObjFromJson(getFileFromResources(CREATE_VNF_PACKAGE), CreateVnfPkgInfoRequest.class);
        VnfPkgInfo vnfPkgInfo = createVnfPackage(createVnfPkgInfoRequest);
        assertNotNull(VNF_PACKAGE_ID_IS_NULL, vnfPkgInfo.getId());
        etsiOnboardById(vnfPkgInfo.getId(), testPackageData.getOnboardApp());
        vnfPkgInfo = retrieveAndVerifyVNFPackageById(vnfPkgInfo.getId());
        checkHelmChartUrlsSavedMultipleVnfd(vnfPkgInfo.getId());
        deletePackageAndVerifyDeleted(vnfPkgInfo);
    }

    @Test
    @Description("Upload a CSAR with rollback interface using ETSI standard onboarding")
    public void etsiOnboardCSARWithRollbackInterface() {
        TestPackageData testPackageData = getTestPackageData(ETSI_PACKAGE_WITH_ROLLBACK_WITHOUT_IMAGE);
        List<OperationInfo> expectedOperations = testPackageData.getOperations();
        CreateVnfPkgInfoRequest createVnfPkgInfoRequest =
                getRequestObjFromJson(getFileFromResources(CREATE_VNFPACKAGE_SKIP_IMAGE_UPLOAD_JSON), CreateVnfPkgInfoRequest.class);
        VnfPkgInfo vnfPkgInfo = createVnfPackage(createVnfPkgInfoRequest);
        etsiOnboardById(vnfPkgInfo.getId(), testPackageData.getOnboardApp());
        vnfPkgInfo = retrieveAndVerifyVNFPackageById(vnfPkgInfo.getId());
        checkPackageOperations(vnfPkgInfo.getId(), expectedOperations, testPackageData.getOnboardApp().getDestinationDescriptorIds());
        ResponseEntity<String> deleteResponse = deletePackage(vnfPkgInfo);
        verifySingleDeletedPackage(deleteResponse, vnfPkgInfo);
    }

    @Test
    @Description("Verify that all parts of the CNF package are removed if the package is in the “Not In Use” state \n" +
            "  and docker images of this package are not used by other CNF packages.")
    public void removePackageAndVerifyDockerImagesCleanup() {
        TestPackageData testPackageData = getTestPackageData(NGINX_REDIS_PACKAGE_1);
        List<OperationInfo> expectedOperations = testPackageData.getOperations();
        CreateVnfPkgInfoRequest createVnfPkgInfoRequest =
                getRequestObjFromJson(getFileFromResources(CREATE_VNF_PACKAGE), CreateVnfPkgInfoRequest.class);
        VnfPkgInfo vnfPkgInfo = createVnfPackage(createVnfPkgInfoRequest);
        assertNotNull(vnfPkgInfo, VNF_PACKAGE_ID_IS_NULL);
        etsiOnboardById(vnfPkgInfo.getId(), testPackageData.getOnboardApp());
        vnfPkgInfo = retrieveAndVerifyVNFPackageById(vnfPkgInfo.getId());
        checkPresenceOfDockerManifests(vnfPkgInfo);
        checkPackageOperations(vnfPkgInfo.getId(), expectedOperations, testPackageData.getOnboardApp().getDestinationDescriptorIds());
        deletePackageAndVerifyDeleted(vnfPkgInfo);
        ResponseEntity<String> deleteResponseRepeat = deletePackage(vnfPkgInfo);
        verifyNotFoundDeletionOfPackageResponse(deleteResponseRepeat);
    }

    @Test
    @Description("    Verify that all parts of the CNF package are removed \n" +
            "  if the package is in the “Not In Use” state and is uploaded after previous deletion.")
    public void removePackageAndVerifyRepeatedOnboarding() {
        TestPackageData testPackageData = getTestPackageData(NGINX_REDIS_PACKAGE_1);
        List<OperationInfo> expectedOperations = testPackageData.getOperations();
        CreateVnfPkgInfoRequest createVnfPkgInfoRequest =
                getRequestObjFromJson(getFileFromResources(CREATE_VNF_PACKAGE), CreateVnfPkgInfoRequest.class);
        VnfPkgInfo vnfPkgInfo = createVnfPackage(createVnfPkgInfoRequest);
        assertNotNull(vnfPkgInfo, VNF_PACKAGE_ID_IS_NULL);
        etsiOnboardById(vnfPkgInfo.getId(), testPackageData.getOnboardApp());
        vnfPkgInfo = retrieveAndVerifyVNFPackageById(vnfPkgInfo.getId());
        checkPresenceOfDockerManifests(vnfPkgInfo);
        deletePackage(vnfPkgInfo);
        vnfPkgInfo = createVnfPackage(createVnfPkgInfoRequest);
        etsiOnboardById(vnfPkgInfo.getId(), testPackageData.getOnboardApp());
        vnfPkgInfo = retrieveAndVerifyVNFPackageById(vnfPkgInfo.getId());
        checkPackageOperations(vnfPkgInfo.getId(), expectedOperations, testPackageData.getOnboardApp().getDestinationDescriptorIds());
        deletePackageAndVerifyDeleted(vnfPkgInfo);
    }

    @Test
    @Description("Verify that the CNF package is removed if the package is in the “Not In Use” state \n" +
            "  and docker images of this package are used by other CNF packages.")
    public void removePackagesWithCommonDockerImagesAndVerify() {
        TestPackageData testPackageDataFirst = getTestPackageData(MONGO_NGINX_PACKAGE);
        TestPackageData testPackageDataSecond = getTestPackageData(NGINX_REDIS_PACKAGE_1);
        List<OperationInfo> expectedOperationsFirst = testPackageDataFirst.getOperations();
        List<OperationInfo> expectedOperationsSecond = testPackageDataSecond.getOperations();

        CreateVnfPkgInfoRequest createVnfPkgInfoRequestOne =
                getRequestObjFromJson(getFileFromResources(CREATE_VNF_PACKAGE), CreateVnfPkgInfoRequest.class);
        VnfPkgInfo vnfPkgInfoOne = createVnfPackage(createVnfPkgInfoRequestOne);

        CreateVnfPkgInfoRequest createVnfPkgInfoRequestTwo =
                getRequestObjFromJson(getFileFromResources(CREATE_VNF_PACKAGE), CreateVnfPkgInfoRequest.class);
        VnfPkgInfo vnfPkgInfoTwo = createVnfPackage(createVnfPkgInfoRequestTwo);

        String packageIdFirst = vnfPkgInfoOne.getId();
        String packageIdSecond = vnfPkgInfoTwo.getId();
        etsiOnboardById(packageIdFirst, testPackageDataFirst.getOnboardApp());
        etsiOnboardById(packageIdSecond, testPackageDataSecond.getOnboardApp());
        vnfPkgInfoOne = retrieveAndVerifyVNFPackageById(packageIdFirst);
        vnfPkgInfoTwo = retrieveAndVerifyVNFPackageById(packageIdSecond);
        checkPresenceOfDockerManifests(vnfPkgInfoOne);
        checkPresenceOfDockerManifests(vnfPkgInfoTwo);
        checkPackageOperations(packageIdFirst, expectedOperationsFirst, testPackageDataFirst.getOnboardApp().getDestinationDescriptorIds());
        checkPackageOperations(packageIdSecond, expectedOperationsSecond, testPackageDataSecond.getOnboardApp().getDestinationDescriptorIds());
        deleteAndVerifyPackagesWithCommonImages(vnfPkgInfoOne, vnfPkgInfoTwo);
    }

    @Test
    @Description("Verify that the CNF package is removed if the package is in the “Not In Use” state \n" +
            " and all docker images of this package are used by other CNF packages.")
    public void removePackagesWithSameDockerImagesAndVerify() {
        TestPackageData testPackageDataFirst = getTestPackageData(NGINX_REDIS_PACKAGE_1);
        TestPackageData testPackageDataSecond = getTestPackageData(NGINX_REDIS_PACKAGE_2);
        List<OperationInfo> expectedOperationsFirst = testPackageDataFirst.getOperations();
        List<OperationInfo> expectedOperationsSecond = testPackageDataSecond.getOperations();

        CreateVnfPkgInfoRequest createVnfPkgInfoRequestOne =
                getRequestObjFromJson(getFileFromResources(CREATE_VNF_PACKAGE), CreateVnfPkgInfoRequest.class);
        VnfPkgInfo vnfPkgInfoOne = createVnfPackage(createVnfPkgInfoRequestOne);

        CreateVnfPkgInfoRequest createVnfPkgInfoRequestTwo =
                getRequestObjFromJson(getFileFromResources(CREATE_VNF_PACKAGE), CreateVnfPkgInfoRequest.class);
        VnfPkgInfo vnfPkgInfoTwo = createVnfPackage(createVnfPkgInfoRequestTwo);

        String packageIdFirst = vnfPkgInfoOne.getId();
        String packageIdSecond = vnfPkgInfoTwo.getId();
        etsiOnboardById(packageIdFirst, testPackageDataFirst.getOnboardApp());
        etsiOnboardById(packageIdSecond, testPackageDataSecond.getOnboardApp());
        vnfPkgInfoOne = retrieveAndVerifyVNFPackageById(packageIdFirst);
        vnfPkgInfoTwo = retrieveAndVerifyVNFPackageById(packageIdSecond);
        checkPresenceOfDockerManifests(vnfPkgInfoOne);
        checkPresenceOfDockerManifests(vnfPkgInfoTwo);
        checkPackageOperations(packageIdFirst, expectedOperationsFirst, testPackageDataFirst.getOnboardApp().getDestinationDescriptorIds());
        checkPackageOperations(packageIdSecond, expectedOperationsSecond, testPackageDataSecond.getOnboardApp().getDestinationDescriptorIds());

        deleteAndVerifyPackagesWithCommonImages(vnfPkgInfoOne, vnfPkgInfoTwo);
    }

    @Test
    @Description("Upload a CSAR with levels and scaling mapping file using ETSI standard onboarding")
    public void etsiOnboardCsarWithLevelsAndMappingFile() {
        TestPackageData testPackageData = getTestPackageData(ETSI_PACKAGE_WITH_LEVELS_AND_MAPPING);
        List<OperationInfo> expectedOperations = testPackageData.getOperations();
        String vnfPkgId = onboardCsarStep(testPackageData.getOnboardApp()).getId();
        VnfPkgInfo vnfPkgInfo = retrieveAndVerifyVNFPackageById(vnfPkgId);
        checkArtifactPresent(vnfPkgId, "Definitions/OtherTemplates/scaling_mapping.yaml", MediaType.TEXT_PLAIN);
        checkPackageOperations(vnfPkgInfo.getId(), expectedOperations, testPackageData.getOnboardApp().getDestinationDescriptorIds());
        deletePackageAndVerifyDeleted(vnfPkgInfo);
    }

    @Test
    @Description("Upload a CSAR with levels and no mapping file using ETSI standard onboarding")
    public void etsiOnboardCsarWithLevelsNoMappingFile() {
        TestPackageData testPackageData = getTestPackageData(ETSI_PACKAGE_WITH_LEVELS_NO_MAPPING);
        List<OperationInfo> expectedOperations = testPackageData.getOperations();
        String vnfPkgId = onboardCsarStep(testPackageData.getOnboardApp()).getId();
        VnfPkgInfo vnfPkgInfo = retrieveAndVerifyVNFPackageById(vnfPkgId);
        checkPackageOperations(vnfPkgInfo.getId(), expectedOperations, testPackageData.getOnboardApp().getDestinationDescriptorIds());
        deletePackageAndVerifyDeleted(vnfPkgInfo);
    }

    @Test
    @Description("Upload a Tosca 1.3 CSAR with multiple packages using input stream")
    public void etsiOnboardTosca1Dot3Rel4CSARMultiChartUsingInputStream() {
        TestPackageData testPackageData = getTestPackageData(TOSCA_1_DOT_3_REL_4_PACKAGE_WITH_MULTIPLE_CHARTS);
        List<OperationInfo> expectedOperations = testPackageData.getOperations();
        OnboardApp onboardApp = testPackageData.getOnboardApp();

        CreateVnfPkgInfoRequest createVnfPkgInfoRequest =
              getRequestObjFromJson(String.format(getFileFromResources(CREATE_VNFPACKAGE_WITH_IMAGE_UPLOAD_JSON),
                                  getCsarName(onboardApp)), CreateVnfPkgInfoRequest.class);

        VnfPkgInfo vnfPkgInfo = createVnfPackage(createVnfPkgInfoRequest);
        String packageId = vnfPkgInfo.getId();
        etsiOnboardByIdWithInputStream(packageId, onboardApp);
        vnfPkgInfo = retrieveAndVerifyVNFPackageById(packageId);
        verifyUploadedHelmChartsAndImages(getOnboardApp(onboardApp));
        verifyImageCountOfVNFPackageById(packageId, 14);
        checkPackageOperations(packageId, expectedOperations, testPackageData.getOnboardApp().getDestinationDescriptorIds());
        checkServiceModel(packageId);
        deletePackageAndVerifyDeleted(vnfPkgInfo);
    }

    @Test
    @Description("Upload a Tosca 1.3 CSAR with single packages using ETSI standard onboarding")
    public void etsiOnboardTosca1Dot3CSARSingleChart() {
        TestPackageData testPackageData = getTestPackageData(PACKAGE_WITH_SINGLE_CHART_WITHOUT_IMAGE_1_DOT_3);
        List<OperationInfo> expectedOperations = testPackageData.getOperations();
        OnboardApp onboardApp = testPackageData.getOnboardApp();
        CreateVnfPkgInfoRequest createVnfPkgInfoRequest =
                getRequestObjFromJson(getFileFromResources(CREATE_VNFPACKAGE_SKIP_IMAGE_UPLOAD_JSON), CreateVnfPkgInfoRequest.class);
        VnfPkgInfo vnfPkgInfo = createVnfPackage(createVnfPkgInfoRequest);
        String packageId = vnfPkgInfo.getId();
        etsiOnboardById(packageId, onboardApp);
        vnfPkgInfo = retrieveAndVerifyVNFPackageById(packageId);
        verifyUploadedHelmChartsAndImages(getOnboardApp(onboardApp));
        verifyImageCountOfVNFPackageById(packageId, 0);
        checkPackageOperations(packageId, expectedOperations, testPackageData.getOnboardApp().getDestinationDescriptorIds());
        checkServiceModel(packageId);
        deletePackageAndVerifyDeleted(vnfPkgInfo);
    }

    @Test
    @Description("Upload a Tosca 1.2 CSAR with multiple packages and any triggers using ETSI standard onboarding")
    public void etsiOnboardTosca1Dot2WithAnyTriggersCSARMultiChart() {
        TestPackageData testPackageData = getTestPackageData(PACKAGE_WITH_MULTI_CHART_FOR_ANY_TRIGGER);
        List<OperationInfo> expectedOperations = testPackageData.getOperations();
        OnboardApp onboardApp = testPackageData.getOnboardApp();
        CreateVnfPkgInfoRequest createVnfPkgInfoRequest =
                getRequestObjFromJson(getFileFromResources(CREATE_VNFPACKAGE_SKIP_IMAGE_UPLOAD_JSON), CreateVnfPkgInfoRequest.class);
        VnfPkgInfo vnfPkgInfo = createVnfPackage(createVnfPkgInfoRequest);
        String packageId = vnfPkgInfo.getId();
        etsiOnboardById(packageId, onboardApp);
        vnfPkgInfo = retrieveAndVerifyVNFPackageById(packageId);
        verifyUploadedHelmChartsAndImages(getOnboardApp(onboardApp));
        verifyImageCountOfVNFPackageById(packageId, 0);
        checkPackageOperations(packageId, expectedOperations, testPackageData.getOnboardApp().getDestinationDescriptorIds());
        deletePackageAndVerifyDeleted(vnfPkgInfo);
    }

    @AfterEach
    public void deleteAllPackages() {
        clearPackages();
    }
}
