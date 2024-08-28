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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CREATED;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.presentation.services.helmservice.HelmChartStatus;
import com.ericsson.amonboardingservice.presentation.services.helmservice.HelmService;
import com.ericsson.amonboardingservice.infrastructure.client.RestClient;

@SpringBootTest(properties = "onboarding.upload.concurrency=none")
public class HelmServiceWithNoneConcurrencyTest extends AbstractDbSetupTest {

    private static final String TEST_URL = "localhost:8080/api/onboarded";

    @Autowired
    @InjectMocks
    private HelmService helmService;
    @MockBean
    private RestClient restClient;

    @BeforeEach
    public void setUp() {
        when(restClient.buildUrl(anyString(), anyString())).thenReturn(TEST_URL);
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
    public void testUploadExistentChart(@TempDir Path tempFolder) {
        Map<Path, HelmChartStatus> chart = new HashMap<>();
        chart.put(tempFolder.getRoot(), HelmChartStatus.PRESENT);

        when(restClient.postFile(anyString(), any(Path.class), anyString(), anyString())).thenReturn(new ResponseEntity<>("", CREATED));
        Map<Path, ResponseEntity<String>> response = helmService.uploadNewCharts(chart, LocalDateTime.now().plusSeconds(30));
        verify(restClient, times(0)).postFile(anyString(), any(Path.class), anyString(), anyString());
    }

    @Test
    public void testUploadMultipleCharts(@TempDir Path tempFolder) {
        Map<Path, HelmChartStatus> charts = new HashMap<>();
        charts.put(createChartPath(tempFolder, "1"), HelmChartStatus.NOT_PRESENT);
        charts.put(createChartPath(tempFolder, "2"), HelmChartStatus.NOT_PRESENT);

        when(restClient.postFile(anyString(), any(Path.class), anyString(), anyString())).thenReturn(new ResponseEntity<>("", CREATED));

        Map<Path, ResponseEntity<String>> response = helmService.uploadNewCharts(charts, LocalDateTime.now().plusSeconds(30));
        assertThat(List.copyOf(response.values()).get(0).getStatusCode()).isEqualTo(CREATED);
        verify(restClient, times(2)).postFile(anyString(), any(Path.class), anyString(), anyString());
    }

    private Path createChartPath(@TempDir Path tempFolder, String pathNameSuffix) {
        File file = new File(tempFolder.toFile(), pathNameSuffix);
        return file.toPath();
    }
}
