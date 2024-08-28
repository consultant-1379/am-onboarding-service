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

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import com.google.common.net.MediaType;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by eendjor on 22/08/2018.
 */
@Slf4j
@Component
public class FileTypeDetector {
    /**
     * A file type detector for probing a file to guess its file type.
     *
     * The initial bytes in a file are examined to guess its file type.
     *
     * @param inputStream file input stream.
     * @param contentType expected content type.
     *
     * @return
     *      returns <code>true</code> if file has given #contentType, otherwise <code>false</code>
     */
    public boolean hasFileContentType(InputStream inputStream, MediaType contentType) {
        LOGGER.info("Started checking whether file has content type: {}", contentType);
        try {
            String type = new Tika().detect(inputStream);
            boolean hasFileContentType = Objects.equals(type, contentType.toString());
            LOGGER.info("Completed checking whether file has content type: {}", contentType);
            return hasFileContentType;
        } catch (IOException e) {
            LOGGER.error("It was not possible to determine the file type but will continue", e);
            return false;
        }
    }
}
