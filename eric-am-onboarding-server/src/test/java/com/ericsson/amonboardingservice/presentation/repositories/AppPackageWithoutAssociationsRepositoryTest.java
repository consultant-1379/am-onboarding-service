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
package com.ericsson.amonboardingservice.presentation.repositories;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class AppPackageWithoutAssociationsRepositoryTest extends AbstractDbSetupTest {

    @Autowired
    private AppPackageWithoutAssociationsRepository appPackageWithoutAssociationsRepository;

    @Test
    public void testSelectPageShouldExcludeFields() {
        List<String> excludedFields =
                List.of("descriptorModel", "files", "helmfile", "vnfdZip", "chartsRegistryUrl",
                        "appPackageDockerImages", "userDefinedData", "serviceModelRecordEntity",
                        "operationDetails", "onboardingDetail", "appPackageArtifacts");
        Page<AppPackage> appPackagePage = appPackageWithoutAssociationsRepository
                .selectPageExcludeFields(AppPackage.class, excludedFields, Pageable.unpaged());

        assertThat(appPackagePage.getContent()).isNotEmpty();

        AppPackage appPackage = appPackagePage.getContent().get(0);
        assertThat(appPackage.getDescriptorModel()).isNullOrEmpty();
        assertThat(appPackage.getFiles()).isNullOrEmpty();
        assertThat(appPackage.getHelmfile()).isNullOrEmpty();
        assertThat(appPackage.getVnfdZip()).isNullOrEmpty();
        assertThat(appPackage.getChartsRegistryUrl()).isNullOrEmpty();
        assertThat(appPackage.getAppPackageDockerImages()).isNullOrEmpty();
        assertThat(appPackage.getUserDefinedData()).isNullOrEmpty();
        assertThat(appPackage.getServiceModelRecordEntity()).isNull();
        assertThat(appPackage.getOperationDetails()).isNullOrEmpty();
        assertThat(appPackage.getOnboardingDetail()).isNull();
        assertThat(appPackage.getAppPackageArtifacts()).isNullOrEmpty();
    }
}
