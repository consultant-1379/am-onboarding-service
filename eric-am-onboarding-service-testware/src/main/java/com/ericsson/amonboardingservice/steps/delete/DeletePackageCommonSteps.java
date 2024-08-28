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
package com.ericsson.amonboardingservice.steps.delete;

import com.ericsson.amonboardingservice.model.DockerImage;
import com.ericsson.amonboardingservice.model.VnfPkgInfo;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static com.ericsson.amonboardingservice.api.VnfPackagesClient.deleteVnfPackage;
import static com.ericsson.amonboardingservice.steps.delete.DeletePackageVerifier.verifyDeletedDockerImages;
import static com.ericsson.amonboardingservice.steps.delete.DeletePackageVerifier.verifyDeletedHelmCharts;
import static com.ericsson.amonboardingservice.steps.delete.DeletePackageVerifier.verifyNotDeletedDockerImages;
import static com.ericsson.amonboardingservice.steps.delete.DeletePackageVerifier.verifySingleDeletedPackage;
import static com.ericsson.amonboardingservice.steps.delete.DeletePackageVerifier.verifySuccessfulPackageDeletionResponse;

@Slf4j
public final class DeletePackageCommonSteps {
    private DeletePackageCommonSteps() {
    }

    @Step("Delete onboarded CSAR package")
    public static ResponseEntity<String> deletePackage(VnfPkgInfo pkgInfo) {
        return deleteVnfPackage(pkgInfo);
    }

    @Step("Delete onboarded CSARs where are common docker images and verify")
    public static void deleteAndVerifyPackagesWithCommonImages(VnfPkgInfo vnfPkgOne, VnfPkgInfo vnfPkgTwo) {
        LOGGER.info("Current VnfPackageIds are {}, {}", vnfPkgOne.getId(), vnfPkgTwo.getId());
        List<DockerImage> packageOneImages = DockerImage.getDockerImagesFromVnfPkgInfo(vnfPkgOne);
        List<DockerImage> packageTwoImages = DockerImage.getDockerImagesFromVnfPkgInfo(vnfPkgTwo);
        List<DockerImage> commonImages = DockerImage.getCommonImages(packageOneImages, packageTwoImages);
        List<DockerImage> packageOneImagesWithoutCommon = DockerImage.removeCommonImages(packageOneImages, commonImages);

        ResponseEntity<String> deleteResponseOne = deleteVnfPackage(vnfPkgOne);
        verifySuccessfulPackageDeletionResponse(deleteResponseOne);
        verifyDeletedHelmCharts(vnfPkgOne);
        verifyNotDeletedDockerImages(commonImages);
        verifyDeletedDockerImages(packageOneImagesWithoutCommon);

        ResponseEntity<String> deleteResponseTwo = deleteVnfPackage(vnfPkgTwo);

        verifySingleDeletedPackage(deleteResponseTwo, vnfPkgTwo);
    }
}
