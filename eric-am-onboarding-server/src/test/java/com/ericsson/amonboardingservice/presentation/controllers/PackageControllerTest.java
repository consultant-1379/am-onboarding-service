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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static com.ericsson.amonboardingservice.utils.Utility.checkResponseMessage;

import com.ericsson.amonboardingservice.model.ProblemDetails;
import com.ericsson.amonboardingservice.utils.UrlUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.model.AppPackageList;
import com.ericsson.amonboardingservice.model.AppPackageResponse;
import com.ericsson.amonboardingservice.presentation.exceptions.ErrorMessage;
import com.ericsson.amonboardingservice.presentation.services.supportedoperationservice.SupportedOperationService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class PackageControllerTest extends AbstractDbSetupTest {

    private static final String PACKAGE_CONTENT_URI = "/api/v1/packages";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SupportedOperationService supportedOperationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void checkGetNonExistentPackage() throws Exception {
        MvcResult mvcResult = mockMvc
                .perform(MockMvcRequestBuilders.get(PACKAGE_CONTENT_URI + "/1234"))
                .andExpect(status().is(404))
                .andReturn();
        checkResponseMessage(mvcResult, "Package with id: \\\"1234\\\" not found");
    }

    @Test
    public void testGetAllPackageWithValidFilter() throws Exception {
        String response = mockMvc
                .perform(MockMvcRequestBuilders.get(
                        PACKAGE_CONTENT_URI + "?filter="
                                + "(in,packages/appSoftwareVersion,1.20 (CXS101289_R81E08),1.21 (CXS101289_R81E08))"))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist(UrlUtils.PAGINATION_INFO))
                .andExpect(header().doesNotExist(HttpHeaders.LINK))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        AppPackageList appPackageList = objectMapper.readValue(response, AppPackageList.class);
        assertThat(appPackageList.getPackages())
                .isNotEmpty()
                .extracting(AppPackageResponse::getAppSoftwareVersion)
                .containsOnly("1.20 (CXS101289_R81E08)", "1.21 (CXS101289_R81E08)");
    }

    @Test
    public void testGetAllPackageWithInvalidFilter() throws Exception {
        String response = mockMvc
                .perform(MockMvcRequestBuilders.get(PACKAGE_CONTENT_URI + "?filter=(eq,appProductName)"))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        ErrorMessage errorMessage = objectMapper.readValue(response, ErrorMessage.class);
        assertThat(errorMessage.getMessage()).isEqualTo("Invalid filter value provided eq,appProductName");
    }

    @Test
    public void testGetAllPackageWithInvalidOperation() throws Exception {
        String response = mockMvc
                .perform(MockMvcRequestBuilders.get(
                        PACKAGE_CONTENT_URI + "?filter=(test,packages/appProductName,test)"))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        ErrorMessage errorMessage = objectMapper.readValue(response, ErrorMessage.class);
        assertThat(errorMessage.getMessage())
                .isEqualTo("Invalid operation provided test,packages/appProductName,test");
    }

    @Test
    public void testGetAllPackageWithAnOnboardingState() throws Exception {
        String response = mockMvc
                .perform(MockMvcRequestBuilders.get(
                        PACKAGE_CONTENT_URI + "?filter=(eq,packages/onboardingState,ONBOARDED)"))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist(UrlUtils.PAGINATION_INFO))
                .andExpect(header().doesNotExist(HttpHeaders.LINK))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        AppPackageList appPackageList = objectMapper.readValue(response, AppPackageList.class);
        assertThat(appPackageList.getPackages())
                .isNotEmpty()
                .extracting(AppPackageResponse::getOnboardingState)
                .containsOnly("ONBOARDED");
    }

    @Test
    public void testGetAllPackageWithAnOnboardingStateWithOperationNeq() throws Exception {
        String response = mockMvc
                .perform(MockMvcRequestBuilders.get(
                        PACKAGE_CONTENT_URI + "?filter=(neq,packages/onboardingState,ONBOARDED)"))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist(UrlUtils.PAGINATION_INFO))
                .andExpect(header().doesNotExist(HttpHeaders.LINK))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        AppPackageList appPackageList = objectMapper.readValue(response, AppPackageList.class);
        assertThat(appPackageList.getPackages())
                .isNotEmpty()
                .extracting(AppPackageResponse::getOnboardingState)
                .doesNotContain("ONBOARDED");
    }

    @Test
    public void testGetAllPackageWithAnOnboardingStateWithSomeInvalidValue() throws Exception {
        String response = mockMvc
                .perform(MockMvcRequestBuilders.get(
                        PACKAGE_CONTENT_URI + "?filter=(neq,packages/onboardingState,dfasdfa)"))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        ErrorMessage errorMessage = objectMapper.readValue(response, ErrorMessage.class);
        assertThat(errorMessage).isNotNull();
        assertThat(errorMessage.getMessage())
                .isEqualTo("Invalid value provided for type class " +
                        "com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage$OnboardingStateEnum, " +
                        "valid values are [CREATED, UPLOADING, PROCESSING, ONBOARDED, ERROR]");
    }

    @Test
    public void testGetAllPackageShouldReturnBadRequestWhenInvalidPageNumber() throws Exception {
        String response = mockMvc
                .perform(MockMvcRequestBuilders.get(
                        PACKAGE_CONTENT_URI + "?nextpage_opaque_marker=1000"))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        ProblemDetails problemDetails = objectMapper.readValue(response, ProblemDetails.class);
        assertThat(problemDetails).isNotNull();
        assertThat(problemDetails.getDetail())
                .startsWith("Requested page number exceeds the total number of pages. ");
    }

    @Test
    public void testGetAllPackageWithAnOnboardingStateWithMultipleValue() throws Exception {
        String response = mockMvc
                .perform(MockMvcRequestBuilders.get(
                        PACKAGE_CONTENT_URI + "?filter=(in,packages/onboardingState,ONBOARDED,CREATED)"))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist(UrlUtils.PAGINATION_INFO))
                .andExpect(header().doesNotExist(HttpHeaders.LINK))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        AppPackageList appPackageList = objectMapper.readValue(response, AppPackageList.class);
        assertThat(appPackageList.getPackages())
                .isNotEmpty()
                .extracting(AppPackageResponse::getOnboardingState)
                .containsOnly("ONBOARDED", "CREATED");
    }

    @Test
    public void testGetAllPackageWithAnOnboardingStateWithOperationNin() throws Exception {
        String response = mockMvc
                .perform(MockMvcRequestBuilders.get(
                        PACKAGE_CONTENT_URI + "?filter=(nin,packages/onboardingState,CREATED)"))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        AppPackageList appPackageList = objectMapper.readValue(response, AppPackageList.class);
        assertThat(appPackageList.getPackages())
                .isNotEmpty()
                .extracting(AppPackageResponse::getOnboardingState)
                .doesNotContain("ERROR");
    }

    @Test
    public void testGetAllPackageWithInvalidParameter() throws Exception {
        String response = mockMvc
                .perform(MockMvcRequestBuilders.get(PACKAGE_CONTENT_URI + "?filter=(eq,test,test)"))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        ErrorMessage errorMessage = objectMapper.readValue(response, ErrorMessage.class);
        assertThat(errorMessage.getMessage()).isEqualTo("Filter eq,test,test not supported");
    }

    @Test
    public void shouldAddPaginationHeadersWhenSizeSpecified() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(PACKAGE_CONTENT_URI + "?size=15"))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().exists(UrlUtils.PAGINATION_INFO))
                .andExpect(header().exists(HttpHeaders.LINK));
    }

    @Test
    public void shouldAddPaginationHeadersWhenOpaqueMarkerSpecified() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(PACKAGE_CONTENT_URI + "?nextpage_opaque_marker=1"))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().exists(UrlUtils.PAGINATION_INFO))
                .andExpect(header().exists(HttpHeaders.LINK));
    }

    @Test
    public void shouldReturnBadRequestWhenPageSizeIsGreaterThaMax() throws Exception {
        String content = mockMvc
                .perform(MockMvcRequestBuilders.get(PACKAGE_CONTENT_URI + "?size=200"))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andReturn().getResponse().getContentAsString();

        ProblemDetails problemDetails = objectMapper.readValue(content, ProblemDetails.class);
        assertThat(problemDetails).isNotNull();
        assertThat(problemDetails.getDetail())
                .isEqualTo("Total size of the results will be shown cannot be more than 100. Requested page size 200");
    }

    @Test
    public void shouldReturnBadRequestWhenPageNumberIsGreaterThanMax() throws Exception {
        String content = mockMvc
                .perform(MockMvcRequestBuilders.get(PACKAGE_CONTENT_URI + "?nextpage_opaque_marker=200"))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andReturn().getResponse().getContentAsString();

        ProblemDetails problemDetails = objectMapper.readValue(content, ProblemDetails.class);
        assertThat(problemDetails).isNotNull();
        assertThat(problemDetails.getDetail())
                .startsWith("Requested page number exceeds the total number of pages. Requested page: 200. Total pages:");
    }

}
