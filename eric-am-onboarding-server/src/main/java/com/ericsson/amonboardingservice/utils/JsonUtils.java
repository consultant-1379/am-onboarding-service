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
package com.ericsson.amonboardingservice.utils;

import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
public final class JsonUtils {

    public static final String EMPTY_JSON_MSG = "Json String is empty or null";
    private static final String INVALID_JSON_MSG = "Invalid Json String";

    private JsonUtils() {
    }

    public static String formatJsonString(String jsonString) {
        String formattedJson;
        if (Strings.isNullOrEmpty(jsonString)) {
            formattedJson = EMPTY_JSON_MSG;
        } else {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Object json = mapper.readValue(jsonString, Object.class);

                formattedJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
            } catch (IOException jpe) {
                formattedJson = INVALID_JSON_MSG;
                LOGGER.error("Error parsing the Json String", jpe);
            }
        }
        return formattedJson;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getJsonValue(final String json, final String key, final T defaultValue) {
        if (!Strings.isNullOrEmpty(json) && !Strings.isNullOrEmpty(key)) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, T> respMap = objectMapper.readValue(json, Map.class);
                return respMap.entrySet().stream().filter(c -> c.getKey().equals(key)).findFirst().
                        map(Map.Entry::getValue).orElse(defaultValue);
            } catch (final IOException ioe) {
                LOGGER.error("Failed to parse json response", ioe);
            } catch (final Exception ex) {
                LOGGER.error("Unknown error", ex);
            }
        } else {
            LOGGER.error("Invalid Values provided to method");
        }
        return null;
    }

    public static <T> String getJsonFromObj(T obj) {
        if (obj != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.writeValueAsString(obj);
            } catch (JsonProcessingException e) {
                LOGGER.error("Failed to parse object into json", e);
            }
        } else {
            LOGGER.error("Object can't be null");
        }
        return null;
    }

    public static <T> T parseJsonToClass(final String json, Class<T> clazz) {
        if (!Strings.isNullOrEmpty(json) && clazz != null) {

            ObjectMapper objectMapper = new ObjectMapper();
            try {
                return objectMapper.readValue(json, clazz);
            } catch (JsonProcessingException e) {
                throw new InternalRuntimeException("Failed to parse JSON into class", e);
            }
        } else {
            throw new InternalRuntimeException("Failed to parse JSON to class: there is null JSON or class.");
        }
    }

    public static String readJsonFromResource(String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    public static <T extends List<?>> T cast(Object obj) {
        return (T) obj;
    }

    public static Map convertStringToJSONObj(String jsonString) {
        try {
            return new ObjectMapper().readValue(jsonString, Map.class);
        } catch (JSONException | JsonProcessingException e) {
            LOGGER.error("Unable to convert to JSON Object. Invalid JSON string", e);
        }
        return null;
    }
}
