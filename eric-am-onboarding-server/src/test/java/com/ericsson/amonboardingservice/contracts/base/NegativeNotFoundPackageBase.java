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
import com.ericsson.amonboardingservice.presentation.services.vnfpackageservice.VnfPackageService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.context.WebApplicationContext;

import jakarta.inject.Inject;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@SpringBootTest(classes = ApplicationServer.class)
public class NegativeNotFoundPackageBase extends AbstractDbSetupTest {
    @Inject
    private WebApplicationContext context;

    @MockBean
    VnfPackageService vnfPackageService;

    @BeforeEach
    public void setup()
    {
        given(vnfPackageService.getVnfPackage(anyString())).willReturn(Optional.ofNullable(null));
        RestAssuredMockMvc.webAppContextSetup(context);
    }


}
