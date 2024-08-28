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
import com.ericsson.amonboardingservice.presentation.controllers.VnfPackageControllerImpl;
import com.ericsson.amonboardingservice.presentation.services.vnfdservice.VnfdService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import lombok.SneakyThrows;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@SpringBootTest(classes = ApplicationServer.class)
public class PositiveGetArtifactBase extends AbstractDbSetupTest {

    @Mock
    VnfdService vnfdService;

    @InjectMocks
    VnfPackageControllerImpl vnfPackageController;

    @BeforeEach
    public void setup() {
        final String valuesFile = "\n{ \"values\": { \"name\": \"chart_values\" }, \"resources\": { \"memory\": \"20Gi\", \"cpu\": \"400m\" }, \"limits\": { "
                + "\"memory\": \"30Gi\", \"cpu\": \"1000\" }}                \n";

        given(vnfdService.fetchArtifact(anyString(), anyString())).willAnswer(invocationOnMock ->
                                                                                      new byte[2]
        );

        given(vnfdService.fetchArtifact(anyString(), eq("Definitions/OtherTemplates/values_1.yaml"))).willAnswer(invocationOnMock ->
                                                                                                                         valuesFile.getBytes());

        given(vnfdService.fetchArtifact(anyString(), eq("Definitions/OtherTemplates/scaling_mapping.yamlplain"))).willAnswer(invocation ->
                                                                                                                                     returnMappingFile());
        RestAssuredMockMvc.standaloneSetup(vnfPackageController);
    }

    private byte[] returnMappingFile() throws URISyntaxException {
        byte[] encoded = new byte[0];
        try {
            encoded = Files.readAllBytes(Paths.get(Resources.getResource(
                    "contracts/api/vnf_packages/positive/getArtifact/scaling_mapping.yaml.properties").toURI()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return encoded;
    }
}
