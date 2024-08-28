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
package com.ericsson.amonboardingservice.presentation.services.vnfpackageservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.ericsson.amonboardingservice.model.VnfPackageArtifactInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.model.VnfPkgInfo;
import com.ericsson.amonboardingservice.model.VnfPkgInfoV2;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;

@SpringBootTest()
@ActiveProfiles("test")
public class VnfPackageServiceTest extends AbstractDbSetupTest {

    @Autowired
    private VnfPackageService vnfPackageService;
    @Autowired
    private AppPackageRepository appPackageRepository;

    private final String PACKAGE_ID_WITH_ERROR = "c2def1ce-4cf4-477c-aab3-21cb04e6a380";

    @Test
    @Transactional
    public void testListVnfPackages() {
        List<VnfPkgInfo> infos = vnfPackageService.listVnfPackages(Pageable.unpaged()).getContent();
        checkValues(pkgIdsToCheck(), infos);
    }

    @Test
    @Transactional
    public void testListVnfPackagesV2() {
        List<VnfPkgInfoV2> infos = vnfPackageService.listVnfPackagesV2(Pageable.unpaged()).getContent();
        checkValuesV2(pkgIdsToCheck(), infos);
    }

    @Test
    @Transactional
    public void testGetVnfPackage() {
        Optional<VnfPkgInfo> info = vnfPackageService.getVnfPackage("b3def1ce-4cf4-477c-aab3-21cb04e6a380");
        assertThat(info.isPresent()).isTrue();
        assertThat(info.get().getId()).isEqualTo("b3def1ce-4cf4-477c-aab3-21cb04e6a380");
        assertThat(info.get().getSoftwareImages().size()).isEqualTo(3);
    }

    @Test
    @Transactional
    public void testGetVnfPackageV2() {
        Optional<VnfPkgInfoV2> info = vnfPackageService.getVnfPackageV2(PACKAGE_ID_WITH_ERROR);
        assertThat(info.isPresent()).isTrue();
        assertThat(info.get().getId()).isEqualTo(PACKAGE_ID_WITH_ERROR);
        assertThat(info.get().getOnboardingState()).isEqualTo(VnfPkgInfoV2.OnboardingStateEnum.ERROR);
        assertThat(info.get().getOnboardingFailureDetails()).isNotNull();
    }

    @Test
    @Transactional
    public void testGetVnfPackageV2WithArtifacts() {
        String id = "f3def1ce-4cf4-477c-123";
        Optional<VnfPkgInfoV2> info = vnfPackageService.getVnfPackageV2(id);
        assertThat(info.isPresent()).isTrue();
        assertThat(info.get().getId()).isEqualTo(id);
        assertThat(info.get().getAdditionalArtifacts()).isNotNull();
        Optional<VnfPackageArtifactInfo> vnfPackageArtifactInfo = info.get().getAdditionalArtifacts().stream().findFirst();
        assertThat(vnfPackageArtifactInfo.isPresent()).isTrue();
        assertThat(vnfPackageArtifactInfo.get().getArtifactPath())
                .isEqualTo("Definitions/OtherTemplates/sample-vnf-0.1.2.tgz");
        assertThat(vnfPackageArtifactInfo.get().getMetadata()).isNull();
        assertThat(vnfPackageArtifactInfo.get().getChecksum()).isNull();
        assertThat(info.get().getOnboardingState()).isEqualTo(VnfPkgInfoV2.OnboardingStateEnum.ONBOARDED);
        assertThat(info.get().getOnboardingFailureDetails()).isNull();
    }

    @Test
    @Transactional
    public void testGetVnfPackageV2WithOutArtifacts() {
        String id = "57c24601-e70d-418d-8477-64fb58a7563c";
        Optional<VnfPkgInfoV2> info = vnfPackageService.getVnfPackageV2(id);
        assertThat(info.isPresent()).isTrue();
        assertThat(info.get().getId()).isEqualTo(id);
        assertThat(info.get().getAdditionalArtifacts()).isNotNull();
        Optional<VnfPackageArtifactInfo> vnfPackageArtifactInfo = info.get().getAdditionalArtifacts().stream().findFirst();
        assertThat(vnfPackageArtifactInfo.isPresent()).isFalse();
        assertThat(info.get().getOnboardingState()).isEqualTo(VnfPkgInfoV2.OnboardingStateEnum.ONBOARDED);
        assertThat(info.get().getOnboardingFailureDetails()).isNull();
    }

    @Test
    public void testGetVnfPackageForNonExistentPackage() {
        Optional<VnfPkgInfo> info = vnfPackageService.getVnfPackage("id-doesnt-exist");
        assertThat(info).isEqualTo(Optional.empty());
    }

    @Test
    @Transactional
    public void getAllPackageWithAValidFilterOnProductName() {
        Page<VnfPkgInfo> vnfPkgInfoPage = vnfPackageService
                .listVnfPackages("(eq,vnfProductName,SAPC)", Pageable.unpaged());
        assertThat(vnfPkgInfoPage.getContent())
                .isNotEmpty()
                .extracting(VnfPkgInfo::getVnfProductName)
                .containsOnly("SAPC");

        Page<VnfPkgInfo> allSGSNMMEPackagesPage = vnfPackageService
                .listVnfPackages("(eq,vnfProductName,SGSN-MME)", Pageable.unpaged());
        assertThat(allSGSNMMEPackagesPage.getContent())
                .isNotEmpty()
                .extracting(VnfPkgInfo::getVnfProductName)
                .containsOnly("SGSN-MME");
    }

    @Test
    public void testGetAllPackagesWithProductNameNotPresent() {
        Page<VnfPkgInfo> vnfPkgInfoPage = vnfPackageService
                .listVnfPackages("(eq,vnfProductName,test)", Pageable.unpaged());

        assertThat(vnfPkgInfoPage.isEmpty()).isTrue();
    }

    @Test
    public void testGetAllPackagesWithInvalidFilter() {
        assertThatThrownBy(() -> vnfPackageService
                .listVnfPackages("(eq,vnfProductName)", Pageable.unpaged()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid filter value provided eq,vnfProductName");

    }

    @Test
    public void testGetAllPackagesWithInvalidOperation() {
        assertThatThrownBy(() -> vnfPackageService
                .listVnfPackages("(test,vnfProductName,test)", Pageable.unpaged()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid operation provided test,vnfProductName,test");
    }

    @Test
    public void testGetAllPackagesWithInvalidParameter() {
        assertThatThrownBy(() -> vnfPackageService
                .listVnfPackages("(eq,test,test)", Pageable.unpaged()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Filter eq,test,test not supported");
    }

    @Test
    @Transactional
    public void testGetAllPackagesWithMultipleFilters() {
        Page<VnfPkgInfo> vnfPkgInfoPage = vnfPackageService.listVnfPackages(
                "(eq,vnfProductName,SGSN-MME);(eq,vnfSoftwareVersion,1.20 (CXS101289_R81E08))",
                Pageable.unpaged()
        );

        assertThat(vnfPkgInfoPage.getContent())
                .isNotEmpty()
                .extracting(VnfPkgInfo::getVnfProductName, VnfPkgInfo::getVnfSoftwareVersion)
                .containsOnly(tuple("SGSN-MME", "1.20 (CXS101289_R81E08)"));
    }

    @Test
    @Transactional
    public void testGetAllPackagesWithMultipleSoftwareVersion() {
        Page<VnfPkgInfo> vnfPkgInfoPage = vnfPackageService.listVnfPackages(
                "(in,vnfSoftwareVersion,1.20 (CXS101289_R81E08),1.21 (CXS101289_R81E08))",
                Pageable.unpaged()
        );

        assertThat(vnfPkgInfoPage.getContent())
                .isNotEmpty()
                .extracting(VnfPkgInfo::getVnfSoftwareVersion)
                .containsOnly("1.20 (CXS101289_R81E08)", "1.21 (CXS101289_R81E08)");
    }

    @Test
    @Transactional
    public void testGetPackageWithSecurityOption() {
        AppPackage appPackage = getAppPackage(AppPackage.PackageSecurityOption.OPTION_2);
        String packageId = appPackageRepository.save(appPackage).getPackageId();

        Optional<VnfPkgInfo> vnfPkgInfo = vnfPackageService.getVnfPackage(packageId);
        assertThat(vnfPkgInfo).isPresent();
        assertThat(vnfPkgInfo.get().getPackageSecurityOption())
                .isEqualTo(VnfPkgInfo.PackageSecurityOptionEnum.OPTION_2);
        appPackageRepository.deleteByPackageId(appPackage.getPackageId());
    }

    @Test
    @Transactional
    public void testGetAllPackagesWithSecurityOption() {
        AppPackage appPackage1 = getAppPackage(AppPackage.PackageSecurityOption.OPTION_2);
        AppPackage appPackage2 = getAppPackage(AppPackage.PackageSecurityOption.OPTION_2);
        String packageId1 = appPackageRepository.save(appPackage1).getPackageId();
        String packageId2 = appPackageRepository.save(appPackage2).getPackageId();

        Page<VnfPkgInfo> vnfPkgInfoPage = vnfPackageService.listVnfPackages(Pageable.unpaged());

        assertThat(vnfPkgInfoPage.getContent())
                .isNotEmpty()
                .extracting(VnfPkgInfo::getId, VnfPkgInfo::getPackageSecurityOption)
                .contains(
                        tuple(packageId1, VnfPkgInfo.PackageSecurityOptionEnum.OPTION_2),
                        tuple(packageId2, VnfPkgInfo.PackageSecurityOptionEnum.OPTION_2)
                );

        appPackageRepository.deleteByPackageId(packageId1);
        appPackageRepository.deleteByPackageId(packageId2);
    }

    @Test
    @Transactional
    public void testGetFilteredPackagesWithSecurityOption1() {
        AppPackage appPackage1 = getAppPackage(AppPackage.PackageSecurityOption.OPTION_1);
        AppPackage appPackage2 = getAppPackage(AppPackage.PackageSecurityOption.OPTION_2);
        String packageId1 = appPackageRepository.save(appPackage1).getPackageId();
        String packageId2 = appPackageRepository.save(appPackage2).getPackageId();

        Page<VnfPkgInfo> vnfPkgInfoPage = vnfPackageService
                .listVnfPackages("(eq,packageSecurityOption,OPTION_1)", Pageable.unpaged());

        assertThat(vnfPkgInfoPage.getContent())
                .isNotEmpty()
                .extracting(VnfPkgInfo::getId, VnfPkgInfo::getPackageSecurityOption)
                .contains(tuple(packageId1, VnfPkgInfo.PackageSecurityOptionEnum.OPTION_1))
                .doesNotContain(tuple(packageId2, VnfPkgInfo.PackageSecurityOptionEnum.OPTION_2));

        appPackageRepository.deleteByPackageId(packageId1);
        appPackageRepository.deleteByPackageId(packageId2);
    }

    @Test
    @Transactional
    public void testListVnfPackagesShouldNotReturnIsSetForDeletionPackages() {
        AppPackage initialPackage = new AppPackage();
        initialPackage.setForDeletion(true);
        AppPackage saved = appPackageRepository.save(initialPackage);

        Page<VnfPkgInfo> vnfPkgInfoPage = vnfPackageService.listVnfPackages(Pageable.unpaged());

        assertThat(vnfPkgInfoPage.getContent())
                .isNotEmpty()
                .extracting(VnfPkgInfo::getId)
                .doesNotContain(saved.getPackageId());

        appPackageRepository.delete(saved);
    }

    private AppPackage getAppPackage(AppPackage.PackageSecurityOption securityOption) {
        AppPackage appPackage = new AppPackage();
        appPackage.setPackageSecurityOption(securityOption);
        appPackage.setOnboardingState(AppPackage.OnboardingStateEnum.ONBOARDED);
        return appPackage;
    }

    public void checkValues(List<String> ids, List<VnfPkgInfo> packages) {
        for (String id : ids) {
            boolean containsVnf = packages.stream().anyMatch(item -> id.equals(item.getId()));
            assertThat(containsVnf).isTrue();
        }
        assertThat(ids.contains(PACKAGE_ID_WITH_ERROR)).isFalse();
    }

    public void checkValuesV2(List<String> ids, List<VnfPkgInfoV2> packages) {
        for (String id : ids) {
            boolean containsVnf = packages.stream().anyMatch(item -> id.equals(item.getId()));
            assertThat(containsVnf).isTrue();
        }
        Optional<VnfPkgInfoV2> vnfPkgInfoV2 = packages.stream()
                .filter(item -> PACKAGE_ID_WITH_ERROR.equals(item.getId()))
                .findFirst();
        assertThat(vnfPkgInfoV2).isPresent();
        assertThat(vnfPkgInfoV2.get().getOnboardingState()).isEqualTo(VnfPkgInfoV2.OnboardingStateEnum.ERROR);
        assertThat(vnfPkgInfoV2.get().getOnboardingFailureDetails()).isNotNull();
    }

    public List<String> pkgIdsToCheck() {
        List<String> ids = new ArrayList<>();
        ids.add("b3def1ce-4cf4-477c-aab3-21cb04e6a380");
        ids.add("e3def1ce-4cf4-477c-aab3-21cb04e6a382");
        ids.add("e3def1ce-4cf4-477c-aab3-21cb04e6a384");
        return ids;
    }


}


