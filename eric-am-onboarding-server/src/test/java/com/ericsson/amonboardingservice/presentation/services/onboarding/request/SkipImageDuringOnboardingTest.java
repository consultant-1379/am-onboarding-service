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
package com.ericsson.amonboardingservice.presentation.services.onboarding.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.ericsson.amonboardingservice.utils.Constants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppUserDefinedData;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageDatabaseService;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@RunWith(MockitoJUnitRunner.class)
public class SkipImageDuringOnboardingTest {

    private static final String SKIP_IMAGE_UPLOAD_SET_TO_TRUE_WITH_DISABLED_CONTAINER_REGISTRY =
            "Inverting skipImagesUpload parameter of package test_packageID, due to disabled Container Registry";

    @InjectMocks
    private OnboardImages onboardImages;

    @Mock
    private PackageDatabaseService databaseService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void skipImageIsTrueWithInternalRegistryDisabledAndEmptyUserData() {
        List<ILoggingEvent> loggingEvents = runListAppender();
        AppPackage appPackage = new AppPackage().setPackageId("test_packageID");
        when(databaseService.getAppPackageById(any())).thenReturn(appPackage);

        boolean isPackageImagelessActualValue = onboardImages.isPackageImageless("test_packageID", false);

        assertThat(isPackageImagelessActualValue).isTrue();
        assertThat(loggingEvents).hasSize(3);
        List<String> actualMessages = loggingEvents.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
        assertThat(actualMessages.contains(SKIP_IMAGE_UPLOAD_SET_TO_TRUE_WITH_DISABLED_CONTAINER_REGISTRY)).isTrue();
    }

    @Test
    public void skipImageIsTrueWithInternalRegistryDisabled() {
        List<ILoggingEvent> loggingEvents = runListAppender();
        AppUserDefinedData appUserDefinedData = new AppUserDefinedData().setKey(Constants.SKIP_IMAGE_UPLOAD).setValue("false");
        List<AppUserDefinedData> appUserDefinedDataList = new ArrayList<>();
        appUserDefinedDataList.add(appUserDefinedData);
        AppPackage appPackage = new AppPackage().setPackageId("test_packageID").setUserDefinedData(appUserDefinedDataList);
        when(databaseService.getAppPackageById(any())).thenReturn(appPackage);

        boolean isPackageImagelessActualValue = onboardImages.isPackageImageless("test_packageID", false);

        assertThat(isPackageImagelessActualValue).isTrue();
        assertThat(loggingEvents).hasSize(3);
        List<String> actualMessages = loggingEvents.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
        assertThat(actualMessages.contains(SKIP_IMAGE_UPLOAD_SET_TO_TRUE_WITH_DISABLED_CONTAINER_REGISTRY)).isTrue();
    }

    @Test
    public void skipImageIsFalseWithInternalRegistryEnabled() {
        List<ILoggingEvent> loggingEvents = runListAppender();
        AppUserDefinedData appUserDefinedData = new AppUserDefinedData().setKey(Constants.SKIP_IMAGE_UPLOAD).setValue("false");
        List<AppUserDefinedData> appUserDefinedDataList = new ArrayList<>();
        appUserDefinedDataList.add(appUserDefinedData);
        AppPackage appPackage = new AppPackage().setPackageId("test_packageID").setUserDefinedData(appUserDefinedDataList);
        when(databaseService.getAppPackageById(any())).thenReturn(appPackage);

        boolean isPackageImagelessActualValue = onboardImages.isPackageImageless("test_packageID", true);

        assertThat(isPackageImagelessActualValue).isFalse();
        assertThat(loggingEvents).hasSize(2);
        List<String> actualMessages = loggingEvents.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
        assertThat(actualMessages.contains(SKIP_IMAGE_UPLOAD_SET_TO_TRUE_WITH_DISABLED_CONTAINER_REGISTRY)).isFalse();
    }

    @Test
    public void skipImageIsTrueWithInternalRegistryEnabled() {
        List<ILoggingEvent> loggingEvents = runListAppender();
        AppUserDefinedData appUserDefinedData = new AppUserDefinedData().setKey(Constants.SKIP_IMAGE_UPLOAD).setValue("true");
        List<AppUserDefinedData> appUserDefinedDataList = new ArrayList<>();
        appUserDefinedDataList.add(appUserDefinedData);
        AppPackage appPackage = new AppPackage().setPackageId("test_packageID").setUserDefinedData(appUserDefinedDataList);
        when(databaseService.getAppPackageById(any())).thenReturn(appPackage);

        boolean isPackageImagelessActualValue = onboardImages.isPackageImageless("test_packageID", true);

        assertThat(isPackageImagelessActualValue).isTrue();
        assertThat(loggingEvents).hasSize(2);
        List<String> actualMessages = loggingEvents.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
        assertThat(actualMessages.contains(SKIP_IMAGE_UPLOAD_SET_TO_TRUE_WITH_DISABLED_CONTAINER_REGISTRY)).isFalse();
    }

    private List<ILoggingEvent> runListAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(OnboardImages.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        return listAppender.list;
    }
}
