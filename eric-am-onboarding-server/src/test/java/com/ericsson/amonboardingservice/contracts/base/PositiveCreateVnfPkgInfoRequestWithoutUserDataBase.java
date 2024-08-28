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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.ApplicationServer;
import com.ericsson.amonboardingservice.model.VnfPkgInfo;
import com.ericsson.amonboardingservice.model.VnfPkgInfoLink;
import com.ericsson.amonboardingservice.model.VnfPkgInfoLinks;
import com.ericsson.amonboardingservice.presentation.controllers.VnfPackageControllerImpl;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageService;
import com.ericsson.amonboardingservice.presentation.services.idempotency.IdempotencyServiceImpl;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles("dev")
@SpringBootTest(classes = ApplicationServer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class PositiveCreateVnfPkgInfoRequestWithoutUserDataBase extends AbstractDbSetupTest {

    @Mock
    private PackageService packageService;

    @Mock
    protected IdempotencyServiceImpl idempotencyService;

    @InjectMocks
    private VnfPackageControllerImpl vnfPackageController;

    @BeforeEach
    public void setup() {
        given(packageService.createVnfPackage(any())).willReturn(getVnfPkgInfo());
        given(idempotencyService.executeTransactionalIdempotentCall(any())).willCallRealMethod();
        RestAssuredMockMvc.standaloneSetup(vnfPackageController);
    }

    private static VnfPkgInfo getVnfPkgInfo() {
        VnfPkgInfo vnfPkgInfo = new VnfPkgInfo();
        vnfPkgInfo.setId("97d82f0d-4dab-4c97-a2d7-b882868f37ac");
        vnfPkgInfo.setUserDefinedData("");
        vnfPkgInfo.setOperationalState(VnfPkgInfo.OperationalStateEnum.DISABLED);
        vnfPkgInfo.setOnboardingState(VnfPkgInfo.OnboardingStateEnum.CREATED);
        vnfPkgInfo.setUsageState(VnfPkgInfo.UsageStateEnum.NOT_IN_USE);

        VnfPkgInfoLinks vnfPkgInfoLinks = new VnfPkgInfoLinks().self(
                new VnfPkgInfoLink().href(
                        "http://localhost/api/vnfpkgm/v1/vnf_packages/97d82f0d-4dab-4c97-a2d7-b882868f37ac"
                )
        );
        vnfPkgInfo.setLinks(vnfPkgInfoLinks);

        return vnfPkgInfo;
    }
}
