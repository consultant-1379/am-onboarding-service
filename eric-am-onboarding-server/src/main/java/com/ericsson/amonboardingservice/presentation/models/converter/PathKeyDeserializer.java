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

import java.nio.file.Paths;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdKeyDeserializer;

public class PathKeyDeserializer extends StdKeyDeserializer {

    public PathKeyDeserializer(final int kind, final Class<?> cls) {
        super(kind, cls);
    }

    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt) {
        return Paths.get(key);
    }
}
