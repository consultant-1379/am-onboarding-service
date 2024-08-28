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
package com.ericsson.amonboardingservice.aspect;

import static com.ericsson.am.shared.http.HttpUtility.getCurrentHttpRequest;

import jakarta.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.presentation.services.idempotency.IdempotencyService;
import com.ericsson.amonboardingservice.utils.Constants;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
public class IdempotencyAspect {

    @Autowired
    private IdempotencyService idempotencyService;

    @Around(value = "com.ericsson.amonboardingservice.aspect.PointcutConfig.exceptionHandler()")
    public Object around(final ProceedingJoinPoint pjp) throws Throwable {
        final Object result = pjp.proceed();
        HttpServletRequest request;

        try {
            request = getCurrentHttpRequest();
        } catch (IllegalStateException ex) {
            LOGGER.warn(ex.getMessage(), ex);
            return result;
        }

        if (request == null) {
            LOGGER.warn("Unable to get incoming HttpServletRequest");
            return result;
        }

        String idempotencyKey = request.getHeader(Constants.IDEMPOTENCY_KEY_HEADER);

        if (idempotencyKey == null) {
            LOGGER.warn("Idempotency key is not present for call to {}", request.getRequestURI());
            return result;
        }

        final ResponseEntity<?> responseEntity;

        if (!result.getClass().equals(ResponseEntity.class)) {

            try {
                MethodSignature signature = (MethodSignature) pjp.getSignature();
                final ResponseStatus status = signature.getMethod().getAnnotation(ResponseStatus.class);

                if (status == null) {
                    LOGGER.warn("RequestURI {}, response does not equal class ResponseEntity, could not find ResponseStatus, response class {}",
                                request.getRequestURI(),
                                result.getClass());
                    return result;
                }

                final HttpStatus code = status.value();
                final HttpHeaders responseHeaders = new HttpHeaders();

                responseHeaders.setContentType(MediaType.APPLICATION_JSON);
                responseEntity = new ResponseEntity<>(result, responseHeaders, code);
            } catch (Exception exception) {
                LOGGER.warn(exception.getMessage(), exception);
                return result;
            }
        } else {
            responseEntity = (ResponseEntity<?>) result;
        }

        try {
            idempotencyService.saveIdempotentResponse(idempotencyKey, responseEntity);
        } catch (InternalRuntimeException ex) {
            LOGGER.error("The request processing details was not updated for idempotencyKey = {}", idempotencyKey);
        }

        return result;
    }
}
