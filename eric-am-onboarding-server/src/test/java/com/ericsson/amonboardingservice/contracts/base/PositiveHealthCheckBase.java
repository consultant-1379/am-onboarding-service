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

import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.am.shared.vnfd.service.ToscaoService;
import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.presentation.controllers.probes.PvcCheckHealthIndicator;
import com.ericsson.amonboardingservice.presentation.services.helmservice.HelmService;
import com.ericsson.amonboardingservice.presentation.services.imageservice.ImageService;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class PositiveHealthCheckBase extends AbstractDbSetupTest {
    @MockBean
    private ImageService imageService;

    @MockBean
    private ToscaoService toscaoService;

    @MockBean
    private HelmService helmService;

    @MockBean
    private PvcCheckHealthIndicator pvcCheckHealthIndicator;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    public void setup() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        String healthUp = "{health : true}";

        given(helmService.healthStatus()).willReturn(healthUp);
        given(imageService.healthStatus()).willReturn(healthUp);
        given(toscaoService.healthStatus()).willReturn(healthUp);
        given(pvcCheckHealthIndicator.health()).willReturn(Health.up().build());

        RestAssuredMockMvc.mockMvc(mockMvc);
    }
}
