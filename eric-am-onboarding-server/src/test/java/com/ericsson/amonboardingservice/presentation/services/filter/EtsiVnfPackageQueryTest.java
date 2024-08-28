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
package com.ericsson.amonboardingservice.presentation.services.filter;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest()
@ActiveProfiles("test")
public class EtsiVnfPackageQueryTest extends AbstractDbSetupTest {

    @Autowired
    private EtsiVnfPackageQuery packageQuery;

    @Test
    @Transactional
    public void testQueryWithUserDefinedDataWithSupportedFilter() {
        Page<AppPackage> appPackagePage = packageQuery.getPageWithFilter(
                "(neq,onboardingState,ONBOARDED);(eq,userDefinedData/overrideRegistryURL,true)",
                Pageable.unpaged()
        );

        assertTrue(appPackagePage.hasContent());

        for (AppPackage appPackage : appPackagePage) {
            assertThat(appPackage.getOnboardingState()).isNotEqualTo(AppPackage.OnboardingStateEnum.ONBOARDED);
            assertThat(appPackage.getUserDefinedData()).isNotEmpty();
            assertThat(appPackage.getUserDefinedData())
                    .anyMatch(data -> data.getKey().equals("overrideRegistryURL") && data.getValue().equals("true"));
        }
    }

    @Test
    @Transactional
    public void testQueryWithOutUSerDefinedData() {
        Page<AppPackage> appPackagePage = packageQuery.getPageWithFilter(
                "(neq,onboardingState,ONBOARDED)", Pageable.unpaged());

        assertTrue(appPackagePage.hasContent());

        for (AppPackage appPackage : appPackagePage.getContent()) {
            assertThat(appPackage.getOnboardingState()).isNotEqualTo(AppPackage.OnboardingStateEnum.ONBOARDED);
        }
    }

    @Test
    @Transactional
    public void testQueryWithUserDefinedDataWithDifferentUSerDefinedData() {
        Page<AppPackage> appPackagePage = packageQuery.getPageWithFilter(
                "(neq,onboardingState,ONBOARDED);"
                        + "(eq,userDefinedData/overrideRegistryURL,true);(eq,userDefinedData/timeOut,40)",
                Pageable.unpaged()
        );

        assertTrue(appPackagePage.hasContent());

        for (AppPackage appPackage : appPackagePage) {
            assertThat(appPackage.getOnboardingState()).isNotEqualTo(AppPackage.OnboardingStateEnum.ONBOARDED);
            assertThat(appPackage.getUserDefinedData()).isNotEmpty();
            assertThat(appPackage.getUserDefinedData())
                    .anyMatch(data -> data.getKey().equals("overrideRegistryURL") && data.getValue().equals("true"));
        }
    }

    @Test
    @Transactional
    public void testQueryWithUserDefinedDataWithDifferentUSerDefinedDataAndGreaterOperator() {
        Page<AppPackage> appPackagePage = packageQuery.getPageWithFilter(
                "(neq,onboardingState,ONBOARDED);"
                        + "(eq,userDefinedData/overrideRegistryURL,true);(gte,userDefinedData/timeOut,20)",
                Pageable.unpaged()
        );

        assertTrue(appPackagePage.hasContent());

        for (AppPackage appPackage : appPackagePage) {
            assertThat(appPackage.getOnboardingState()).isNotEqualTo(AppPackage.OnboardingStateEnum.ONBOARDED);
            assertThat(appPackage.getUserDefinedData()).isNotEmpty();
            assertThat(appPackage.getUserDefinedData())
                    .anyMatch(data -> data.getKey().equals("timeOut"));
        }
    }

    @Test
    @Transactional
    public void testQueryWithUserDefinedDataWithDifferentUSerDefinedDataAndLessThanOperator() {
        Page<AppPackage> appPackagePage = packageQuery.getPageWithFilter(
                "(neq,onboardingState,ONBOARDED);(lt,userDefinedData/timeOut,31)",
                Pageable.unpaged()
        );

        assertTrue(appPackagePage.hasContent());

        for (AppPackage appPackage : appPackagePage) {
            assertThat(appPackage.getOnboardingState()).isNotEqualTo(AppPackage.OnboardingStateEnum.ONBOARDED);
            assertThat(appPackage.getUserDefinedData()).isNotEmpty();
            assertThat(appPackage.getUserDefinedData())
                    .anyMatch(data -> data.getKey().equals("timeOut"));
        }
    }

    @Test
    @Transactional
    public void testQueryWithUserDefinedDataWithInvalidUnsupportedFilter() {
        String filter = "(neq,onboardingState,ONBOARDED);(lt,userDefinedData/timeOut,31);(eq,test,test)";

        assertThatThrownBy(() -> packageQuery.getPageWithFilter(filter, Pageable.unpaged()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(String.format(EtsiVnfPackageQuery.FILTER_KEY_NOT_SUPPORTED_ERROR_MESSAGE, "test"));
    }

    @Test
    @Transactional
    public void testQueryWithInvalidUnsupportedFilter() {
        String filter = "(eq,test,test)";

        assertThatThrownBy(() -> packageQuery.getPageWithFilter(filter, Pageable.unpaged()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Filter eq,test,test not supported");
    }

    @Test
    @Transactional
    public void testQueryWithOnlyUserDefinedData() {
        Page<AppPackage> appPackagePage = packageQuery.getPageWithFilter(
                "(eq,userDefinedData/overrideRegistryURL,true)",
                Pageable.unpaged()
        );

        assertTrue(appPackagePage.hasContent());

        for (AppPackage appPackage : appPackagePage) {
            assertThat(appPackage.getUserDefinedData()).isNotEmpty();
            assertThat(appPackage.getUserDefinedData())
                    .anyMatch(data -> data.getKey().equals("overrideRegistryURL") && data.getValue().equals("true"));
        }
    }

    @Test
    @Transactional
    public void testQueryWithOnlyUserDefinedDataValueNotPresentAndSupportedFilter() {
        Page<AppPackage> appPackagePage = packageQuery.getPageWithFilter(
                "(neq,onboardingState,ONBOARDED);(eq,userDefinedData/test,true)",
                Pageable.unpaged()
        );
        assertTrue(appPackagePage.isEmpty());
    }

    @Test
    @Transactional
    public void testQueryWithOnlyUserDefinedDataAndSupportedFilterValueNotPresnt() {
        Page<AppPackage> appPackagePage = packageQuery.getPageWithFilter(
                "(eq,vnfdId,ONBOARDED);(eq,userDefinedData/overrideRegistryURL,true)",
                Pageable.unpaged()
        );

        assertTrue(appPackagePage.isEmpty());
    }

    @Test
    @Transactional
    public void testQueryWithUserDefinedDataWithDifferentUserDefinedDataAndInOperator() {
        Page<AppPackage> appPackagePage = packageQuery.getPageWithFilter(
                "(neq,onboardingState,ONBOARDED);(in,userDefinedData/timeOut,20,30)",
                Pageable.unpaged()
        );

        assertTrue(appPackagePage.hasContent());

        for (AppPackage appPackage : appPackagePage) {
            assertThat(appPackage.getOnboardingState()).isNotEqualTo(AppPackage.OnboardingStateEnum.ONBOARDED);
            assertThat(appPackage.getUserDefinedData()).isNotEmpty();
            assertThat(appPackage.getUserDefinedData())
                    .anyMatch(data -> data.getKey().equals("timeOut"));
        }
    }

    @Test
    @Transactional
    public void testQueryWithUserDefinedDataWithDifferentUserDefinedDataAndInAndNotInOperator() {
        Page<AppPackage> appPackagePage = packageQuery.getPageWithFilter(
                "(neq,onboardingState,ONBOARDED);"
                        + "(in,userDefinedData/timeOut,20,30);(nin,userDefinedData/overrideRegistryURL,true)",
                Pageable.unpaged()
        );

        assertTrue(appPackagePage.hasContent());

        for (AppPackage appPackage : appPackagePage) {
            assertThat(appPackage.getOnboardingState()).isNotEqualTo(AppPackage.OnboardingStateEnum.ONBOARDED);
            assertThat(appPackage.getUserDefinedData()).isNotEmpty();
            assertThat(appPackage.getUserDefinedData())
                    .anyMatch(data -> data.getKey().equals("timeOut"));
        }
    }

    @Test
    @Transactional
    public void testQueryShouldNotReturnIsSetForDeletionPackages() {
        Page<AppPackage> appPackagePage = packageQuery.getPageWithFilter(
                "(neq,onboardingState,ONBOARDED)",
                Pageable.unpaged()
        );

        assertThat(appPackagePage.getContent())
                .isNotEmpty()
                .extracting(AppPackage::isSetForDeletion)
                .doesNotContain(true);
    }
}
