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

import static java.nio.file.Files.newInputStream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static com.ericsson.amonboardingservice.TestUtils.getResource;
import static com.ericsson.amonboardingservice.utils.Utility.checkResponseMessage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.TestUtils;
import com.ericsson.amonboardingservice.presentation.models.OnboardingResponseLinks;
import com.ericsson.amonboardingservice.presentation.services.ToscaHelper;
import com.ericsson.amonboardingservice.presentation.services.helmservice.HelmService;
import com.ericsson.amonboardingservice.utils.Constants;
import com.ericsson.amonboardingservice.infrastructure.client.RestClient;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class InternalPackageControllerTest extends AbstractDbSetupTest {

    private static final String FILE = "file";
    private static final String VALID_VNFD_YAML = "valid_vnfd.yaml";
    private static final String PACKAGE_CONTENT_URI = "/api/v1/internal/packages";
    private static final String HELM_URL = "helmChartURI";
    private static final String HELM_URL_VALUE =
            "http://eric-lcm-helm-chart-registry.process-engine-4-eric-am-onboarding-service:8080/onboarded/charts/sampledescriptor-0.0.1-223.tgz";
    private static final String HElM_CHART_FILE = "sampledescriptor-0.0.1-223.tgz";
    private static final String HELM_CHART_FILE_NAME = "sampledescriptor-0.0.1-223";
    private static final String HELM_CHART_FILE_EXTENSION = ".tgz";
    private static final String PACKAGE_ID = "c3def1ce-4cf4-477c-aab3-21cb04e6a379";

    @MockBean
    RestClient restClient;
    @MockBean
    HelmService helmService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;
    @MockBean
    private ToscaHelper toscaHelper;


    @Test
    public void testV1PackagesPostFailsBecause1Dot3ToscaVnfdNotSupported() throws Exception {
        when(toscaHelper.isTosca1Dot2(any())).thenReturn(false);
        when(helmService.getChart(HELM_URL_VALUE, HElM_CHART_FILE)).thenReturn(Optional.of(createTempChartFile()));
        MockMultipartFile mockMultipartFile = new MockMultipartFile(FILE, createInputStream(VALID_VNFD_YAML));
        MvcResult mvcResult = getMvcResultWithExpectedStatus(mockMultipartFile, 400);

        assertThat(mvcResult.getResponse().getContentAsString())
            .isEqualTo("Failed to upload vnfd file because 1.3 TOSCA VNFD is not supported");
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    @Transactional
    public void uploadVnfPackage() throws Exception {
        when(toscaHelper.isTosca1Dot2(any())).thenReturn(true);
        when(helmService.getChart(HELM_URL_VALUE, HElM_CHART_FILE)).thenReturn(Optional.of(createTempChartFile()));

        MockMultipartFile mockMultipartFile = new MockMultipartFile(FILE, createInputStream(VALID_VNFD_YAML));
        MvcResult mvcResult = getMvcResultWithExpectedStatus(mockMultipartFile, 201);
        checkResponseMessage(mvcResult, "vnfd file has been successfully onboarded");
        checkResponseMessage(mvcResult, PACKAGE_ID);

        // Put the database back into the state it was before, depending on execution order other tests were failing.
        when(helmService.getChart(HELM_URL_VALUE, HElM_CHART_FILE)).thenReturn(Optional.empty());

        JSONObject response = new JSONObject(mvcResult.getResponse().getContentAsString());
        OnboardingResponseLinks links = mapper.readValue(response.get("_links").toString(), OnboardingResponseLinks.class);
        String id = links.getSelf().getHref().split("/")[9];
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/vnfpkgm/v1/vnf_packages/" + id)
                                .header(Constants.IDEMPOTENCY_KEY_HEADER, UUID.randomUUID()))
                .andExpect(status().is(204)).andReturn();
    }

    private static File createTempChartFile() throws IOException, URISyntaxException {
        File tempPackageFile = File.createTempFile(HELM_CHART_FILE_NAME, HELM_CHART_FILE_EXTENSION);
        Files.copy(TestUtils.getResource(HElM_CHART_FILE), tempPackageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return tempPackageFile;
    }

    private static InputStream createInputStream(String fileName) throws URISyntaxException, IOException {
        return newInputStream(getResource(fileName));
    }

    private MvcResult getMvcResultWithExpectedStatus(MockMultipartFile file, int httpStatus) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.multipart(PACKAGE_CONTENT_URI).file(file).param(HELM_URL, HELM_URL_VALUE)
                        .contentType(MediaType.MULTIPART_FORM_DATA)).andExpect(status().is(httpStatus)).andReturn();
    }
}
