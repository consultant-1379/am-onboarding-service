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
import com.ericsson.amonboardingservice.model.AppPackageResponse;
import com.ericsson.amonboardingservice.presentation.controllers.VnfPackageControllerImpl;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageService;
import com.ericsson.amonboardingservice.presentation.services.vnfdservice.VnfdService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static com.ericsson.amonboardingservice.utils.JsonUtils.readJsonFromResource;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(SpringExtension.class)
public class PositiveGetVnfdBase extends AbstractDbSetupTest {

    @Mock
    private PackageService packageService;
    @Mock
    private VnfdService vnfdService;
    @InjectMocks
    private VnfPackageControllerImpl vnfPackageController;

    @BeforeEach
    public void setup() {
        given(packageService.getPackage(anyString())).willAnswer(invocationOnMock -> {
            String responseTemplate = readJsonFromResource("AppPackageResponse.json");
            ObjectMapper mapper = new ObjectMapper();
            return Optional.of(mapper.readValue(responseTemplate, AppPackageResponse.class));
        });
        given(vnfdService.createVnfdYamlFileByPackageId(anyString())).will(invocation ->
                "no-scaling".equals(invocation.getArgument(0)) ? returnVNFD() : returnScalingVNFD()
        );
        RestAssuredMockMvc.standaloneSetup(vnfPackageController);
    }

    private Path returnVNFD() throws URISyntaxException {
        return Paths.get(Resources.getResource("contracts/api/vnf_packages/positive/getVnfd/sample_vnfd.yaml.properties").toURI());
    }

    private Path returnScalingVNFD() throws URISyntaxException {
        return Paths.get(Resources.getResource("contracts/api/vnf_packages/positive/getVnfd/sample_vnfd_with_scaling.yaml.properties").toURI());
    }
}
