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
import com.ericsson.amonboardingservice.model.HelmPackage;
import com.ericsson.amonboardingservice.model.VnfPkgInfo;
import com.ericsson.amonboardingservice.utilities.RestUtils;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.util.List;

import static com.ericsson.amonboardingservice.api.DockerRegistryClient.getManifest;
import static com.ericsson.amonboardingservice.api.HelmChartsClient.ALL_HELM_CHARTS_URI;
import static com.ericsson.amonboardingservice.model.DockerImage.getDockerImagesFromVnfPkgInfo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public final class DeletePackageVerifier {

    private DeletePackageVerifier() {
    }

    public static void verifyDeletedDockerImages(List<DockerImage> images) {
        images.forEach(image -> {
            ResponseEntity<String> responseEntity = getManifest(image);
            assertThat(String.format("Deletion of onboarded package image %s from docker registry", image),
                    responseEntity.getStatusCode(), is(HttpStatus.NOT_FOUND));
        });
    }

    public static void verifyNotDeletedDockerImages(List<DockerImage> images) {
        images.forEach(image -> {
            ResponseEntity<String> responseEntity = getManifest(image);
            assertThat(String.format("Docker image %s was not found", image.toString()),
                    responseEntity.getStatusCode(), is(HttpStatus.OK));
        });
    }

    public static void verifySuccessfulPackageDeletionResponse(ResponseEntity<String> deleteResponse) {
        if (deleteResponse.getBody() != null) {
            LOGGER.info(deleteResponse.getBody());
        }
        assertThat("Deletion of onboarded package has failed.",
                deleteResponse.getStatusCodeValue(), is(204));
    }

    public static void verifyDeletedHelmCharts(VnfPkgInfo vnfPkgInfo) {
        List<HelmPackage> helmPackages = vnfPkgInfo.getHelmPackageUrls();
        if (helmPackages != null) {
            helmPackages.forEach(helmPackage -> verifyHelmChartDeletion(helmPackage.getChartUrl()));
        }
    }

    private static String getChartNameByChartUrl(String chartUrl) {
        String chartName = chartUrl.substring(chartUrl.lastIndexOf("/") + 1, chartUrl.lastIndexOf("-"));
        if (Character.isDigit(chartName.charAt(chartName.length() - 1))) {
            chartName = chartName.substring(0, chartName.lastIndexOf("-"));
        }
        return chartName;
    }

    private static void verifyHelmChartDeletion(final String chartUrl) {
        String chartName = getChartNameByChartUrl(chartUrl);
        final String chartRetrievalUrl = ALL_HELM_CHARTS_URI + File.separator + chartName;
        String chartVersion = chartUrl.substring(chartUrl.indexOf(chartName));
        verifyResponseForHelmChartDeleted(chartRetrievalUrl, chartVersion);
    }

    private static void verifyResponseForHelmChartDeleted(String url, String version) {
        ResponseEntity<String> getChartResponse = RestUtils.httpGetCall(url, String.class);
        if (getChartResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
            assertSame(getChartResponse.getStatusCode(), HttpStatus.NOT_FOUND, "HelmCharts are deleted");
        } else {
            LOGGER.info("Response body is: " + getChartResponse.getBody());
            JSONArray helmCharts = new JSONArray(getChartResponse.getBody());
            LOGGER.info("HelmCharts : {} ", helmCharts.toString());
            assertFalse(StringUtils.contains(helmCharts.toString(), "\"version\" : " + version),
                    "Helm chart versions to be different");
        }
    }

    @Step("Validate deletion of single CSAR")
    public static void verifySingleDeletedPackage(
            ResponseEntity<String> responseEntityDelete, VnfPkgInfo vnfPackageStateBeforeDeletion) {
        verifySuccessfulPackageDeletionResponse(responseEntityDelete);
        verifyDeletedDockerImages(getDockerImagesFromVnfPkgInfo(vnfPackageStateBeforeDeletion));
        verifyDeletedHelmCharts(vnfPackageStateBeforeDeletion);
    }

    @Step
    public static void verifyNotFoundDeletionOfPackageResponse(ResponseEntity<String> deleteResponse) {
        LOGGER.info(deleteResponse.getBody());
        assertThat("Expected error about not found package",
                deleteResponse.getStatusCodeValue(), is(404));
    }
}
