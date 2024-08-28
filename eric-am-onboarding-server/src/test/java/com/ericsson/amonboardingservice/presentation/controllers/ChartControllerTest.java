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
package com.ericsson.amonboardingservice.presentation.controllers;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.infrastructure.configuration.HelmRegistryConfig;
import com.ericsson.amonboardingservice.infrastructure.client.RestClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.ericsson.amonboardingservice.utils.Utility.checkResponseMessage;
import static com.ericsson.amonboardingservice.utils.Utility.readDataFromFile;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest()
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class ChartControllerTest extends AbstractDbSetupTest {

    private static final String ALL_HELM_CHARTS_URI = "/api/v1/charts";
    private static final String HELM_CHART_BY_NAME_URI = ALL_HELM_CHARTS_URI + "/test";
    private static final String GET_ALL_HELM_CHARTS_URL = "http://localhost/api/onboarded/charts";
    private static final String GET_INDIVIDUAL_HELM_CHART_URL = GET_ALL_HELM_CHARTS_URL + "/test";

    @MockBean
    private RestClient restClient;

    @Autowired
    private MockMvc mockMvc;

    private MvcResult mvcResult;

    @Autowired
    private HelmRegistryConfig helmRegistryConfig;

    @Test
    public void verifyAllHelmCharts() throws Exception {
        setUpJsonResponseForAllHelmCharts();
        mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(ALL_HELM_CHARTS_URI)).andExpect(status().is(200))
                .andReturn();
        checkResponseMessage(mvcResult, "eric-data-document-database-pg");
    }

    @Test
    public void verifyHelmChartByName() throws Exception {
        setUpJsonResponseForHelmChartByName();
        mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(HELM_CHART_BY_NAME_URI)).andExpect(status().is(200))
                .andReturn();
        checkResponseMessage(mvcResult, "eric-data-document-database-pg");
    }

    @Test
    public void verifyNonExistentHelmChart() throws Exception {
        setUpJsonResponseForNonExistentHelmChart();
        mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(HELM_CHART_BY_NAME_URI)).andExpect(status().is(404))
                .andReturn();
        checkResponseMessage(mvcResult, "404 Not Found");
    }

    private void setUpJsonResponseForAllHelmCharts() throws IOException {
        when(restClient.buildUrl(anyString(), anyString())).thenCallRealMethod();
        when(restClient.get(eq(GET_ALL_HELM_CHARTS_URL), eq(helmRegistryConfig.getBASIC_AUTH_USER()), eq(helmRegistryConfig.getBASIC_AUTH_PASS())))
                .thenReturn(new ResponseEntity<>(readDataFromFile("allChartDetails.json", StandardCharsets.UTF_8), HttpStatus.OK));
    }

    private void setUpJsonResponseForHelmChartByName() throws IOException {
        when(restClient.buildUrl(anyString(), anyString())).thenCallRealMethod();
        when(restClient.get(eq(GET_INDIVIDUAL_HELM_CHART_URL),
                            eq(helmRegistryConfig.getBASIC_AUTH_USER()),
                            eq(helmRegistryConfig.getBASIC_AUTH_PASS())))
                .thenReturn(new ResponseEntity<>(readDataFromFile("allChartDetails.json", StandardCharsets.UTF_8), HttpStatus.OK));
    }

    private void setUpJsonResponseForNonExistentHelmChart() {
        when(restClient.buildUrl(anyString(), anyString())).thenCallRealMethod();
        when(restClient.get(eq(GET_INDIVIDUAL_HELM_CHART_URL),
                            eq(helmRegistryConfig.getBASIC_AUTH_USER()),
                            eq(helmRegistryConfig.getBASIC_AUTH_PASS())))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "404 Not Found"));
    }
}
