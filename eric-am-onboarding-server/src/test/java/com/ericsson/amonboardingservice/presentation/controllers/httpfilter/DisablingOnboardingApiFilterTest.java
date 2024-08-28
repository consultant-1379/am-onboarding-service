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
package com.ericsson.amonboardingservice.presentation.controllers.httpfilter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;

@ExtendWith(MockitoExtension.class)
public class DisablingOnboardingApiFilterTest {
    private static final String SMALL_STACK_APPLICATION_ENABLED = "true";
    private static final String SMALL_STACK_APPLICATION_DISABLED = "false";

    private static final String VNF_PACKAGE_API = "/api/vnfpkgm/v1/vnf_packages";
    private static final String ACTUATOR = "/actuator/health";

    private DisablingOnboardingApiFilter disablingOnboardingApiFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    @ParameterizedTest
    @ValueSource(strings = {SMALL_STACK_APPLICATION_DISABLED, SMALL_STACK_APPLICATION_ENABLED})
    public void doFilterInternalShouldPassForGetMethod(String smallStackApplication) throws ServletException, IOException {
        disablingOnboardingApiFilter = new DisablingOnboardingApiFilter(smallStackApplication);

        when(request.getMethod()).thenReturn(HttpMethod.GET.name());

        disablingOnboardingApiFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void doFilterShouldPassForPostMethodWithSmallStackEnabled() throws ServletException, IOException {
        disablingOnboardingApiFilter = new DisablingOnboardingApiFilter(SMALL_STACK_APPLICATION_ENABLED);

        when(request.getMethod()).thenReturn(HttpMethod.POST.name());

        disablingOnboardingApiFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void doFilterShouldNotPassForPostMethodWithSmallStackDisabled() throws ServletException, IOException {
        disablingOnboardingApiFilter = new DisablingOnboardingApiFilter(SMALL_STACK_APPLICATION_DISABLED);

        when(request.getMethod()).thenReturn(HttpMethod.POST.name());

        disablingOnboardingApiFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(403);
    }

    @Test
    public void shouldNotFilterReturnsFalseForNotActuatorUrl() {
        disablingOnboardingApiFilter = new DisablingOnboardingApiFilter(SMALL_STACK_APPLICATION_ENABLED);

        when(request.getRequestURI()).thenReturn(VNF_PACKAGE_API);

        boolean shouldNotFilter = disablingOnboardingApiFilter.shouldNotFilter(request);

        assertFalse(shouldNotFilter);
    }

    @Test
    public void shouldNotFilterReturnsTrueForActuatorUrl() {
        disablingOnboardingApiFilter = new DisablingOnboardingApiFilter(SMALL_STACK_APPLICATION_ENABLED);

        when(request.getRequestURI()).thenReturn(ACTUATOR);

        boolean shouldNotFilter = disablingOnboardingApiFilter.shouldNotFilter(request);

        assertTrue(shouldNotFilter);
    }
}