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
package com.ericsson.amonboardingservice.utils;

import com.ericsson.am.shared.vnfd.model.OperationDetail;
import com.ericsson.amonboardingservice.model.OperationDetailResponse;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.models.vnfd.OperationDetailEntity;

import java.util.List;
import java.util.stream.Collectors;

public final class SupportedOperationUtils {
    private SupportedOperationUtils() {
    }

    public static List<OperationDetailResponse> mapOperationDetailsEntityToResponse(List<OperationDetailEntity> operationDetails) {
        return operationDetails.stream()
                .map(operation -> buildOperationDetailResponse(
                        operation.getOperationName(),
                        operation.getErrorMessage(),
                        operation.isSupported()))
                .collect(Collectors.toList());
    }

    public static OperationDetailEntity buildOperationDetailEntity(AppPackage appPackage, OperationDetail operationDetail) {
        OperationDetailEntity operationDetailEntity = new OperationDetailEntity();
        operationDetailEntity.setSupported(operationDetail.isSupported());
        operationDetailEntity.setAppPackage(appPackage);
        operationDetailEntity.setOperationName(operationDetail.getOperationName());
        operationDetailEntity.setErrorMessage(operationDetail.getErrorMessage());
        return operationDetailEntity;
    }

    public static OperationDetailResponse buildOperationDetailResponse(String operation, String error, boolean isSupported) {
        OperationDetailResponse operationDetailResponse = new OperationDetailResponse();
        operationDetailResponse.setOperationName(operation);
        operationDetailResponse.setError(error);
        operationDetailResponse.setSupported(isSupported);
        return operationDetailResponse;
    }
}
