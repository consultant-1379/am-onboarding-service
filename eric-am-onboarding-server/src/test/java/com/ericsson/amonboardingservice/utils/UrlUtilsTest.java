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

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.presentation.exceptions.InvalidPaginationQueryException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@MockBean(classes = ServletUriComponentsBuilder.class)
public class UrlUtilsTest extends AbstractDbSetupTest {

    private static final int PAGE_SIZE = 3;

    @Test
    public void shouldReturnEmptyHeadersWhenUnpaged() {
        HttpHeaders headers = UrlUtils.createPaginationHeaders(Page.empty());

        assertTrue(headers.isEmpty());
    }

    @Test
    public void shouldReturnPaginationHeaders() {
        HttpHeaders headers = UrlUtils.createPaginationHeaders(page(0, 5));

        assertTrue(headers.containsKey(UrlUtils.PAGINATION_INFO));
        assertTrue(headers.containsKey(HttpHeaders.LINK));
    }

    @Test
    public void shouldReturnCorrectPaginationLinksForTheFirstPage() {
        HttpHeaders headers = UrlUtils.createPaginationHeaders(page(0, 5));

        assertThat(headers.get(HttpHeaders.LINK).get(0))
                .isEqualTo("<http://localhost?nextpage_opaque_marker=1>;rel=\"self\"," +
                        "<http://localhost?nextpage_opaque_marker=1>;rel=\"first\"," +
                        "<http://localhost?nextpage_opaque_marker=2>;rel=\"last\"," +
                        "<http://localhost?nextpage_opaque_marker=2>;rel=\"next\"");
        assertThat(headers.get(UrlUtils.PAGINATION_INFO).get(0))
                .isEqualTo("number=1,size=3,totalPages=2,totalElements=5");
    }

    @Test
    public void shouldReturnCorrectPaginationLinksForNotTheFirstPage() {
        HttpHeaders headers = UrlUtils.createPaginationHeaders(page(1, 10));

        assertThat(headers.get(HttpHeaders.LINK).get(0))
                .isEqualTo("<http://localhost?nextpage_opaque_marker=2>;rel=\"self\"," +
                        "<http://localhost?nextpage_opaque_marker=1>;rel=\"first\"," +
                        "<http://localhost?nextpage_opaque_marker=4>;rel=\"last\"," +
                        "<http://localhost?nextpage_opaque_marker=1>;rel=\"previous\"," +
                        "<http://localhost?nextpage_opaque_marker=3>;rel=\"next\"");
        assertThat(headers.get(UrlUtils.PAGINATION_INFO).get(0))
                .isEqualTo("number=2,size=3,totalPages=4,totalElements=10");
    }

    @Test
    public void shouldReturnCorrectPaginationLinksForSinglePage() {
        HttpHeaders headers = UrlUtils.createPaginationHeaders(page(0, 2));

        assertThat(headers.get(HttpHeaders.LINK).get(0))
                .isEqualTo("<http://localhost?nextpage_opaque_marker=1>;rel=\"self\"," +
                        "<http://localhost?nextpage_opaque_marker=1>;rel=\"first\"," +
                        "<http://localhost?nextpage_opaque_marker=1>;rel=\"last\"");
        assertThat(headers.get(UrlUtils.PAGINATION_INFO).get(0))
                .isEqualTo("number=1,size=3,totalPages=1,totalElements=2");
    }

    @Test
    public void shouldThrowExceptionIfPageNumberExceedsTotalPages() {
        assertThatThrownBy(() -> UrlUtils.createPaginationHeaders(page(5, 1)))
                .isInstanceOf(InvalidPaginationQueryException.class)
                .hasMessage("Requested page number exceeds the total number of pages. " +
                        "Requested page: 6. Total pages: 1");
    }

    private static Page<?> page(int pageNumber, int totalElements) {
        return new PageImpl<>(List.of(), PageRequest.of(pageNumber, PAGE_SIZE), totalElements);
    }
}
