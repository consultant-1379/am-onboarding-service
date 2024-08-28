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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import static com.ericsson.amonboardingservice.presentation.services.PageableFactory.DEFAULT_PAGE_SIZE;
import static com.ericsson.amonboardingservice.presentation.services.PageableFactory.MAX_PAGE_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PageableFactoryTest {

    private PageableFactory sut;

    @BeforeEach
    void setUp() {
        sut = new PageableFactory(false);
    }

    @Test
    public void shouldReturnUnpagedWhenPaginationDisabledAndNoParamsProvided() {
        sut = new PageableFactory(true);

        Pageable pageable = sut.createPageable(null, null);

        assertTrue(pageable.isUnpaged());
    }

    @Test
    public void shouldReturnDefaultPageableWhenPaginationEnabled() {
        Pageable pageable = sut.createPageable(null, null);

        assertTrue(pageable.isPaged());
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(DEFAULT_PAGE_SIZE);
    }

    @Test
    public void shouldReturnPageableWithDefaultSizeIfNotSpecified() {
        Pageable pageable = sut.createPageable("2", null);

        assertTrue(pageable.isPaged());
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(DEFAULT_PAGE_SIZE);
    }

    @Test
    public void shouldReturnPageableWithDefaultPageMarkerIfNotSpecified() {
        Pageable pageable = sut.createPageable(null, 10);

        assertTrue(pageable.isPaged());
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);
    }

    @Test
    public void shouldReturnPageableWhenPaginationDisabledWithParamsProvided() {
        sut = new PageableFactory(true);

        Pageable pageable = sut.createPageable("1", 10);

        assertTrue(pageable.isPaged());
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);
    }

    @Test
    public void shouldValidatePageNumberIsGreaterThanZero() {
        assertThatThrownBy(() -> sut.createPageable("-1", 1))
                .isInstanceOf(InvalidPaginationQueryException.class)
                .hasMessage("Invalid page number: -1, page number must be greater than 0");
    }

    @Test
    public void shouldValidatePageNumberIsValidInt() {
        assertThatThrownBy(() -> sut.createPageable("invalid", 1))
                .isInstanceOf(InvalidPaginationQueryException.class)
                .hasMessage("Invalid page value for nextpage_opaque_marker: invalid");
    }

    @Test
    public void shouldValidateSizeIsGreaterThanZero() {
        assertThatThrownBy(() -> sut.createPageable("1", -1))
                .isInstanceOf(InvalidPaginationQueryException.class)
                .hasMessage("Invalid page size: -1, page size must be greater than 0");
    }

    @Test
    public void shouldValidateSizeIsLessThanMaximum() {
        int size = MAX_PAGE_SIZE + 1;
        assertThatThrownBy(() -> sut.createPageable("1", size))
                .isInstanceOf(InvalidPaginationQueryException.class)
                .hasMessage( String.format("Total size of the results will be shown cannot be more than %s. Requested"
                        + " page size %s", MAX_PAGE_SIZE, size));
    }

}