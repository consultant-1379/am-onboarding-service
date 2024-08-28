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
package com.ericsson.amonboardingservice.presentation.services.onboarding.request;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage.AppUsageStateEnum.NOT_IN_USE;
import static com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage.OnboardingStateEnum.CREATED;
import static com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage.OperationalStateEnum.DISABLED;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_IMAGES;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.ericsson.amonboardingservice.presentation.models.vnfd.AppUserDefinedData;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageDatabaseService;
import com.ericsson.amonboardingservice.utils.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ericsson.am.shared.vnfd.model.VnfDescriptorDetails;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.services.dockerservice.DockerService;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { OnboardImages.class })
@TestPropertySource(
        properties = {
                "spring.flyway.enabled = false",
                "container.registry.enabled = false"
        })
public class OnboardImagesTest {

    private static final String TEST_PACKAGE_ID = "test_packageID";

    private AppPackage appPackage = new AppPackage();

    private PackageUploadRequestContext context;

    @Autowired
    private OnboardImages onboardImages;

    @MockBean
    private PackageDatabaseService databaseService;

    @MockBean
    private DockerService dockerService;

    @MockBean
    private Persist nextHandler;

    @BeforeEach
    public void setUp() throws Exception {
        File file = new File(".");
        context = new PackageUploadRequestContext("testCsar.csar", file.toPath(), LocalDateTime.now(), TEST_PACKAGE_ID);
        context.setVnfd(new VnfDescriptorDetails());
        HashMap<String, Path> artifactPaths = new HashMap<>();
        artifactPaths.put(ENTRY_IMAGES, file.toPath());
        context.setArtifactPaths(artifactPaths);
        given(databaseService.getAppPackageById(any())).willReturn(appPackage);
        given(dockerService.onboardDockerTar(any(), any())).willReturn(Collections.emptyList());
    }

    @Test
    public void handle() {
        AppPackage expectedPackage = expectedPackage();
        onboardImages.handle(context);
        verify(dockerService, times(0)).onboardDockerTar(any(), any());
        verify(databaseService).save(eq(expectedPackage));
    }

    private AppPackage expectedPackage() {
        appPackage.setMultipleVnfd(false).setOnboardingState(CREATED).setOperationalState(DISABLED)
                .setPackageId(TEST_PACKAGE_ID).setUsageState(NOT_IN_USE);
        AppUserDefinedData userDefinedData = new AppUserDefinedData().setKey(Constants.SKIP_IMAGE_UPLOAD).setValue("true").setAppPackages(appPackage);
        return appPackage.setUserDefinedData(List.of(userDefinedData));
    }
}