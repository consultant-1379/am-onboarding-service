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
import com.ericsson.amonboardingservice.presentation.controllers.ChartControllerImpl;
import com.ericsson.amonboardingservice.presentation.services.helmservice.HelmService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static com.ericsson.amonboardingservice.utils.JsonUtils.readJsonFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(SpringExtension.class)
public class ChartsOnboardingBase extends AbstractDbSetupTest {

    @Mock
    HelmService helmService;
    @InjectMocks
    ChartControllerImpl chartController;

    @BeforeEach
    public void setup() throws IOException{
        given(helmService.listCharts()).willReturn(readJsonFromResource("contracts/api/charts/onboarding/allChartDetails.json"));
        RestAssuredMockMvc.standaloneSetup(chartController);
    }

    @Test
    public void controllerLoaded() {
        assertThat(chartController).isNotNull();
    }
}
