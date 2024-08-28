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
package com.ericsson.amonboardingservice.presentation.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import static com.ericsson.amonboardingservice.TestUtils.getResource;
import static com.ericsson.amonboardingservice.TestUtils.readDataFromFile;
import static com.ericsson.amonboardingservice.utils.JsonUtils.EMPTY_JSON_MSG;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.infrastructure.client.RestClient;
import com.ericsson.amonboardingservice.presentation.exceptions.ChartOnboardingException;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.services.helmservice.HelmChartStatus;
import com.ericsson.amonboardingservice.presentation.services.helmservice.HelmService;
import com.ericsson.amonboardingservice.presentation.services.manifestservice.ManifestService;
import com.ericsson.amonboardingservice.presentation.services.onboarding.request.OnboardCharts;
import com.ericsson.amonboardingservice.presentation.services.packageservice.CleanUpOnFailureService;

@SpringBootTest()
@ActiveProfiles("test")
public class HelmServiceTest extends AbstractDbSetupTest {

    private static final String TEST_URL = "localhost:8080/api/onboarded";
    private static final String TEST_CHART = "testChart";
    private static final String EMPTY_JSON = "{}";
    private static final String ERIC_POSTGRES = "eric-data-document-database-pg";
    private static final String TEST_POSTGRES = "test-postgres";
    private static final String CHART_NOT_FOUND_JSON = "{\"message\":\"404 Not found\"}";
    private static final List<Path> helmCharts = new ArrayList<>();
    private static Path helmChart;

    @Autowired
    private HelmService helmService;

    @SpyBean
    private OnboardCharts onboardCharts;

    @MockBean
    private RestClient restClient;

    @MockBean
    private ManifestService manifestService;

    @MockBean
    private AppPackageRepository appPackageRepository;

    @MockBean
    private CleanUpOnFailureService failureService;

    @BeforeAll
    public static void fileSetup() throws URISyntaxException, IOException {
        Path sampleDescriptorHelmChart = getResource("sampledescriptor-0.0.1-223.tgz");
        Path chart = Files.createTempFile("helmChart", ".tgz");
        Files.copy(sampleDescriptorHelmChart, chart, StandardCopyOption.REPLACE_EXISTING);
        helmChart = chart;
        helmCharts.add(chart);
    }

    @BeforeEach
    public void setUp() {
        when(restClient.buildUrl(anyString(), anyString())).thenReturn(TEST_URL);
    }

    @Test
    public void testGetAllChartDetails() throws IOException, JSONException {
        when(restClient.get(anyString(), anyString(), anyString()))
                .thenReturn(new ResponseEntity<>(readDataFromFile("allChartDetails.json", StandardCharsets.UTF_8), HttpStatus.OK));
        String response = helmService.listCharts();
        JSONObject jsonObject = new JSONObject(response);
        assertThat(jsonObject.get(ERIC_POSTGRES)).isNotNull();
        assertThat(jsonObject.get(TEST_POSTGRES)).isNotNull();
        assertThat(jsonObject.getJSONArray(TEST_POSTGRES).length()).isEqualTo(2);
    }

    @Test
    public void testGetChartsByName() throws IOException, JSONException {
        when(restClient.get(anyString(), anyString(), anyString()))
                .thenReturn(new ResponseEntity<>(
                        readDataFromFile("chartDetailsByName.json", StandardCharsets.UTF_8), HttpStatus.OK));
        String response = helmService.listChartVersions(TEST_CHART);
        JSONArray jsonArray = new JSONArray(response);
        assertThat(jsonArray.getJSONObject(0)).isNotNull();
        assertThat(jsonArray.getJSONObject(0).get("name")).isEqualTo(ERIC_POSTGRES);
    }

    @Test
    public void testGetChartDetailsEmptyJson() {
        when(restClient.get(anyString(), anyString(), anyString()))
                .thenReturn(new ResponseEntity<>(EMPTY_JSON, HttpStatus.OK));
        String response = helmService.listCharts();
        assertThat(response).isEqualTo("{ }");
    }

    @Test
    public void testGetChartDetailsNullResponse() {
        when(restClient.get(anyString(), anyString(), anyString())).thenThrow(new RestClientException("Rest client exception"));
        Assertions.assertThrows(RestClientException.class, () -> {
                String response = helmService.listCharts();
                assertThat(response).isEqualTo(EMPTY_JSON_MSG);
            }
        );
    }

    @Test
    public void testNonExistentChart() {
        when(restClient.get(anyString(), anyString(), anyString()))
                .thenReturn(new ResponseEntity<>(CHART_NOT_FOUND_JSON, HttpStatus.NOT_FOUND));
        String response = helmService.listChartVersions(TEST_CHART);
        assertThat(response).contains("404 Not found");
    }

    @Test
    public void testGetChartInstallUrl() {
        assertThat(helmService.getChartInstallUrl("sampledescriptor-0.0.1-223.tgz"))
                .matches("localhost/onboarded/charts/sampledescriptor-0.0.1-223.tgz");
    }

    @Test
    public void testUploadNewChart(@TempDir Path tempFolder) {
        Map<Path, HelmChartStatus> chart = new HashMap<>();
        chart.put(tempFolder.getRoot(), HelmChartStatus.NOT_PRESENT);

        when(restClient.postFile(anyString(), any(Path.class), anyString(), anyString())).thenReturn(new ResponseEntity<>("", CREATED));
        Map<Path, ResponseEntity<String>> response = helmService.uploadNewCharts(chart, LocalDateTime.now().plusSeconds(30));
        assertThat(List.copyOf(response.values()).get(0).getStatusCode()).isEqualTo(CREATED);
        verify(restClient, times(1)).postFile(anyString(), any(Path.class), anyString(), anyString());
    }

    @Test
    public void testUploadMultipleCharts(@TempDir Path tempFolder) {
        Map<Path, HelmChartStatus> chart = new HashMap<>();
        chart.put(createChartPath(tempFolder, "1"), HelmChartStatus.NOT_PRESENT);
        chart.put(createChartPath(tempFolder, "2"), HelmChartStatus.NOT_PRESENT);

        when(restClient.postFile(anyString(), any(Path.class), anyString(), anyString())).thenReturn(new ResponseEntity<>("", CREATED));
        Map<Path, ResponseEntity<String>> response = helmService.uploadNewCharts(chart, LocalDateTime.now().plusSeconds(30));
        assertThat(List.copyOf(response.values()).get(0).getStatusCode()).isEqualTo(CREATED);
        verify(restClient, times(2)).postFile(anyString(), any(Path.class), anyString(), anyString());
    }

    @Test
    public void testUploadMultipleChartsWithSomeAlreadyPresentInRegistry(@TempDir Path tempFolder) {
        Map<Path, HelmChartStatus> helmChartStatus = mockConflictedChartsAndResponse(tempFolder, CREATED);

        Map<Path, ResponseEntity<String>> response = helmService.uploadNewCharts(helmChartStatus, LocalDateTime.now().plusSeconds(30));
        onboardCharts.handleHelmUploadResponse(response, "some-package-id", helmChartStatus);

        verify(restClient, times(3)).postFile(anyString(), any(Path.class), anyString(), anyString());
        verify(restClient, times(2)).getFile(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void testUploadMultipleChartsWithBothPresentInRegistryAndFailed(@TempDir Path tempFolder) {
        Map<Path, HelmChartStatus> helmChartStatus = mockConflictedChartsAndResponse(tempFolder, INTERNAL_SERVER_ERROR);

        when(appPackageRepository.findByPackageId(anyString())).thenReturn(Optional.of(new AppPackage()));
        when(appPackageRepository.save(any(AppPackage.class))).thenReturn(new AppPackage());
        doNothing().when(failureService).cleanupChartInFailure(eq(helmChartStatus));

        Map<Path, ResponseEntity<String>> response = helmService.uploadNewCharts(helmChartStatus, LocalDateTime.now().plusSeconds(30));

        ChartOnboardingException exception = assertThrows(ChartOnboardingException.class, ()
                -> onboardCharts.handleHelmUploadResponse(response, "some-package-id", helmChartStatus));

        verify(restClient, times(3)).postFile(anyString(), any(Path.class), anyString(), anyString());
        verify(restClient, times(2)).getFile(anyString(), anyString(), anyString(), anyString());
        assertThat(exception.getMessage()).isEqualTo(INTERNAL_SERVER_ERROR.getReasonPhrase());
    }

    @Test
    public void testUploadNewChartsRetriesWhenIOErrorOccurs(@TempDir Path tempFolder) {
        Map<Path, HelmChartStatus> charts = Map.of(tempFolder.getRoot(), HelmChartStatus.NOT_PRESENT);

        when(restClient.postFile(anyString(), any(Path.class), anyString(), anyString()))
                .thenThrow(new ResourceAccessException("I/O error happened"))
                .thenReturn(new ResponseEntity<>("", CREATED));

        Map<Path, ResponseEntity<String>> response = helmService.uploadNewCharts(charts, LocalDateTime.now().plusSeconds(30));

        assertThat(response.values()).extracting(ResponseEntity::getStatusCode).containsOnly(CREATED);
        verify(restClient, times(2)).postFile(anyString(), any(Path.class), anyString(), anyString());
    }

    @Test
    public void testUploadNewChartsReturnsInternalServerErrorWhenIOErrorOccursAndTimeoutExpired(@TempDir Path tempFolder) {
        Map<Path, HelmChartStatus> charts = Map.of(tempFolder.getRoot(), HelmChartStatus.NOT_PRESENT);

        when(restClient.postFile(anyString(), any(Path.class), anyString(), anyString()))
                .thenThrow(new ResourceAccessException("I/O error happened"))
                .thenReturn(new ResponseEntity<>("", CREATED));

        Map<Path, ResponseEntity<String>> response = helmService.uploadNewCharts(charts, LocalDateTime.now().plusSeconds(2));

        assertThat(response.values()).extracting(ResponseEntity::getStatusCode).containsOnly(INTERNAL_SERVER_ERROR);
        verify(restClient, times(1)).postFile(anyString(), any(Path.class), anyString(), anyString());
    }

    @Test
    public void testUploadExistingChart() {
        when(restClient.getFile(anyString(), anyString(), anyString(), anyString())).thenReturn(Optional.of(helmChart.toFile()));
        when(manifestService.compareArtifacts(any(Path.class), any(Path.class))).thenReturn(true);
        HelmChartStatus status = helmService.checkChartPresent(helmChart);
        assertThat(status).isEqualTo(HelmChartStatus.PRESENT);
        verify(restClient, times(1)).getFile(anyString(), anyString(), anyString(), anyString());
        verify(restClient, times(0)).postFile(anyString(), any(Path.class), anyString(), anyString());
    }

    @Test
    public void testUploadExistingChartWhichDoesntMatch() {
        when(restClient.getFile(anyString(), anyString(), anyString(), anyString())).thenReturn(Optional.of(helmChart.toFile()));
        when(manifestService.compareArtifacts(any(Path.class), any(Path.class))).thenReturn(false);
        HelmChartStatus status = helmService.checkChartPresent(helmChart);
        assertThat(status).isEqualTo(HelmChartStatus.PRESENT_BUT_CONTENT_NOT_MATCHING);
    }

    @Test
    public void testUploadExistingMultipleCharts() {
        when(restClient.getFile(anyString(), anyString(), anyString(), anyString())).thenReturn(Optional.of(helmChart.toFile()));
        when(manifestService.compareArtifacts(any(Path.class), any(Path.class))).thenReturn(true);
        Map<Path, HelmChartStatus> chartStatusMap = helmService.checkChartPresent(helmCharts);
        assertThat(chartStatusMap.get(helmChart)).isEqualTo(HelmChartStatus.PRESENT);
        verify(restClient, times(1)).getFile(anyString(), anyString(), anyString(), anyString());
        verify(restClient, times(0)).postFile(anyString(), any(Path.class), anyString(), anyString());
    }

    @Test
    public void testUploadExistingMultipleChartWhichDoesntMatch() {
        when(restClient.getFile(anyString(), anyString(), anyString(), anyString())).thenReturn(Optional.of(helmChart.toFile()));
        when(manifestService.compareArtifacts(any(Path.class), any(Path.class))).thenReturn(false);
        Map<Path, HelmChartStatus> chartStatusMap = helmService.checkChartPresent(helmCharts);
        assertThat(chartStatusMap.get(helmChart)).isEqualTo(HelmChartStatus.PRESENT_BUT_CONTENT_NOT_MATCHING);
    }

    private Path createChartPath(@TempDir Path tempFolder, String pathNameSuffix) {
        File file = new File(tempFolder.toFile(), pathNameSuffix);
        return file.toPath();
    }

    private Map<Path, HelmChartStatus> mockConflictedChartsAndResponse(Path tempFolder, HttpStatus nonExistentChartStatus) {
        Path nonExistentInRegistryChartPath = tempFolder.resolve("non-existent-chart.tgz");
        Path firstExistentInRegistryChartPath = tempFolder.resolve("existent-chart-1.tgz");
        Path secondExistentInRegistryChartPath = tempFolder.resolve("existent-chart-2.tgz");

        Map<Path, HelmChartStatus> helmChartStatus = new HashMap<>();
        helmChartStatus.put(nonExistentInRegistryChartPath, HelmChartStatus.NOT_PRESENT);
        helmChartStatus.put(firstExistentInRegistryChartPath, HelmChartStatus.NOT_PRESENT);
        helmChartStatus.put(secondExistentInRegistryChartPath, HelmChartStatus.NOT_PRESENT);

        when(restClient.postFile(anyString(), eq(nonExistentInRegistryChartPath), anyString(), anyString()))
                .thenReturn(new ResponseEntity<>(nonExistentChartStatus.getReasonPhrase(), nonExistentChartStatus));
        when(restClient.postFile(anyString(), eq(firstExistentInRegistryChartPath), anyString(), anyString()))
                .thenReturn(new ResponseEntity<>(CONFLICT.getReasonPhrase(), CONFLICT));
        when(restClient.postFile(anyString(), eq(secondExistentInRegistryChartPath), anyString(), anyString()))
                .thenReturn(new ResponseEntity<>(CONFLICT.getReasonPhrase(), CONFLICT));
        when(restClient.getFile(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(firstExistentInRegistryChartPath.toFile()));
        when(manifestService.compareArtifacts(any(Path.class), any(Path.class))).thenReturn(true);

        return helmChartStatus;
    }
}
