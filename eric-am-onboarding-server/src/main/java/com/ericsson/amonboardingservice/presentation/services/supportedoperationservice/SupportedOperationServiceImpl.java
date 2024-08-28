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

import static com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage.OnboardingStateEnum.ONBOARDED;
import static com.ericsson.amonboardingservice.utils.SupportedOperationUtils.buildOperationDetailEntity;
import static com.ericsson.amonboardingservice.utils.SupportedOperationUtils.mapOperationDetailsEntityToResponse;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.am.shared.vnfd.model.OperationDetail;
import com.ericsson.am.shared.vnfd.validation.ToscaSupportedOperationValidator;
import com.ericsson.amonboardingservice.model.OperationDetailResponse;
import com.ericsson.amonboardingservice.presentation.exceptions.DataNotFoundException;
import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.models.vnfd.OperationDetailEntity;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.repositories.OperationDetailEntityRepository;
import com.ericsson.amonboardingservice.utils.Constants;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SupportedOperationServiceImpl implements SupportedOperationService {

    @Autowired
    private OperationDetailEntityRepository operationDetailEntityRepository;

    @Autowired
    private AppPackageRepository appPackageRepository;

    @Override
    @Transactional
    public List<OperationDetailResponse> getSupportedOperations(String packageId) {
        AppPackage appPackage = appPackageRepository.findByPackageIdNotBeingDeleted(packageId)
                .orElseThrow(() -> new DataNotFoundException(String.format(Constants.PACKAGE_NOT_PRESENT_ERROR_MESSAGE, packageId)));

        List<OperationDetailEntity> operationDetails = operationDetailEntityRepository.findByAppPackagePackageId(packageId);

        boolean isOnboarded = ONBOARDED.equals(appPackage.getOnboardingState());

        if (operationDetails.isEmpty() && isOnboarded) {
            operationDetails = parsePackageSupportedOperations(appPackage);
            operationDetailEntityRepository.saveAll(operationDetails);
        }
        return mapOperationDetailsEntityToResponse(operationDetails);
    }

    @Override
    @Transactional
    public void deleteSupportedOperations(String appPackageId) {
        List<OperationDetailEntity> operationDetails = operationDetailEntityRepository.findByAppPackagePackageId(appPackageId);
        if (operationDetails != null && !operationDetails.isEmpty()) {
            LOGGER.info("Deleting operationDetails for package id: {}", appPackageId);
            operationDetailEntityRepository.deleteAll(operationDetails);
        }
    }

    @Override
    public List<OperationDetailEntity> parsePackageSupportedOperations(AppPackage appPackage) {
        LOGGER.info("Package {} operation details parsing has been started", appPackage.getPackageId());

        String descriptorModel = appPackage.getDescriptorModel();
        if (StringUtils.isBlank(descriptorModel)) {
            LOGGER.error("AppPackage descriptor model id not found for package id {}", appPackage.getPackageId());
            throw new InternalRuntimeException(String.format(Constants.VNFD_FILE_IS_NOT_FOUND_FOR_PACKAGE_ERROR_MESSAGE, appPackage.getPackageId()));
        }
        List<OperationDetail> vnfdSupportedOperations = ToscaSupportedOperationValidator.getVnfdSupportedOperations(descriptorModel);
        return buildOperationDetailEntityList(appPackage, vnfdSupportedOperations);
    }

    private static List<OperationDetailEntity> buildOperationDetailEntityList(AppPackage appPackage, List<OperationDetail> operationDetails) {
        return operationDetails
                .stream()
                .map(operationDetail -> buildOperationDetailEntity(appPackage, operationDetail))
                .collect(Collectors.toList());
    }
}
