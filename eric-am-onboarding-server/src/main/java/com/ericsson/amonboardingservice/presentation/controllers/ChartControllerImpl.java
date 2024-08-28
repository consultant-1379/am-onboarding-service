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
package com.ericsson.amonboardingservice.presentation.controllers;

import com.ericsson.amonboardingservice.api.ChartsApi;
import com.ericsson.amonboardingservice.presentation.services.helmservice.HelmService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1")
@Tag(name = "charts", description = "The charts API")
public class ChartControllerImpl implements ChartsApi {

    @Autowired
    private HelmService helmService;

    @Override
    public ResponseEntity<String> chartsGet() {
        return new ResponseEntity<>(helmService.listCharts(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> chartsChartNameGet(@PathVariable("chartName") String chartName) {
        return new ResponseEntity<>(helmService.listChartVersions(chartName), HttpStatus.OK);
    }
}
