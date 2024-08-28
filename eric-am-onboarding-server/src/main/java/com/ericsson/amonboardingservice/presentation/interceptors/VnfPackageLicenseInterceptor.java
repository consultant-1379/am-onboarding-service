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
package com.ericsson.amonboardingservice.presentation.interceptors;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import com.ericsson.amonboardingservice.presentation.exceptions.MissingLicensePermissionException;
import com.ericsson.amonboardingservice.presentation.models.license.Permission;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.services.license.LicenseConsumerService;
import com.ericsson.amonboardingservice.utils.Constants;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class VnfPackageLicenseInterceptor implements HandlerInterceptor {

    private static final List<String> METHODS_TO_INTERCEPT = Arrays.asList("POST", "PUT", "DELETE");
    private static final Set<String> ADDING_ENTITY_PATHS = Set.of("/api/vnfpkgm/v1/vnf_packages", "/api/vnfpkgm/v1/vnf_packages/");
    private static final String DELETE_ENTITY_PATH_REGEX = "^/api/vnfpkgm/v1/vnf_packages/[\\w-]+/?$";
    private final AppPackageRepository appPackageRepository;
    private final LicenseConsumerService licenseConsumerService;
    private final boolean restrictedMode;

    public VnfPackageLicenseInterceptor(final LicenseConsumerService licenseConsumerService,
                                        @Value("${onboarding.restrictedMode:true}") final boolean restrictedMode,
                                        final AppPackageRepository appPackageRepository) {

        this.licenseConsumerService = licenseConsumerService;
        this.restrictedMode = restrictedMode;
        this.appPackageRepository = appPackageRepository;
    }

    @Override
    public boolean preHandle(final HttpServletRequest request,
                             final @NonNull HttpServletResponse response,
                             final @NonNull Object handler) {

        if (restrictedMode && METHODS_TO_INTERCEPT.contains(request.getMethod())) {
            EnumSet<Permission> allowedPrivileges = licenseConsumerService.getPermissions();

            if (CollectionUtils.isEmpty(allowedPrivileges) || !allowedPrivileges.contains(Permission.ONBOARDING)) {
                checkOnboardingLimitsWithoutLicense(request);
            }
        }
        return true;
    }

    private void checkOnboardingLimitsWithoutLicense(HttpServletRequest request) {
        if (HttpMethod.DELETE.matches(request.getMethod())
                && request.getRequestURI().matches(DELETE_ENTITY_PATH_REGEX)) {
            return;
        }

        int numberOfPackages = (int) appPackageRepository.count();
        if (HttpMethod.POST.matches(request.getMethod())
                && ADDING_ENTITY_PATHS.contains(request.getRequestURI())
                && numberOfPackages >= Constants.ALLOWED_NUMBER_OF_PACKAGES) {
            throw new MissingLicensePermissionException(Constants.ILLEGAL_NUMBER_OF_RESOURCES_ERROR_MESSAGE);
        } else if (numberOfPackages > Constants.ALLOWED_NUMBER_OF_PACKAGES) {
            throw new MissingLicensePermissionException(Constants.ILLEGAL_NUMBER_OF_RESOURCES_ERROR_MESSAGE);
        }
    }
}
