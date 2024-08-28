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

import com.ericsson.amonboardingservice.presentation.exceptions.InvalidPaginationQueryException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class PageableFactory {

    public static final int DEFAULT_PAGE_SIZE = 15;
    public static final int MAX_PAGE_SIZE = 100;

    private final boolean paginationDisabledByDefault;

    public PageableFactory(@Value("${onboarding.pagination.disabledByDefault:true}")
                           boolean paginationDisabledByDefault) {
        this.paginationDisabledByDefault = paginationDisabledByDefault;
    }

    public Pageable createPageable(String page, Integer size) {
        if (paginationDisabled(page, size)) {
            return Pageable.unpaged();
        }

        int parsedPage = parsePage(page);
        int parsedSize = parseSize(size);
        validate(parsedPage, parsedSize);

        return PageRequest.of(parsedPage - 1, parsedSize);
    }

    private boolean paginationDisabled(String page, Integer size) {
        return paginationDisabledByDefault && page == null && size == null;
    }

    private static int parsePage(String page) {
        if (page == null) {
            return 1;
        }
        try {
            return Integer.parseInt(page);
        } catch (NumberFormatException numberFormatException) {
            throw new InvalidPaginationQueryException(
                    String.format("Invalid page value for nextpage_opaque_marker: %s", page),
                    numberFormatException
            );
        }
    }

    private static int parseSize(Integer size) {
        return size == null ? DEFAULT_PAGE_SIZE : size;
    }

    private static void validate(int page, int size) {
        validatePage(page);
        validateSize(size);
    }

    private static void validatePage(int page) {
        if (page < 1) {
            throw new InvalidPaginationQueryException(
                    String.format("Invalid page number: %s, page number must be greater than 0", page));
        }
    }

    private static void validateSize(int size) {
        if (size < 1) {
            throw new InvalidPaginationQueryException(
                    String.format("Invalid page size: %s, page size must be greater than 0", size));
        }

        if (size > MAX_PAGE_SIZE) {
            throw new InvalidPaginationQueryException(
                    String.format("Total size of the results will be shown cannot be more than %s. Requested"
                            + " page size %s", MAX_PAGE_SIZE, size));
        }
    }

}