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
package com.ericsson.amonboardingservice.presentation.models.converter;

import java.nio.file.Path;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.ericsson.amonboardingservice.presentation.exceptions.ConvertingPackageUploadContextException;
import com.ericsson.amonboardingservice.presentation.services.onboarding.request.PackageUploadRequestContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

@Converter
@Slf4j
public class PackageUploadContextConverter implements AttributeConverter<PackageUploadRequestContext, String> {

    private final ObjectMapper mapper = createObjectMapper();

    @Override
    public String convertToDatabaseColumn(final PackageUploadRequestContext packageUploadRequestContext) {
        try {
            return mapper.writeValueAsString(packageUploadRequestContext);
        } catch (JsonProcessingException e) {
            throw new ConvertingPackageUploadContextException(String.format("Unable to convert PackageUploadRequestContext to json due to: %s", e));
        }
    }

    @Override
    public PackageUploadRequestContext convertToEntityAttribute(final String str) {
        try {
            return mapper.readValue(str, PackageUploadRequestContext.class);
        } catch (JsonProcessingException e) {
            throw new ConvertingPackageUploadContextException(String.format("Unable to convert json to PackageUploadRequestContext due to: %s", e));
        }
    }

    private static ObjectMapper createObjectMapper() {
        SimpleModule module = new SimpleModule();
        module.addKeyDeserializer(Path.class, new PathKeyDeserializer(1, Path.class));

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }
}
