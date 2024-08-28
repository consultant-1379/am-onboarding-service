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

import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.amonboardingservice.model.AdditionalPropertyResponse;
import com.ericsson.amonboardingservice.model.AppPackageList;
import com.ericsson.amonboardingservice.model.OperationDetailResponse;
import com.ericsson.amonboardingservice.model.ServiceModelRecordResponse;
import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static com.ericsson.amonboardingservice.utilities.AccTestUtils.isRunLocal;
import static com.ericsson.amonboardingservice.utilities.RestUtils.COOKIE;
import static com.ericsson.amonboardingservice.utilities.RestUtils.JSESSIONID_TEMPLATE;
import static com.ericsson.amonboardingservice.utilities.RestUtils.TOKEN;
import static com.ericsson.amonboardingservice.utilities.RestUtils.httpGetCall;
import static com.ericsson.amonboardingservice.utilities.RestUtils.httpGetCallMap;
import static com.ericsson.amonboardingservice.utilities.RestUtils.httpPostCall;
import static com.ericsson.amonboardingservice.utilities.TestConstants.BASE_URI;
import static com.ericsson.amonboardingservice.utilities.TestConstants.HOST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class PackageClient {
    public static final String PACKAGES_URI = "/packages";
    public static final String PACKAGES_URL = HOST + BASE_URI + PACKAGES_URI;
    public static final String ADDITIONAL_PARAMETERS_KEY = "additionalParameters";
    public static final String SERVICE_MODEL_KEY = "serviceModel";
    public static final String SUPPORTED_OPERATIONS_KEY = "supportedOperations";

    private PackageClient() {
    }

    public static ResponseEntity<AppPackageList> getAllPackages() {
        return httpGetCall(PACKAGES_URL, AppPackageList.class);
    }

    public static ResponseEntity<String> postPackage(FileSystemResource csarPackage) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MULTIPART_FORM_DATA);
        if (isRunLocal()) {
            httpHeaders.set(COOKIE, String.format(JSESSIONID_TEMPLATE, TOKEN));
        }
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("PackageContents", csarPackage);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, httpHeaders);

        LOGGER.info("Executing onboarding REST request {} to host: {}", requestEntity, PACKAGES_URL);
        return httpPostCall(PACKAGES_URL, requestEntity, String.class);
    }

    public static List<AdditionalPropertyResponse> getAdditionalParameters(String packageId,
                                                                           String operation,
                                                                           List<String> destinationDescriptorIds) {
        LOGGER.info("Retrieving additional parameters for package  {}: operation = {}, destinationDescriptorIds = {}",
                    packageId, operation, destinationDescriptorIds);
        String additionalPropertyPathParam = packageId + "/" + operation;
        String uri = getUri(ADDITIONAL_PARAMETERS_KEY, additionalPropertyPathParam);

        List<AdditionalPropertyResponse> additionalPropertyList = new ArrayList<>();

        if (LCMOperationsEnum.ROLLBACK.getOperation().equals(operation)) {
            List<AdditionalPropertyResponse> rollbackAdditionalPropertyResponses
                    = getRollbackAdditionalPropertyResponses(destinationDescriptorIds, uri);
            additionalPropertyList.addAll(rollbackAdditionalPropertyResponses);
        } else {
            ResponseEntity<List<AdditionalPropertyResponse>> responseEntity
                    = httpGetCall(uri, new ParameterizedTypeReference<>() { });
            checkAdditionalPropertyResponseList(responseEntity);
            if (responseEntity.getBody() != null) {
                additionalPropertyList.addAll(responseEntity.getBody());
            }
        }
        return additionalPropertyList;
    }

    private static List<AdditionalPropertyResponse> getRollbackAdditionalPropertyResponses(List<String> destinationDescriptorIds, String uri) {
        List<AdditionalPropertyResponse> additionalPropertyList = new ArrayList<>();
        if (destinationDescriptorIds.isEmpty()) {

            ResponseEntity<Map<Object, Object>> failedRollbackAdditionalPropertiesResponse = httpGetCallMap(uri, new ParameterizedTypeReference<>() {
            });
            checkAdditionalPropertyFailResponse(failedRollbackAdditionalPropertiesResponse);
        } else {
            String destinationDescriptorId = destinationDescriptorIds
                    .stream()
                    .findAny()
                    .orElseThrow(() -> new IllegalArgumentException("Destination descriptor Id not found"));
            String rollbackAdditionalPropertyUri = buildUriForGettingRollbackAdditionalProperty(uri, destinationDescriptorId);
            ResponseEntity<List<AdditionalPropertyResponse>> responseEntity = httpGetCall(rollbackAdditionalPropertyUri,
                                                                                          new ParameterizedTypeReference<>() { });
            checkAdditionalPropertyResponseList(responseEntity);
            additionalPropertyList.addAll(responseEntity.getBody());
        }
        return additionalPropertyList;
    }

    private static String buildUriForGettingRollbackAdditionalProperty(String uri, String destinationId) {
        return uri + "?targetDescriptorId=" + destinationId;
    }

    private static void checkAdditionalPropertyFailResponse(ResponseEntity<Map<Object, Object>> responseEntity) {
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertNotNull(responseEntity);
        Map<Object, Object> mapResponseEntityBody = responseEntity.getBody(); // NOSONAR
        assertNotNull(mapResponseEntityBody);
        assertEquals("VNFD does not support operation rollback. Node type interfaces are invalid", mapResponseEntityBody.get("message")); // NOSONAR
    }

    private static void checkAdditionalPropertyResponseList(ResponseEntity<List<AdditionalPropertyResponse>> responseEntity) {
        assertNotNull(responseEntity, "The response of getting additional parameters for operation should not be empty");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
    }

    public static ServiceModelRecordResponse getServiceModel(String packageId) {
        ResponseEntity<ServiceModelRecordResponse> responseEntity = httpGetCall(getUri(SERVICE_MODEL_KEY, packageId),
                                                                                ServiceModelRecordResponse.class);
        assertNotNull(responseEntity, "The response of getting Service Model for package should not be empty");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        ServiceModelRecordResponse serviceModelRecordResponse = responseEntity.getBody();
        assertNotNull(serviceModelRecordResponse);
        return serviceModelRecordResponse;
    }

    public static List<OperationDetailResponse> getSupportedOperations(String packageId) {
        ResponseEntity<List<OperationDetailResponse>> responseEntity = httpGetCall(getUri(SUPPORTED_OPERATIONS_KEY, packageId),
                                                                                   new ParameterizedTypeReference<>() { });
        assertNotNull(responseEntity, "The response of getting the list of supported operations for package should not be empty");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        List<OperationDetailResponse> operationDetailResponseList = responseEntity.getBody();
        assertNotNull(operationDetailResponseList);
        return operationDetailResponseList;
    }

    public static String getUri(String key, String pathParam) {
        Map<String, String> map = ImmutableMap
                .of(
                        ADDITIONAL_PARAMETERS_KEY, String.format(PACKAGES_URL + "/%s/additional_parameters", pathParam),
                        SERVICE_MODEL_KEY, String.format(PACKAGES_URL + "/%s/service_model", pathParam),
                        SUPPORTED_OPERATIONS_KEY, String.format(PACKAGES_URL + "/%s/supported_operations", pathParam)
                );
        return map.get(key);
    }

    public static ResponseEntity<String> getPackage(String packageId) {
        String packageUrl = PACKAGES_URL + File.separator + packageId;
        return httpGetCall(packageUrl, String.class);
    }
}
