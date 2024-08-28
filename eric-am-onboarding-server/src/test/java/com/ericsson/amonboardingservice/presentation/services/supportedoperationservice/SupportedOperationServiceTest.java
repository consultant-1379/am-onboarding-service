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
package com.ericsson.amonboardingservice.presentation.services.supportedoperationservice;

import com.ericsson.am.shared.vnfd.model.OperationDetail;
import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.am.shared.vnfd.validation.ToscaSupportedOperationValidator;
import com.ericsson.amonboardingservice.model.OperationDetailResponse;
import com.ericsson.amonboardingservice.presentation.exceptions.DataNotFoundException;
import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.models.vnfd.OperationDetailEntity;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.repositories.OperationDetailEntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.ericsson.amonboardingservice.TestUtils.readDataFromFile;
import static com.ericsson.amonboardingservice.utils.SupportedOperationUtils.buildOperationDetailEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {SupportedOperationServiceImpl.class})
public class SupportedOperationServiceTest {
    private static final String TEST_PACKAGE_ID = "testPackageId";
    private static final String VDU_SCALING_DELTA_NOT_DEFINED = "Exception happened during check if operation is supported: VduScalingDelta not "
            + "defined for aspect Aspect4";

    @Autowired
    private SupportedOperationService supportedOperationService;

    @MockBean
    private OperationDetailEntityRepository operationDetailEntityRepository;

    @MockBean
    private AppPackageRepository appPackageRepository;

    @Test
    public void testGetOperationDetailListVnfdTosca1dot2() throws IOException {
        setUpAppPackage("vnfd/valid_vnfd.yaml",
                AppPackage.OnboardingStateEnum.ONBOARDED);

        List<OperationDetailResponse> actualSupportedOperations = supportedOperationService.getSupportedOperations(TEST_PACKAGE_ID);

        assertThat(actualSupportedOperations).isNotNull().hasSize(9);

        Map<String, Boolean> expectedOperationsMap = getExpectedOperations();
        expectedOperationsMap.put(LCMOperationsEnum.HEAL.getOperation(), false);
        expectedOperationsMap.put(LCMOperationsEnum.SCALE.getOperation(), false);
        expectedOperationsMap.put(LCMOperationsEnum.ROLLBACK.getOperation(), false);
        expectedOperationsMap.put(LCMOperationsEnum.CHANGE_CURRENT_PACKAGE.getOperation(), true);

        assertOperationDetailsMatchExpected(actualSupportedOperations, expectedOperationsMap);
    }

    @Test
    public void testGetOperationDetailEmptyListWhenNotOnboarded() throws IOException {
        setUpAppPackage("vnfd/valid_vnfd.yaml",
                AppPackage.OnboardingStateEnum.PROCESSING);

        List<OperationDetailResponse> actualSupportedOperations = supportedOperationService.getSupportedOperations(TEST_PACKAGE_ID);

        assertThat(actualSupportedOperations).isNotNull().hasSize(0);
    }

    @Test
    public void testGetOperationDetailListVnfdTosca1dot3() throws IOException {
       setUpAppPackage("vnfd/tosca_1_3/valid_tosca_1_3_multi_charts.yaml",
               AppPackage.OnboardingStateEnum.ONBOARDED);

        List<OperationDetailResponse> actualSupportedOperations = supportedOperationService.getSupportedOperations(TEST_PACKAGE_ID);
        assertThat(actualSupportedOperations).isNotNull().hasSize(9);

        Map<String, Boolean> expectedOperationsMap = getExpectedOperations();

        assertOperationDetailsMatchExpected(actualSupportedOperations, expectedOperationsMap);
    }

    @Test
    public void testDeleteOperationDetailList() throws IOException {
        AppPackage appPackage = setUpAppPackage("vnfd/tosca_1_3/valid_tosca_1_3_multi_charts.yaml",
                AppPackage.OnboardingStateEnum.ONBOARDED);
        List<OperationDetailEntity> operationDetails = supportedOperationService.parsePackageSupportedOperations(appPackage);
        when(operationDetailEntityRepository.findByAppPackagePackageId(TEST_PACKAGE_ID)).thenReturn(operationDetails);
        supportedOperationService.deleteSupportedOperations(TEST_PACKAGE_ID);
        verify(operationDetailEntityRepository, times(1)).deleteAll(operationDetails);
    }

    @Test
    public void testGetOperationDetailListTosca1dot3WrongPolicies() throws IOException {
        setUpAppPackage("vnfd/tosca_1_3/vnfd_tosca_1_3_multi_b_wrong_policies.yaml",
                AppPackage.OnboardingStateEnum.ONBOARDED);

        List<OperationDetailResponse> actualSupportedOperations = supportedOperationService.getSupportedOperations(TEST_PACKAGE_ID);
        assertThat(actualSupportedOperations).isNotNull().hasSize(9);

        Map<String, Boolean> expectedOperationsMap = getExpectedOperations();
        expectedOperationsMap.put(LCMOperationsEnum.HEAL.getOperation(), false);
        expectedOperationsMap.put(LCMOperationsEnum.SCALE.getOperation(), false);
        expectedOperationsMap.put(LCMOperationsEnum.ROLLBACK.getOperation(), false);
        expectedOperationsMap.put(LCMOperationsEnum.CHANGE_CURRENT_PACKAGE.getOperation(), true);

        assertOperationDetailsMatchExpected(actualSupportedOperations, expectedOperationsMap);
        assertIsNotSupportedWithError(actualSupportedOperations, LCMOperationsEnum.ROLLBACK, VDU_SCALING_DELTA_NOT_DEFINED);
        assertIsNotSupportedWithError(actualSupportedOperations, LCMOperationsEnum.SCALE, VDU_SCALING_DELTA_NOT_DEFINED);
    }

    @Test
    public void testGetOperationDetailListTosca1dot2WrongRollback() throws IOException {
        setUpAppPackage("vnfd/invalid_vnfd_with_rollback_interface_and_without_rollback_policies.yaml",
                AppPackage.OnboardingStateEnum.ONBOARDED);

        List<OperationDetailResponse> actualSupportedOperations = supportedOperationService.getSupportedOperations(TEST_PACKAGE_ID);
        assertThat(actualSupportedOperations).isNotNull().hasSize(9);

        Map<String, Boolean> expectedOperationsMap = getExpectedOperations();
        expectedOperationsMap.put(LCMOperationsEnum.HEAL.getOperation(), false);
        expectedOperationsMap.put(LCMOperationsEnum.SCALE.getOperation(), false);
        expectedOperationsMap.put(LCMOperationsEnum.ROLLBACK.getOperation(), false);
        expectedOperationsMap.put(LCMOperationsEnum.CHANGE_CURRENT_PACKAGE.getOperation(), true);

        assertOperationDetailsMatchExpected(actualSupportedOperations, expectedOperationsMap);
        assertIsNotSupportedWithError(actualSupportedOperations, LCMOperationsEnum.ROLLBACK,
                "Exception happened during check if operation is supported: VNF package change policy not " +
                        "defined for VNF package change interface");
    }

    @Test
    public void testAppPackageNotFoundById() {
        when(appPackageRepository.findByPackageId(TEST_PACKAGE_ID))
                .thenReturn(Optional.empty());
        DataNotFoundException dataNotFoundException = assertThrows(DataNotFoundException.class,
                                                                   () -> supportedOperationService.getSupportedOperations(TEST_PACKAGE_ID));
        assertEquals("Package with id: testPackageId not found", dataNotFoundException.getMessage());
    }

    @Test
    public void testGetOperationDetailListFailEmptyVnfd() {
        when(operationDetailEntityRepository.findByAppPackagePackageId(TEST_PACKAGE_ID))
                .thenReturn(new ArrayList<>());
        AppPackage appPackage = new AppPackage();
        appPackage.setOnboardingState(AppPackage.OnboardingStateEnum.ONBOARDED);
        when(appPackageRepository.findByPackageIdNotBeingDeleted(TEST_PACKAGE_ID))
                .thenReturn(Optional.of(appPackage));
        InternalRuntimeException internalRuntimeException = assertThrows(InternalRuntimeException.class,
                () -> supportedOperationService.getSupportedOperations(TEST_PACKAGE_ID));
        assertEquals("VNFD file is not found for package id null", internalRuntimeException.getMessage());

    }

    private AppPackage setUpAppPackage(String filePath, AppPackage.OnboardingStateEnum onboardingState) throws IOException {
        String vnfd = readDataFromFile(filePath, StandardCharsets.UTF_8);
        AppPackage appPackage = getAppPackage(filePath, onboardingState);
        when(appPackageRepository.findByPackageIdNotBeingDeleted(TEST_PACKAGE_ID))
                .thenReturn(Optional.of(appPackage));
        when(appPackageRepository.save(any()))
                .thenReturn(buildAppPackage(vnfd));
        return appPackage;
    }

    private AppPackage getAppPackage(String filePath, AppPackage.OnboardingStateEnum onboardingState) throws IOException {
        String vnfd = readDataFromFile(filePath, StandardCharsets.UTF_8);
        AppPackage appPackage = new AppPackage();
        appPackage.setDescriptorModel(vnfd);
        appPackage.setPackageId(TEST_PACKAGE_ID);
        appPackage.setOnboardingState(onboardingState);

        return appPackage;
    }

    private AppPackage buildAppPackage(String vnfd) {
        AppPackage appPackage = new AppPackage();
        appPackage.setDescriptorModel(vnfd);
        appPackage.setPackageId(TEST_PACKAGE_ID);
        appPackage.setOperationDetails(getOperationDetailEntityList(vnfd, appPackage));
        return appPackage;
    }

    private List<OperationDetailEntity> getOperationDetailEntityList(String vnfd, AppPackage appPackage) {
        List<OperationDetail> vnfdSupportedOperations = ToscaSupportedOperationValidator.getVnfdSupportedOperations(vnfd);
        return vnfdSupportedOperations
                .stream()
                .map(operationDetail -> buildOperationDetailEntity(appPackage, operationDetail))
                .collect(Collectors.toList());
    }


    private void assertOperationDetailsMatchExpected(List<OperationDetailResponse> supportedOperations,
                                                     Map<String, Boolean> expectedOperations) {
        for (OperationDetailResponse operationDetail : supportedOperations) {
            String operationName = operationDetail.getOperationName();
            assertEquals(expectedOperations.get(operationName), operationDetail.getSupported());
        }
    }

    private Map<String, Boolean> getExpectedOperations() {
        Map<String, Boolean> expectedOperationsMap = new HashMap<>();
        expectedOperationsMap.put(LCMOperationsEnum.INSTANTIATE.getOperation(), true);
        expectedOperationsMap.put(LCMOperationsEnum.TERMINATE.getOperation(), true);
        expectedOperationsMap.put(LCMOperationsEnum.SCALE.getOperation(), true);
        expectedOperationsMap.put(LCMOperationsEnum.MODIFY_INFO.getOperation(), true);
        expectedOperationsMap.put(LCMOperationsEnum.CHANGE_VNFPKG.getOperation(), true);
        expectedOperationsMap.put(LCMOperationsEnum.CHANGE_CURRENT_PACKAGE.getOperation(), true);
        expectedOperationsMap.put(LCMOperationsEnum.HEAL.getOperation(), true);
        expectedOperationsMap.put(LCMOperationsEnum.ROLLBACK.getOperation(), true);
        expectedOperationsMap.put(LCMOperationsEnum.SYNC.getOperation(), true);
        return expectedOperationsMap;
    }

    private void assertIsNotSupportedWithError(List<OperationDetailResponse> operationDetails,
                                               LCMOperationsEnum operation, String errorMessage) {
        assertThat(operationDetails.stream().anyMatch(operationDetail ->
                operationDetail.getOperationName().equals(operation.getOperation())
                        && !operationDetail.getSupported()
                        && operationDetail.getError().equals(errorMessage))).isTrue();
    }
}
