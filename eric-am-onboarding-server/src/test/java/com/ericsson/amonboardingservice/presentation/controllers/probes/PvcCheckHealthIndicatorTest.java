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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@DirtiesContext
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, " +
        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
@ContextConfiguration(classes = { PvcCheckHealthIndicator.class })
public class PvcCheckHealthIndicatorTest {

    @Value("${onboarding.root_ca_path}")
    private String rootCaPath;

    @Value("${healthCheckEnv.mountPaths.readOnly}")
    private String[] readOnlyMountPaths;

    @Value("${healthCheckEnv.mountPaths.readWrite}")
    private String[] readWriteMountPaths;

    @Autowired
    private PvcCheckHealthIndicator pvcCheckHealthIndicator;

    @Test
    public void shouldPerformCheckOnEachConfiguredPath() {
        Health healthStatus = pvcCheckHealthIndicator.health();
        int mountsCount = readOnlyMountPaths.length + readWriteMountPaths.length + 1; // 1 stands for root ca path
        assertEquals(mountsCount, healthStatus.getDetails().size());
        assertTrue(healthStatus.getDetails().containsKey(rootCaPath));
        assertTrue(healthStatus.getDetails().keySet().containsAll(List.of(readOnlyMountPaths)));
        assertTrue(healthStatus.getDetails().keySet().containsAll(List.of(readWriteMountPaths)));
    }

    @Nested
    @TestPropertySource(properties = { "onboarding.skipCertificateValidation=true" })
    public class ChangedPropertiesSubtest {

        @Value("${onboarding.root_ca_path}")
        private String rootCaPath;

        @Value("${healthCheckEnv.mountPaths.readOnly}")
        private String[] readOnlyMountPaths;

        @Value("${healthCheckEnv.mountPaths.readWrite}")
        private String[] readWriteMountPaths;

        @Autowired
        private PvcCheckHealthIndicator pvcCheckHealthIndicator;

        @Test
        public void shouldSkipCheckOnSkipValidationFlagIsTrue() {
            Health healthStatus = pvcCheckHealthIndicator.health();
            int mountsCount = readOnlyMountPaths.length + readWriteMountPaths.length;

            assertEquals(mountsCount, healthStatus.getDetails().size());
            assertFalse(healthStatus.getDetails().containsKey(rootCaPath));
            assertTrue(healthStatus.getDetails().keySet().containsAll(List.of(readOnlyMountPaths)));
            assertTrue(healthStatus.getDetails().keySet().containsAll(List.of(readWriteMountPaths)));
        }
    }
}