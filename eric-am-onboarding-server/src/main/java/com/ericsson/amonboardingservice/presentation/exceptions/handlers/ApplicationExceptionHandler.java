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
package com.ericsson.amonboardingservice.presentation.exceptions.handlers;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INSUFFICIENT_STORAGE;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;

import java.net.URI;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;

import com.ericsson.amonboardingservice.presentation.exceptions.InvalidPaginationQueryException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;

import com.ericsson.am.shared.vnfd.service.exception.ToscaoException;
import com.ericsson.amonboardingservice.model.ProblemDetails;
import com.ericsson.amonboardingservice.presentation.exceptions.ArtifactNotFoundException;
import com.ericsson.amonboardingservice.presentation.exceptions.AuthorizationException;
import com.ericsson.amonboardingservice.presentation.exceptions.DataNotFoundException;
import com.ericsson.amonboardingservice.presentation.exceptions.DockerServiceException;
import com.ericsson.amonboardingservice.presentation.exceptions.ErrorMessage;
import com.ericsson.amonboardingservice.presentation.exceptions.FailedOnboardingException;
import com.ericsson.amonboardingservice.presentation.exceptions.FailedOnboardingValidationException;
import com.ericsson.amonboardingservice.presentation.exceptions.HelmChartRegistryUnavailableException;
import com.ericsson.amonboardingservice.presentation.exceptions.IllegalPackageStateException;
import com.ericsson.amonboardingservice.presentation.exceptions.InsufficientDiskSpaceException;
import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.presentation.exceptions.InvalidRequestParameterException;
import com.ericsson.amonboardingservice.presentation.exceptions.MissingLicensePermissionException;
import com.ericsson.amonboardingservice.presentation.exceptions.PackageNotFoundException;
import com.ericsson.amonboardingservice.presentation.exceptions.ServiceModelRecordNotFoundException;
import com.ericsson.amonboardingservice.presentation.exceptions.SkopeoServiceException;
import com.ericsson.amonboardingservice.presentation.exceptions.StateConflictException;
import com.ericsson.amonboardingservice.presentation.exceptions.UnhandledException;
import com.ericsson.amonboardingservice.presentation.exceptions.UnsupportedMediaTypeException;
import com.ericsson.amonboardingservice.presentation.exceptions.UserInputException;
import com.ericsson.amonboardingservice.utils.Constants;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@ControllerAdvice
@Order(HIGHEST_PRECEDENCE)
public class ApplicationExceptionHandler {

    @ResponseBody
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Throwable.class)
    public ErrorMessage handle(Throwable e) {
        return handleException(e);
    }

    @ResponseBody
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorMessage handle(IllegalArgumentException e) {
        return handleException(e);
    }

    @ResponseBody
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(UnsupportedOperationException.class)
    public ErrorMessage handle(UnsupportedOperationException e) {
        return handleException(e);
    }

    @ResponseBody
    @ResponseStatus(UNSUPPORTED_MEDIA_TYPE)
    @ExceptionHandler(UnsupportedMediaTypeException.class)
    public ErrorMessage handle(UnsupportedMediaTypeException e) {
        return handleException(e);
    }

    @ResponseBody
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ExceptionHandler(DockerServiceException.class)
    public ErrorMessage handle(DockerServiceException e) {
        return handleException(e);
    }

    @ResponseBody
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ExceptionHandler(SkopeoServiceException.class)
    public ErrorMessage handle(SkopeoServiceException e) {
        return handleException(e);
    }

    @ResponseBody
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(ServletRequestBindingException.class)
    public ErrorMessage handle(ServletRequestBindingException e) {
        return handleException(e);
    }

    @ResponseBody
    @ResponseStatus(NOT_FOUND)
    @ExceptionHandler(HttpClientErrorException.class)
    public ErrorMessage handle(HttpClientErrorException e) {
        return handleException(e);
    }

    @ResponseBody
    @ResponseStatus(UNAUTHORIZED)
    @ExceptionHandler(AuthorizationException.class)
    public ErrorMessage handle(AuthorizationException e) {
        return handleException(e);
    }

    @ResponseBody
    @ResponseStatus(NOT_FOUND)
    @ExceptionHandler(DataNotFoundException.class)
    public ErrorMessage handle(DataNotFoundException e) {
        return handleException(e);
    }

    @ResponseBody
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(InternalRuntimeException.class)
    public ErrorMessage handle(InternalRuntimeException e) {
        return handleException(e);
    }

    @ResponseBody
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(InvalidRequestParameterException.class)
    public ErrorMessage handle(InvalidRequestParameterException e) {
        return handleException(e);
    }

    @ResponseBody
    @ResponseStatus(METHOD_NOT_ALLOWED)
    @ExceptionHandler(MissingLicensePermissionException.class)
    public ErrorMessage handle(MissingLicensePermissionException e) {
        return handleException(e);
    }

    @ExceptionHandler(PackageNotFoundException.class)
    public ResponseEntity<ProblemDetails> handlePackageNotFoundException(final PackageNotFoundException e) {
        return createProblemDetails(e, "Package not found", NOT_FOUND);
    }

    @ExceptionHandler(ArtifactNotFoundException.class)
    public ResponseEntity<ProblemDetails> handleArtifactNotFoundException(final ArtifactNotFoundException e) {
        return createProblemDetails(e, "ArtifactPath not found", NOT_FOUND);
    }

    @ExceptionHandler(UserInputException.class)
    public ResponseEntity<ProblemDetails> handleUserInputException(final UserInputException e) {
        return createProblemDetails(e, "User input is not correct", BAD_REQUEST);
    }

    @ExceptionHandler(ServiceModelRecordNotFoundException.class)
    public ResponseEntity<ProblemDetails> handleServiceModelRecordNotFoundException(final ServiceModelRecordNotFoundException e) {
        return createProblemDetails(e, "Service model record not found", NOT_FOUND);
    }

    @ExceptionHandler(InputMismatchException.class)
    public ResponseEntity<ProblemDetails> handleInputMismatchException(final InputMismatchException e) {
        return createProblemDetails(e, "Signed Csar contents are not correct", BAD_REQUEST);
    }

    @ExceptionHandler(StateConflictException.class)
    public ResponseEntity<ProblemDetails> handleStateConflictException(final StateConflictException e) {
        return createProblemDetails(e, "Conflicted state", CONFLICT);
    }

    @ExceptionHandler(UnhandledException.class)
    public ResponseEntity<ProblemDetails> handleUnhandledException(final UnhandledException e) {
        return createProblemDetails(e, "Internal Error", BAD_REQUEST);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ProblemDetails> handleHttpMediaTypeNotAcceptableException(final HttpMediaTypeNotAcceptableException e) {
        return createProblemDetails(e, "Media type not supported for this api", BAD_REQUEST);
    }

    @ExceptionHandler(IllegalPackageStateException.class)
    public ResponseEntity<ProblemDetails> handleIllegalPackageStateException(
            final IllegalPackageStateException e) {
        return createProblemDetails(e, "Invalid package state", CONFLICT);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetails> handle(HttpMessageNotReadableException e) {
        return createProblemDetails(e, "Malformed Request", BAD_REQUEST);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetails> handle(HttpMediaTypeNotSupportedException e) {
        return createProblemDetails(e, "Media type not supported", BAD_REQUEST);
    }

    @ExceptionHandler(FailedOnboardingException.class)
    public ResponseEntity<ProblemDetails> handle(FailedOnboardingException e) {
        return createProblemDetails(e, "Onboarding Failed", BAD_REQUEST);
    }

    @ExceptionHandler(InsufficientDiskSpaceException.class)
    public ResponseEntity<ProblemDetails> handle(InsufficientDiskSpaceException e) {
        return createProblemDetails(e, "Onboarding Failed", INSUFFICIENT_STORAGE);
    }

    @ExceptionHandler(HelmChartRegistryUnavailableException.class)
    public ResponseEntity<ProblemDetails> handle(HelmChartRegistryUnavailableException e) {
        return createProblemDetails(e, "Helm chart registry cannot be contacted", INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ToscaoException.class)
    public ResponseEntity<ProblemDetails> handlePackageNotFoundException(final ToscaoException e) {
        return createProblemDetails(e, "Error in Tosca service", INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(FailedOnboardingValidationException.class)
    public ResponseEntity<ProblemDetails> handle(FailedOnboardingValidationException e) {
        return createProblemDetails(e, "Failed validation during onboarding", BAD_REQUEST);
    }

    @ExceptionHandler(InvalidPaginationQueryException.class)
    public ResponseEntity<ProblemDetails> handleInvalidPaginationQueryException(InvalidPaginationQueryException ex) {
        return createProblemDetails(ex, "Invalid Pagination Query Parameter Exception", BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetails> handle(MethodArgumentNotValidException e) {
        final BindingResult bindingResult = e.getBindingResult();
        if (bindingResult.hasErrors()) {
            return createProblemDetails(e.toString(),
                                        createErrorMessage(bindingResult.getAllErrors()),
                                        "Mandatory parameter missing",
                                        HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static ResponseEntity<ProblemDetails> createProblemDetails(final String exceptionString,
                                                                      final String exceptionMessage,
                                                                      final String title,
                                                                      final HttpStatus httpStatus) {
        LOGGER.error("{} Occurred, {}", exceptionString, exceptionMessage);

        ProblemDetails problemDetails = new ProblemDetails(httpStatus.value(), exceptionMessage);
        problemDetails.setTitle(title);
        problemDetails.setType(URI.create(Constants.TYPE_BLANK));
        problemDetails.setInstance(URI.create(Constants.TYPE_BLANK));
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(problemDetails, responseHeaders, httpStatus);
    }

    public static ResponseEntity<ProblemDetails> createProblemDetails(final Throwable throwable,
                                                                      final String title,
                                                                      final HttpStatus httpStatus) {
        logThrowable(throwable);

        ProblemDetails problemDetails = new ProblemDetails(httpStatus.value(), throwable.getMessage());
        problemDetails.setTitle(title);
        problemDetails.setType(URI.create(Constants.TYPE_BLANK));
        problemDetails.setInstance(URI.create(Constants.TYPE_BLANK));
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(problemDetails, responseHeaders, httpStatus);
    }

    private static ErrorMessage handleException(final Throwable throwable) {
        logThrowable(throwable);
        return new ErrorMessage(throwable.getMessage());
    }

    private static void logThrowable(final Throwable throwable) {
        LOGGER.error("Error happened during request processing", throwable);
    }

    private static String createErrorMessage(List<ObjectError> allError) {
        List<String> fieldErrors = new ArrayList<>();
        allError.forEach(error -> {
            StringBuilder errorMessage = new StringBuilder();
            fieldErrors.add(errorMessage.append(((FieldError) error).getField())
                                    .append(" ")
                                    .append(error.getDefaultMessage()).toString());
        });

        return StringUtils.join(fieldErrors, ",");
    }
}
