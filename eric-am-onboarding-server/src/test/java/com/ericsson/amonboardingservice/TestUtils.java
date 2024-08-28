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

import static java.nio.file.Files.newInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.ericsson.amonboardingservice.logging.InMemoryAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.slf4j.LoggerFactory;

public final class TestUtils {

    private TestUtils(){}

    public static InputStream createInputStream(String fileName) throws URISyntaxException, IOException {
        return newInputStream(getResource(fileName));
    }

    public static Path getResource(String resourceName) throws URISyntaxException {
        return Paths.get(Resources.getResource(resourceName).toURI());
    }

    public static String readDataFromFile(String fileName, Charset charset) throws IOException {
        return Resources.toString(Resources.getResource(fileName), charset);
    }


    public static byte[] convertToBytes(Object object) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsBytes(object);
    }

    public static InMemoryAppender getInMemoryAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger("com.ericsson.amonboardingservice");
        InMemoryAppender inMemoryAppender = new InMemoryAppender();
        inMemoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.addAppender(inMemoryAppender);
        inMemoryAppender.start();

        return inMemoryAppender;
    }
}
