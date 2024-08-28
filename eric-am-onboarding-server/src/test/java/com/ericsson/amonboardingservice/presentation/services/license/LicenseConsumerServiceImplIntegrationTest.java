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
package com.ericsson.amonboardingservice.presentation.services.license;

import com.ericsson.amonboardingservice.presentation.models.license.Permission;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestPropertySource(properties = {
        "license.host=http://localhost:${stubrunner.runningstubs.eric-eo-lm-consumer-api.port}" })
@AutoConfigureStubRunner(ids = { "com.ericsson.orchestration.mgmt:eric-eo-lm-consumer-api" })
@SpringBootTest
@AutoConfigureObservability
public class LicenseConsumerServiceImplIntegrationTest {

    private static final String LICENSE_CONSUMER_PATH = "/lc/v1/cvnfm/permissions";
    private static final String INVALID_LICENSE_CONSUMER_PATH = "/lc/v1/test/permissions";

    private static final PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer(DockerImageName.parse(
                    "armdocker.rnd.ericsson.se/dockerhub-ericsson-remote/postgres")
            .asCompatibleSubstituteFor("postgres"));

    static {
        postgreSQLContainer.start();
        System.setProperty("DB_URL", postgreSQLContainer.getJdbcUrl());
        System.setProperty("DB_USERNAME", postgreSQLContainer.getUsername());
        System.setProperty("DB_PASSWORD", postgreSQLContainer.getPassword());
    }

    @Value("${license.host}")
    private String licenseUrlHost;

    @Autowired
    private LicenseConsumerServiceImpl licenseConsumerService;

    @Test
    public void getPermissionsWithoutRetryTestSuccess() {
        ReflectionTestUtils.setField(licenseConsumerService, "licenseServiceUrl",
                licenseUrlHost + LICENSE_CONSUMER_PATH);
        EnumSet<Permission> result = licenseConsumerService.fetchPermissions();
        assertEquals(EnumSet.allOf(Permission.class), result);
    }

    @Test
    public void getPermissionsWithoutRetryTestFailure() {
        ReflectionTestUtils.setField(licenseConsumerService, "licenseServiceUrl",
                licenseUrlHost + INVALID_LICENSE_CONSUMER_PATH);
        assertThrows(RestClientException.class, () -> licenseConsumerService.fetchPermissions());
    }
}
