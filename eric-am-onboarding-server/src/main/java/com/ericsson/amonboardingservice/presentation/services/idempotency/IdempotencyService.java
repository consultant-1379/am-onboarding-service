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
package com.ericsson.amonboardingservice.presentation.services.idempotency;

import java.util.function.Supplier;

import org.springframework.http.ResponseEntity;

import com.ericsson.amonboardingservice.presentation.models.entity.RequestProcessingDetails;

import jakarta.servlet.http.HttpServletResponse;

public interface IdempotencyService {

    <T> ResponseEntity<T> executeTransactionalIdempotentCall(Supplier<ResponseEntity<T>> supplier);

    ResponseEntity<String> buildResponseEntity(RequestProcessingDetails requestProcessingDetails);

    RequestProcessingDetails getRequestProcessingDetails(String idempotencyKey);

    <T> void saveIdempotentResponse(String idempotencyKey, ResponseEntity<T> response);

    void createProcessingRequest(String idempotencyKey, String requestHash, Integer retryAfter);

    void updateResponseWithProcessedData(HttpServletResponse response, RequestProcessingDetails details);

    void updateProcessingRequestCreationTime(RequestProcessingDetails requestProcessingDetails);
}
