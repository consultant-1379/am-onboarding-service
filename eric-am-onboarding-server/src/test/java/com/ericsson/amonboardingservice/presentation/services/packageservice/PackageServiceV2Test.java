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
package com.ericsson.amonboardingservice.presentation.services.packageservice;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.model.AppPackageResponseV2;
import com.ericsson.amonboardingservice.presentation.models.ChartUrlsEntity;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ericsson.amonboardingservice.presentation.models.ChartUrlsEntity.ChartTypeEnum.CNF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
        properties = {
                "onboarding.skipToscaoValidation = true",
                "url="
        })
public class PackageServiceV2Test extends AbstractDbSetupTest {

    @Autowired
    private PackageServiceV2 packageService;

    @Autowired
    private AppPackageRepository appPackageRepository;

    @Test
    public void testGetPackageV2() {
        AppPackage initialPackage = getAppPackage();
        var actualPackage = appPackageRepository.save(initialPackage);

        Optional<AppPackageResponseV2> responsePackageOptional = packageService.getPackageV2(actualPackage.getPackageId());

        assertThat(responsePackageOptional.isPresent()).isTrue();

        AppPackageResponseV2 responsePackage = responsePackageOptional.get();

        assertThat(responsePackage.getOnboardingState()).isEqualTo(actualPackage.getOnboardingState().toString());
        assertThat(responsePackage.getOperationalState().toString())
                .isEqualTo(actualPackage.getOperationalState().toString());
        assertThat(responsePackage.getUsageState().toString())
                .isEqualTo(actualPackage.getUsageState().toString());
        assertThat(responsePackage.getPackageSecurityOption().toString())
                .isEqualTo(actualPackage.getPackageSecurityOption().toString());
        assertThat(responsePackage.getAppDescriptorId()).isEqualTo(actualPackage.getDescriptorId());
        assertThat(responsePackage.getDescriptorVersion()).isEqualTo(actualPackage.getDescriptorVersion());
        assertThat(responsePackage.getDescriptorModel()).isNotNull();
        assertThat(responsePackage.getAppProductName()).isEqualTo(actualPackage.getProductName());
        assertThat(responsePackage.getAppProvider()).isEqualTo(actualPackage.getProvider());
        assertThat(responsePackage.getAppSoftwareVersion()).isEqualTo(actualPackage.getSoftwareVersion());
        assertThat(responsePackage.getAppPkgId()).isEqualTo(actualPackage.getPackageId());
        assertThat(responsePackage.getHelmPackageUrls().size())
                .isEqualTo(actualPackage.getChartsRegistryUrl().size());
    }

    @Test
    void testGetPackagesV2WithUIVerbosityAndFilter() {
        Page<AppPackageResponseV2> appPackagePage =
                packageService.listPackagesV2("(eq,packages/appDescriptorId,1)", PackageResponseVerbosity.UI, Pageable.unpaged());

        assertThat(appPackagePage.get()).hasSize(1);

        AppPackageResponseV2 appPackage = appPackagePage.getContent().get(0);

        assertThat(appPackage.getAppPkgId()).isNotEmpty();
        assertThat(appPackage.getAppProvider()).isNotEmpty();
        assertThat(appPackage.getAppDescriptorId()).isNotEmpty();
        assertThat(appPackage.getAppSoftwareVersion()).isNotEmpty();
        assertThat(appPackage.getImagesURL()).isNullOrEmpty();
        assertThat(appPackage.getDescriptorModel()).isNull();
        assertThat(appPackage.getHelmPackageUrls()).isNullOrEmpty();
        assertThat(appPackage.getImagesURL()).isNullOrEmpty();
    }

    @Test
    void testGetPackagesV2WithUIVerbosityAndInvalidFilter() {
        assertThatThrownBy(() -> packageService
                .listPackagesV2("(eq,packages/appDescriptorIdInvalid,1)", PackageResponseVerbosity.UI, Pageable.unpaged()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testListPackagesV2ShouldNotReturnPackagesSetForDeletion() {
        AppPackage initialPackage = getAppPackage(true);
        var actualPackage = appPackageRepository.save(initialPackage);
        assertThat(actualPackage.isSetForDeletion()).isTrue();

        Page<AppPackageResponseV2> appPackagePage = packageService.listPackagesV2(Pageable.unpaged());
        assertThat(appPackagePage.get())
                .isNotEmpty()
                .extracting(AppPackageResponseV2::getAppPkgId)
                .doesNotContain(actualPackage.getPackageId());
    }

    @Test
    public void testListPackagesV2ShouldReturnCorrectPageData() {
        Pageable pageable = Pageable.ofSize(2);
        Page<AppPackageResponseV2> appPackagePage = packageService.listPackagesV2(pageable);

        assertPage(appPackagePage, pageable);
    }

    @Test
    public void testListPackagesV2WithUIVerbosityShouldReturnCorrectPageData() {
        Pageable pageable = Pageable.ofSize(1);
        Page<AppPackageResponseV2> appPackagePage = packageService.listPackagesV2(PackageResponseVerbosity.UI, pageable);

        assertPage(appPackagePage, pageable);
    }

    @Test
    public void testListPackagesV2WithFilterShouldReturnCorrectPageData() {
        Pageable pageable = Pageable.ofSize(1);
        Page<AppPackageResponseV2> appPackagePage =
                packageService.listPackagesV2("(neq,packages/appDescriptorId,1)", pageable);

        assertPage(appPackagePage, pageable);
    }

    private static void assertPage(Page<?> page, Pageable pageable) {
        assertThat(page.getContent()).hasSize(pageable.getPageSize());
        assertThat(page.getTotalElements()).isGreaterThan(pageable.getPageSize());
        assertThat(page.getTotalPages()).isGreaterThan(pageable.getPageSize());
        assertThat(page.getNumber()).isEqualTo(pageable.getPageNumber());
    }

    private static AppPackage getAppPackage() {
        return getAppPackage(false);
    }

    private static AppPackage getAppPackage(boolean isSetForDeletion) {
        AppPackage appPackage = new AppPackage();
        appPackage.setDescriptorId(UUID.randomUUID().toString());
        appPackage.setDescriptorVersion("1.0.44");
        appPackage.setDescriptorModel("{}");
        appPackage.setProductName("spider-app");
        appPackage.setProvider("Ericsson");
        appPackage.setSoftwareVersion("1.0.44");
        appPackage.setPackageSecurityOption(AppPackage.PackageSecurityOption.UNSIGNED);
        appPackage.setOnboardingState(AppPackage.OnboardingStateEnum.CREATED);
        appPackage.setOperationalState(AppPackage.OperationalStateEnum.ENABLED);
        appPackage.setUsageState(AppPackage.AppUsageStateEnum.IN_USE);
        appPackage.setForDeletion(isSetForDeletion);
        ChartUrlsEntity chartUrlsEntity = getChartUrlsEntity(appPackage);
        List<ChartUrlsEntity> chartUrlsEntities = new ArrayList<>();
        chartUrlsEntities.add(chartUrlsEntity);
        appPackage.setChartsRegistryUrl(chartUrlsEntities);
        return appPackage;
    }

    private static ChartUrlsEntity getChartUrlsEntity(final AppPackage appPackage) {
        ChartUrlsEntity chartUrlsEntity = new ChartUrlsEntity();
        chartUrlsEntity.setPriority(1);
        chartUrlsEntity.setChartsRegistryUrl(
                "http://eric-lcm-helm-chart-registry.process-engine-4-eric-am-onboarding-service:8080/onboarded/charts/sampledescriptor-0.0.1-223"
                        + ".tgz");
        chartUrlsEntity.setChartName("sampledescriptor");
        chartUrlsEntity.setChartVersion("0.0.1-223");
        chartUrlsEntity.setAppPackage(appPackage);
        chartUrlsEntity.setChartType(CNF);
        chartUrlsEntity.setChartArtifactKey("helm_package");
        return chartUrlsEntity;
    }
}


