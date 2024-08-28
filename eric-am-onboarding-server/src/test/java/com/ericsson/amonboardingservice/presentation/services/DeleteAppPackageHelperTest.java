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

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage.OperationalStateEnum.DISABLED;
import static com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage.OperationalStateEnum.ENABLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@SpringBootTest
public class DeleteAppPackageHelperTest extends AbstractDbSetupTest {
    @Autowired
    private AppPackageRepository appPackageRepository;

    @Autowired
    private DeleteAppPackageHelper helper;

    @Test
    public void testAppPackageSetForDeletionSuccessful() {
        AppPackage appPackage = new AppPackage();
        appPackage.setForDeletion(false);
        appPackage.setOperationalState(ENABLED);
        AppPackage appPackageBeforeMethod = appPackageRepository.save(appPackage);

        assertThat(appPackageBeforeMethod.isSetForDeletion()).isFalse();
        assertThat(appPackageBeforeMethod.getOperationalState()).isEqualTo(ENABLED);

        String packageId = appPackageBeforeMethod.getPackageId();

        assertThat(appPackageRepository.findByPackageId(packageId)).isNotNull();

        helper.setAndSaveAppPackageForDeletion(packageId);

        var appPackageAfterMethod = appPackageRepository.findByPackageId(packageId);

        assertThat(appPackageAfterMethod.get().isSetForDeletion()).isTrue();
        assertThat(appPackageAfterMethod.get().getOperationalState()).isEqualTo(DISABLED);
    }

    @Test
    public void testAppPackageSetForDeletionWhenAppPackageDoesNotExist() {
        String nonexistentAppPackageId = "non-existent-id";
        Optional<AppPackage> appPackageBeforeMethod = appPackageRepository.findByPackageId(nonexistentAppPackageId);

        // assert package does not exist in db
        assertThat(appPackageBeforeMethod).isEmpty();

        assertThatNoException().isThrownBy(() -> helper.setAndSaveAppPackageForDeletion(nonexistentAppPackageId));
    }
}