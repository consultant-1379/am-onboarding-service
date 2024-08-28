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
package com.ericsson.amonboardingservice.contracts.base;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.ApplicationServer;
import com.ericsson.amonboardingservice.presentation.exceptions.DataNotFoundException;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.context.WebApplicationContext;

import jakarta.inject.Inject;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@SpringBootTest(classes = ApplicationServer.class)
public class NegativeNotfoundBase extends AbstractDbSetupTest {

    @Inject
    private WebApplicationContext context;

    @MockBean
    private PackageService packageService;

    @BeforeEach
    public void setup(){
        given(packageService.getPackage(anyString())).willThrow(new DataNotFoundException("Package with id: \"a-non-existent-id\" not found"));
        RestAssuredMockMvc.webAppContextSetup(context);
    }
}
