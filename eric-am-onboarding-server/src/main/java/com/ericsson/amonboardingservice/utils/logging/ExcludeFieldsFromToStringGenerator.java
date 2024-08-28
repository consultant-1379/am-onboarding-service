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
package com.ericsson.amonboardingservice.utils.logging;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public enum ExcludeFieldsFromToStringGenerator {

    INSTANCE;

    private Map<String, ToStringStyle> stylesForClass = new HashMap<>();

    private static Set<String> calculateExcludeFields(Class classDefinition) {
        return Arrays.stream(classDefinition.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(ExcludeFieldsFromToString.class))
                .map(Field::getName)
                .collect(Collectors.toSet());
    }

    public ToStringStyle getStyle(Class loggedObjectType) {
        return (loggedObjectType == null)
                ? null
                : stylesForClass.computeIfAbsent(loggedObjectType.getCanonicalName(),
                    className -> new ExcludeFieldsFromToStringStyle(calculateExcludeFields(loggedObjectType)));
    }

    /**
     * <p>Works with {@link ToStringBuilder} to create a <code>toString</code>.</p>
     *
     * <p><code>ToStringStyle</code> that does not print fields, which are marked by special annotation {@link ExcludeFieldsFromToString}.</p>
     */
    private static final class ExcludeFieldsFromToStringStyle extends ToStringStyle {
        private static final long serialVersionUID = 1L;

        private final Set<String> excludedFields;

        private ExcludeFieldsFromToStringStyle(Set<String> excludedFields) {
            this.excludedFields = excludedFields;
        }

        @Override
        public void append(final StringBuffer buffer, final String fieldName, final Object value, final Boolean fullDetail) {
            if (!excludedFields.contains(fieldName)) {
                super.append(buffer, fieldName, value, fullDetail);
            }
        }
    }

}

