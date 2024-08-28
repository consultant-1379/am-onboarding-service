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
package com.ericsson.amonboardingservice.presentation.services.servicemodelservice;

import java.util.Optional;
import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.am.shared.vnfd.model.servicemodel.ServiceModel;
import com.ericsson.amonboardingservice.model.ServiceModelRecordResponse;
import com.ericsson.amonboardingservice.presentation.models.ServiceModelRecordEntity;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.ServiceModelRecordRepository;
import com.ericsson.amonboardingservice.presentation.services.mapper.ServiceModelMapper;
import com.ericsson.amonboardingservice.presentation.services.onboarding.request.PackageUploadRequestContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ServiceModelServiceImpl implements ServiceModelService {

    public static final String SERVICE_MODEL_NOT_FOUND_MESSAGE = "Service model record for package with id: %s not found";

    @Autowired
    private ServiceModelRecordRepository serviceModelRecordRepository;

    @Autowired
    private ServiceModelMapper serviceModelMapper;

    @Autowired
    private Validator validator;

    @Override
    public Optional<ServiceModelRecordEntity> getServiceModelByPackageId(final String packageId) {
        LOGGER.info("Getting service model record with package id {}", packageId);
        Optional<ServiceModelRecordEntity> serviceModelRecordEntity = serviceModelRecordRepository.findByAppPackagePackageId(packageId);

        if (serviceModelRecordEntity.isEmpty()) {
            LOGGER.warn(String.format(SERVICE_MODEL_NOT_FOUND_MESSAGE, packageId));
        }
        return serviceModelRecordEntity;
    }

    @Override
    public Optional<ServiceModelRecordEntity> saveServiceModelFromRequestContext(final PackageUploadRequestContext context,
                                                                                 final AppPackage appPackage) {

        ServiceModel serviceModel = context.getServiceModel();
        ServiceModelRecordEntity serviceModelRecordEntity;

        if (serviceModel != null && context.getVnfd() != null) {
            String descriptorId = context.getVnfd().getVnfDescriptorId();
            serviceModelRecordEntity = new ServiceModelRecordEntity();
            serviceModelRecordEntity.setServiceModelId(serviceModel.getId());
            serviceModelRecordEntity.setServiceModelName(serviceModel.getName());
            serviceModelRecordEntity.setDescriptorId(descriptorId);
            serviceModelRecordEntity.setAppPackage(appPackage);
            validateEntity(serviceModelRecordEntity);

            return Optional.of(serviceModelRecordRepository.save(serviceModelRecordEntity));
        }

        return Optional.empty();
    }

    @Override
    public Optional<ServiceModelRecordResponse> getServiceModelResponseByPackageId(final String packageId) {
        return getServiceModelByPackageId(packageId).map(serviceModelRecordEntity -> serviceModelMapper
                .toServiceModelRecordResponse(serviceModelRecordEntity));
    }

    private void validateEntity(ServiceModelRecordEntity entity) {
        Set<ConstraintViolation<ServiceModelRecordEntity>> violations = validator.validate(entity);

        if (!violations.isEmpty()) {
            LOGGER.error("ServiceModelRecordEntity constraints validation failed.");
            throw new IllegalArgumentException(new ConstraintViolationException(violations));
        }
    }

}
