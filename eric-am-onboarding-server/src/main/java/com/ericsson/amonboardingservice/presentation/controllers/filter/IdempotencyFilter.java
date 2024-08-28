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
package com.ericsson.amonboardingservice.presentation.controllers.filter;


import com.ericsson.amonboardingservice.presentation.controllers.filter.cachewrapper.CachedBodyRequestWrapper;
import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.presentation.models.ProcessingState;
import com.ericsson.amonboardingservice.presentation.models.entity.RequestProcessingDetails;
import com.ericsson.amonboardingservice.presentation.services.idempotency.IdempotencyService;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.DatatypeConverter;

import com.ericsson.amonboardingservice.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.util.WebUtils;


@Slf4j
@Component
@Order(1)
public class IdempotencyFilter extends OncePerRequestFilter {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private IdempotencyProps idempotencyProps;

    private static final List<String> IDEMPOTENT_METHODS = Arrays.asList("POST", "DELETE");
    private static final Integer DEFAULT_RETRY_AFTER = 30;

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest,
                                    HttpServletResponse httpServletResponse,
                                    FilterChain filterChain) throws ServletException, IOException {

        String idempotencyKey = httpServletRequest.getHeader(Constants.IDEMPOTENCY_KEY_HEADER);

        if (idempotencyKey == null) {
            LOGGER.info("Idempotency key isn't presented as header value for request {}", httpServletRequest.getRequestURI());
            filterChain.doFilter(httpServletRequest, httpServletResponse);
            return;
        }

        HttpServletRequest currentRequest = mapRequest(httpServletRequest);
        String hash;
        try {
            hash = calculateRequestHash(currentRequest);
        } catch (NoSuchAlgorithmException e) {
            throw new InternalRuntimeException(
                    String.format("Failed to generate digest for %s due to %s", httpServletRequest.getRequestURI(), e.getMessage()), e);
        }

        RequestProcessingDetails requestProcessingDetails = idempotencyService.getRequestProcessingDetails(idempotencyKey);

        if (requestProcessingDetails == null) {
            Integer retryAfter = calculateRetryAfterValue(currentRequest.getRequestURI(), currentRequest.getMethod());
            idempotencyService.createProcessingRequest(idempotencyKey, hash, retryAfter);
            filterChain.doFilter(currentRequest, httpServletResponse);
        } else {
            proceedWithExistedRequestDetails(currentRequest, httpServletResponse, filterChain, requestProcessingDetails, hash);
        }
    }

    private static HttpServletRequest mapRequest(final HttpServletRequest httpServletRequest) throws IOException {
        if (WebUtils.getNativeRequest(httpServletRequest, MultipartHttpServletRequest.class) == null) {
            return new CachedBodyRequestWrapper(httpServletRequest);
        }
        return httpServletRequest;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !IDEMPOTENT_METHODS.contains(request.getMethod());
    }

    private void proceedWithExistedRequestDetails(final HttpServletRequest httpServletRequest,
                                                  final HttpServletResponse httpServletResponse,
                                                  final FilterChain filterChain,
                                                  final RequestProcessingDetails requestProcessingDetails,
                                                  final String hash) throws IOException, ServletException {
        if (requestProcessingDetails.getRequestHash().equals(hash)) {
            if (requestProcessingDetails.getProcessingState() == ProcessingState.STARTED) {
                proceedWithStartedRequest(httpServletRequest, httpServletResponse, filterChain, requestProcessingDetails);
            } else {
                idempotencyService.updateResponseWithProcessedData(httpServletResponse, requestProcessingDetails);
            }
        } else {
            LOGGER.error("Request with the same idempotency key, but with different hash already exist for {}", httpServletRequest.getRequestURI());
            httpServletResponse.sendError(HttpStatus.UNPROCESSABLE_ENTITY.value());
        }
    }

    private void proceedWithStartedRequest(final HttpServletRequest httpServletRequest,
                                           final HttpServletResponse httpServletResponse,
                                           final FilterChain filterChain,
                                           final RequestProcessingDetails requestProcessingDetails) throws IOException, ServletException {
        LocalDateTime creationTime = requestProcessingDetails.getCreationTime();
        if (LocalDateTime.now(ZoneOffset.UTC).isAfter(creationTime.plusSeconds(2L * requestProcessingDetails.getRetryAfter()))) {
            idempotencyService.updateProcessingRequestCreationTime(requestProcessingDetails);
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        } else {
            LOGGER.info("Request {} processing still in progress", httpServletRequest.getRequestURI());
            httpServletResponse.setStatus(HttpStatus.TOO_EARLY.value());
            httpServletResponse.addHeader(HttpHeaders.RETRY_AFTER, requestProcessingDetails.getRetryAfter().toString());
        }
    }

    private static String calculateRequestHash(final HttpServletRequest httpServletRequest) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(MessageDigestAlgorithms.SHA_256);

        digest.update(httpServletRequest.getRequestURI().getBytes());
        digest.update(httpServletRequest.getMethod().getBytes());
        digest.update(IOUtils.toByteArray(httpServletRequest.getInputStream()));

        byte[] hash = digest.digest();
        String stringHash = DatatypeConverter.printHexBinary(hash).toLowerCase();

        LOGGER.debug("Hash {} has been calculated for request {}", hash, httpServletRequest.getRequestURI());
        return stringHash;
    }

    private Integer calculateRetryAfterValue(String requestUri, String method) {
        Integer requestLatency = idempotencyProps.findEndpointLatency(requestUri, method);

        return requestLatency != null ? requestLatency : DEFAULT_RETRY_AFTER;
    }
}
