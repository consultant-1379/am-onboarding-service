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

import com.ericsson.amonboardingservice.presentation.exceptions.InvalidPaginationQueryException;
import com.ericsson.amonboardingservice.presentation.models.PaginationInfo;
import com.ericsson.amonboardingservice.presentation.models.URILink;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class UrlUtils {

    public static final String PAGINATION_INFO = "PaginationInfo";
    public static final String FIRST = "first";
    public static final String PREVIOUS = "previous";
    public static final String SELF = "self";
    public static final String NEXT = "next";
    public static final String LAST = "last";
    public static final String NUMBER = "number";
    public static final String SIZE = "size";
    public static final String TOTAL_PAGES = "totalPages";
    public static final String TOTAL_ELEMENTS = "totalElements";
    public static final String NEXTPAGE_OPAQUE_MARKER = "nextpage_opaque_marker";
    public static final String PAGE = "page";

    public static HttpHeaders createPaginationHeaders(Page<?> page) {
        if (page.getPageable().isUnpaged()) {
            return new HttpHeaders();
        }
        PaginationInfo paginationInfo = buildPaginationInfo(page);
        return createHeaders(paginationInfo);
    }

    private static PaginationInfo buildPaginationInfo(Page<?> page) {
        validateMaxPageNumber(page);
        return PaginationInfo.builder()
                .number(page.getNumber() + 1)
                .size(page.getSize())
                .totalPages(getLastPage(page))
                .totalElements((int) page.getTotalElements())
                .build();
    }

    private static HttpHeaders createHeaders(PaginationInfo paginationInfo) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LINK, createLinkHeaderValue(paginationInfo));
        headers.add(PAGINATION_INFO, createPaginationHeaderValue(paginationInfo));
        return headers;
    }

    private static int getLastPage(final Page<?> page) {
        return page.getTotalPages() == 0 ? 1 : page.getTotalPages();
    }

    private static void validateMaxPageNumber(final Page<?> page) {
        if (page.getNumber() != 0 && page.getNumber() + 1 > page.getTotalPages()) {
            throw new InvalidPaginationQueryException(String.format(
                    "Requested page number exceeds the total number of pages. Requested page: %s. Total "
                            + "pages: %s",
                    page.getNumber() + 1,
                    page.getTotalPages()));
        }
    }

    private static String createLinkHeaderValue(PaginationInfo paginationInfo) {
        List<URILink> refLinks = createRefLinks(paginationInfo, PAGE);

        return refLinks.stream()
                .map(url -> "<" + url.getHref() + ">;" + url.getFormattedRel())
                .collect(Collectors.joining(","));
    }

    private static List<URILink> createRefLinks(PaginationInfo paginationInfo, String... queriesToRemove) {
        List<URILink> links = new ArrayList<>();
        int pageSelf = paginationInfo.getNumber();
        int pageLast = paginationInfo.getTotalPages();

        links.add(createLink(SELF, NEXTPAGE_OPAQUE_MARKER, pageSelf, queriesToRemove));
        links.add(createLink(FIRST, NEXTPAGE_OPAQUE_MARKER, 1, queriesToRemove));
        links.add(createLink(LAST, NEXTPAGE_OPAQUE_MARKER, pageLast, queriesToRemove));

        if (pageSelf > 1) {
            links.add(createLink(PREVIOUS, NEXTPAGE_OPAQUE_MARKER, pageSelf - 1, queriesToRemove));
        }
        if (pageSelf < paginationInfo.getTotalPages()) {
            links.add(createLink(NEXT, NEXTPAGE_OPAQUE_MARKER, pageSelf + 1, queriesToRemove));
        }
        return links;
    }

    private static URILink createLink(String rel, String query, int queryValue, String... queriesToRemove) {
        ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentRequest();
        builder.replaceQueryParam(query, queryValue);
        Arrays.stream(queriesToRemove).forEach(builder::replaceQueryParam);
        return new URILink().href(builder.build().toUriString()).rel(rel);
    }

    private static String createPaginationHeaderValue(PaginationInfo vnfInstancePage) {
        HashMap<String, Integer> pagination = new HashMap<>();
        pagination.put(NUMBER, vnfInstancePage.getNumber());
        pagination.put(SIZE, vnfInstancePage.getSize());
        pagination.put(TOTAL_PAGES, vnfInstancePage.getTotalPages());
        pagination.put(TOTAL_ELEMENTS, vnfInstancePage.getTotalElements());

        return pagination.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(","));
    }

}
