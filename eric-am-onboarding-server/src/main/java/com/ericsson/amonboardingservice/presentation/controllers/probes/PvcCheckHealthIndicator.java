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
package com.ericsson.amonboardingservice.presentation.controllers.probes;

import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import utils.probes.FsAccessMode;
import utils.probes.PvcHealthCheck;

@Component
public class PvcCheckHealthIndicator implements HealthIndicator {

    @Value("${onboarding.skipCertificateValidation}")
    private boolean isSkipCertificateValidation;

    @Value("${onboarding.root_ca_path}")
    private String rootCaPath;

    @Value("${healthCheckEnv.mountPaths.readOnly}")
    private String[] readOnlyMountPaths;

    @Value("${healthCheckEnv.mountPaths.readWrite}")
    private String[] readWriteMountPaths;

    private PvcHealthCheck pvcHealthCheck = new PvcHealthCheck();

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder().up();

        if (!isSkipCertificateValidation) {
            populateHealth(new String[]{rootCaPath}, FsAccessMode.READ_ONLY, builder);
        }

        populateHealth(readOnlyMountPaths, FsAccessMode.READ_ONLY, builder);
        populateHealth(readWriteMountPaths, FsAccessMode.READ_WRITE, builder);

        return builder.build();
    }

    private void populateHealth(String[] mountPaths, FsAccessMode mode, Health.Builder builder) {
        for (String mountPath: mountPaths) {
            boolean isHealthy = pvcHealthCheck.checkPvcHealth(Paths.get(mountPath), mode);
            if (isHealthy) {
                builder.withDetail(mountPath, String.format("%s: accessible", mode));
            } else {
                builder.down().withDetail(mountPath, String.format("%s: inaccessible", mode));
            }
        }
    }
}
