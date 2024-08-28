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
package com.ericsson.amonboardingservice.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import com.ericsson.amonboardingservice.presentation.models.converter.PathKeyDeserializer;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class AppPackageLoggingTest {

    @Test
    public void logAppPackage() throws IOException {
        //given
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addKeyDeserializer(Path.class, new PathKeyDeserializer(1, Path.class));
        objectMapper.registerModule(module);
        File file = ResourceUtils.getFile("classpath:logging/appPackage.json");
        byte[] files = Files.readAllBytes(file.toPath());
        AppPackage appPackage = objectMapper.readValue(file, AppPackage.class);
        appPackage.setFiles(files);
        //when
        String appPackageInLogs = appPackage.toString();
        //then
        assertThat(appPackage.getDescriptorModel()).isNotBlank();
        assertThat(appPackage.getFiles()).isNotEmpty();
        assertThat(appPackageInLogs.contains(appPackage.getDescriptorModel())).isFalse();
    }
}
