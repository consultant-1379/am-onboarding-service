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
package com.ericsson.amonboardingservice;

import com.ericsson.amonboardingservice.presentation.models.license.Permission;
import com.ericsson.amonboardingservice.presentation.services.license.LicenseConsumerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.EnumSet;

import static org.mockito.BDDMockito.given;

@AutoConfigureObservability
@TestPropertySource(properties = { "spring.cloud.kubernetes.config.enabled= false" })
public abstract class AbstractDbSetupTest {

    @MockBean
    protected LicenseConsumerServiceImpl licenseConsumerService;

    @BeforeEach
    public void initMocks() {
        given(licenseConsumerService.getPermissions()).willReturn(EnumSet.allOf(Permission.class));
    }

    public static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer(DockerImageName.parse(
            "armdocker.rnd.ericsson.se/dockerhub-ericsson-remote/postgres")
            .asCompatibleSubstituteFor("postgres"));

    static {
        postgreSQLContainer.start();
        System.setProperty("DB_URL", postgreSQLContainer.getJdbcUrl());
        System.setProperty("DB_USERNAME", postgreSQLContainer.getUsername());
        System.setProperty("DB_PASSWORD", postgreSQLContainer.getPassword());
    }
}

