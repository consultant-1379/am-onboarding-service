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

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static com.ericsson.amonboardingservice.presentation.models.license.Permission.CLUSTER_MANAGEMENT;
import static com.ericsson.amonboardingservice.presentation.models.license.Permission.ENM_INTEGRATION;
import static com.ericsson.amonboardingservice.presentation.models.license.Permission.LCM_OPERATIONS;
import static com.ericsson.amonboardingservice.presentation.models.license.Permission.ONBOARDING;

import java.util.EnumSet;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;

import com.ericsson.amonboardingservice.presentation.exceptions.MissingLicensePermissionException;
import com.ericsson.amonboardingservice.presentation.models.license.Permission;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.services.license.LicenseConsumerService;
import com.ericsson.amonboardingservice.utils.Constants;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
public class VnfPackageLicenseInterceptorTest {

    private static final EnumSet<Permission> HARDCODED_PERMISSION_SET = EnumSet
            .of(ONBOARDING, LCM_OPERATIONS, ENM_INTEGRATION, CLUSTER_MANAGEMENT);

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Object handler;

    @Mock
    private LicenseConsumerService licenseConsumerService;

    @Mock
    private AppPackageRepository appPackageRepository;

    private VnfPackageLicenseInterceptor vnfPackageLicenseInterceptor;

    @Test
    public void testGetMethod() {
        vnfPackageLicenseInterceptor = new VnfPackageLicenseInterceptor(licenseConsumerService, true, appPackageRepository);
        when(request.getMethod()).thenReturn(HttpMethod.GET.name());

        boolean result = vnfPackageLicenseInterceptor.preHandle(request, response, handler);
        verifyNoInteractions(licenseConsumerService);
        assertTrue(result);
    }

    @Test
    public void testPostMethodWithAllPermissions() {
        vnfPackageLicenseInterceptor = new VnfPackageLicenseInterceptor(licenseConsumerService, true, appPackageRepository);
        when(request.getMethod()).thenReturn(HttpMethod.POST.name());
        when(licenseConsumerService.getPermissions()).thenReturn(HARDCODED_PERMISSION_SET);

        boolean result = vnfPackageLicenseInterceptor.preHandle(request, response, handler);
        assertTrue(result);
    }

    @Test
    public void testPutMethodWithoutOnboardingPermission() {
        vnfPackageLicenseInterceptor = new VnfPackageLicenseInterceptor(licenseConsumerService, true, appPackageRepository);
        when(request.getMethod()).thenReturn(HttpMethod.PUT.name());
        when(licenseConsumerService.getPermissions()).thenReturn(EnumSet.of(CLUSTER_MANAGEMENT, LCM_OPERATIONS));
        when(appPackageRepository.count()).thenReturn(6L);

        MissingLicensePermissionException illegalNumberOfResources = assertThrows(MissingLicensePermissionException.class,
                                                                                                  () -> vnfPackageLicenseInterceptor.preHandle(request, response, handler));
        assertEquals(Constants.ILLEGAL_NUMBER_OF_RESOURCES_ERROR_MESSAGE, illegalNumberOfResources.getMessage());
    }

    @Test
    public void testDeleteMethodWithoutPermissions() {
        vnfPackageLicenseInterceptor = new VnfPackageLicenseInterceptor(licenseConsumerService, true, appPackageRepository);
        when(request.getMethod()).thenReturn(HttpMethod.DELETE.name());
        when(request.getRequestURI()).thenReturn("/api/vnfpkgm/v1/vnf_packages/packageId");
        when(licenseConsumerService.getPermissions()).thenReturn(EnumSet.noneOf(Permission.class));

        boolean result = vnfPackageLicenseInterceptor.preHandle(request, response, handler);

        verifyNoInteractions(appPackageRepository);
        assertTrue(result);
    }

    @Test
    public void testLicenseCheckIsPerformed() {
        vnfPackageLicenseInterceptor = new VnfPackageLicenseInterceptor(licenseConsumerService, true, appPackageRepository);
        when(request.getMethod()).thenReturn(HttpMethod.POST.name());
        when(licenseConsumerService.getPermissions()).thenReturn(HARDCODED_PERMISSION_SET);

        boolean result = vnfPackageLicenseInterceptor.preHandle(request, response, handler);
        verify(licenseConsumerService).getPermissions();
        assertTrue(result);
    }

    @Test
    public void testLicenseCheckIsSkipped() {
        vnfPackageLicenseInterceptor = new VnfPackageLicenseInterceptor(licenseConsumerService, false, appPackageRepository);

        boolean result = vnfPackageLicenseInterceptor.preHandle(request, response, handler);
        verifyNoInteractions(licenseConsumerService);
        assertTrue(result);
    }

    @Test
    public void testPostMethodWithoutPermissionsWithFivePackages() {
        vnfPackageLicenseInterceptor = new VnfPackageLicenseInterceptor(licenseConsumerService, true, appPackageRepository);
        when(request.getMethod()).thenReturn(HttpMethod.POST.name());
        when(request.getRequestURI()).thenReturn("/api/vnfpkgm/v1/vnf_packages");
        when(licenseConsumerService.getPermissions()).thenReturn(EnumSet.noneOf(Permission.class));
        when(appPackageRepository.count()).thenReturn(5L);

        MissingLicensePermissionException illegalNumberOfResources = assertThrows(MissingLicensePermissionException.class,
                () -> vnfPackageLicenseInterceptor.preHandle(request, response, handler));
        assertEquals(Constants.ILLEGAL_NUMBER_OF_RESOURCES_ERROR_MESSAGE, illegalNumberOfResources.getMessage());
    }

    @Test
    public void testPostMethodWithoutPermissionsWithFivePackagesWithSlash() {
        vnfPackageLicenseInterceptor = new VnfPackageLicenseInterceptor(licenseConsumerService, true, appPackageRepository);
        when(request.getMethod()).thenReturn(HttpMethod.POST.name());
        when(request.getRequestURI()).thenReturn("/api/vnfpkgm/v1/vnf_packages/");
        when(licenseConsumerService.getPermissions()).thenReturn(EnumSet.noneOf(Permission.class));
        when(appPackageRepository.count()).thenReturn(5L);

        MissingLicensePermissionException illegalNumberOfResources = Assert.assertThrows(MissingLicensePermissionException.class,
                () -> vnfPackageLicenseInterceptor.preHandle(request, response, handler));
        assertEquals(Constants.ILLEGAL_NUMBER_OF_RESOURCES_ERROR_MESSAGE, illegalNumberOfResources.getMessage());
    }
}