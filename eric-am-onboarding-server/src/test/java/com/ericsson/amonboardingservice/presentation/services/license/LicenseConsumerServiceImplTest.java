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
package com.ericsson.amonboardingservice.presentation.services.license;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.context.RetryContextSupport;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.ericsson.amonboardingservice.presentation.models.license.Permission;

@SpringBootTest(classes = LicenseConsumerServiceImpl.class)
public class LicenseConsumerServiceImplTest {

    @MockBean
    @Qualifier("licenseRestTemplate")
    private RestTemplate restTemplate;

    @MockBean
    @Qualifier("licenseRetryTemplate")
    private RetryTemplate retryTemplate;

    @Autowired
    private LicenseConsumerServiceImpl licenseConsumerService;

    private final Permission[] mockedLicensePermissions = Permission.values();
    private final ResponseEntity<Permission[]> mockResponseEntity = new ResponseEntity<>(Permission.values(), HttpStatus.ACCEPTED);

    @Test
    public void fetchPermissionsTestWithNull() {
        ResponseEntity<Permission[]> mockedResponseEntityNullBody = new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        given(restTemplate.getForEntity(any(String.class), any(Class.class))).willReturn(mockedResponseEntityNullBody);
        assertThatThrownBy(() -> licenseConsumerService.fetchPermissions()).isInstanceOf(RestClientException.class);
    }

    @Test
    public void fetchPermissionsTestWithEmptyBody() {
        ResponseEntity<Permission[]> mockedResponseEntityNullBody = new ResponseEntity<>(null, HttpStatus.ACCEPTED);
        given(restTemplate.getForEntity(any(String.class), any(Class.class))).willReturn(mockedResponseEntityNullBody);
        assertThat(licenseConsumerService.fetchPermissions()).isEqualTo(EnumSet.noneOf(Permission.class));
    }

    @Test
    public void fetchPermissionsTestWithWrongStatusCode() {
        ResponseEntity<Permission[]> mockResponseEntityWithNotFoundStatus =
                new ResponseEntity<>(mockedLicensePermissions, HttpStatus.NOT_FOUND);
        given(restTemplate.getForEntity(any(String.class), any(Class.class))).willReturn(mockResponseEntityWithNotFoundStatus);
        assertThatThrownBy(() -> licenseConsumerService.fetchPermissions()).isInstanceOf(RestClientException.class);
    }

    @Test
    public void fetchPermissionsTestWithSuccess() {
        given(restTemplate.getForEntity(any(String.class), any(Class.class))).willReturn(mockResponseEntity);
        assertThat(licenseConsumerService.fetchPermissions()).isEqualTo(EnumSet.allOf(Permission.class));
    }

    @Test
    public void getPermissionsTestWithSuccess() {
        given(restTemplate.getForEntity(any(String.class), any(Class.class))).willReturn(mockResponseEntity);
        when(retryTemplate.execute(any(),any(),any())).thenAnswer(invocation -> {
            RetryCallback<EnumSet<Permission>, RestClientException> retry = invocation.getArgument(0);
            return retry.doWithRetry(new RetryContextSupport(null));
        });
        assertThat(licenseConsumerService.getPermissions()).isEqualTo(EnumSet.allOf(Permission.class));
    }

    @Test
    public void getPermissionsTestWithException() {
        when(retryTemplate.execute(any(),any(),any()))
                .thenThrow(new ResourceAccessException("Unable to retrieve license manager data due to: 500"));
        assertThat(licenseConsumerService.getPermissions()).isEqualTo(EnumSet.noneOf(Permission.class));
    }
}
