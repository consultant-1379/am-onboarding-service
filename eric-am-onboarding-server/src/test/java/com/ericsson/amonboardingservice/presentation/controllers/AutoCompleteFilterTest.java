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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.model.AutoCompleteResponse;
import com.ericsson.amonboardingservice.infrastructure.client.RestClient;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class AutoCompleteFilterTest extends AbstractDbSetupTest {

    private static final String AUTO_COMPLETE_URL = "/api/v1/packages/filter/autocomplete";
    private static final String AUTO_COMPLETE_URL_WITH_TYPE = AUTO_COMPLETE_URL + "?type=";

    @Autowired
    private MockMvc mockMvc;

    @Mock
    RestClient restClient;

    private MvcResult mvcResult;

    @Test
    public void getAllValues() {
        try {
            mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(AUTO_COMPLETE_URL)).andExpect(status().is(200))
                    .andReturn();
            AutoCompleteResponse response = new ObjectMapper()
                    .readValue(mvcResult.getResponse().getContentAsString(), AutoCompleteResponse.class);
            assertThat(response).isNotNull();
            assertThat(response.getPackageVersion()).isNotNull().isNotEmpty();
            assertThat(response.getProvider()).isNotNull().isNotEmpty();
            assertThat(response.getSoftwareVersion()).isNotNull().isNotEmpty();
            assertThat(response.getType()).isNotNull().isNotEmpty();
        } catch (Exception ex) {
            fail("This should fail");
        }
    }

    @Test
    public void getAllValuesWithTypeParameter() {
        try {
            mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(AUTO_COMPLETE_URL_WITH_TYPE))
                    .andExpect(status().is(200)).andReturn();
            AutoCompleteResponse response = new ObjectMapper()
                    .readValue(mvcResult.getResponse().getContentAsString(), AutoCompleteResponse.class);
            assertThat(response).isNotNull();
            assertThat(response.getPackageVersion()).isNotNull().isEmpty();
            assertThat(response.getProvider()).isNotNull().isEmpty();
            assertThat(response.getSoftwareVersion()).isNotNull().isEmpty();
            assertThat(response.getType()).isNotNull().isNotEmpty();

        } catch (Exception ex) {
            fail("This should fail");
        }
    }

    @Test
    public void getAllValuesWithTypeParameterValue() {
        try {
            mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(AUTO_COMPLETE_URL_WITH_TYPE + "SG"))
                    .andExpect(status().is(200)).andReturn();
            AutoCompleteResponse response = new ObjectMapper()
                    .readValue(mvcResult.getResponse().getContentAsString(), AutoCompleteResponse.class);
            assertThat(response).isNotNull();
            assertThat(response.getPackageVersion()).isNotNull().isEmpty();
            assertThat(response.getProvider()).isNotNull().isEmpty();
            assertThat(response.getSoftwareVersion()).isNotNull().isEmpty();
            List<String> allType = response.getType();
            assertThat(allType).isNotNull().isNotEmpty();
            for (String type : allType) {
                assertThat(type).contains("SG");
            }

        } catch (Exception ex) {
            fail("This should fail");
        }
    }

}
