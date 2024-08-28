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

import static com.ericsson.am.shared.http.HttpUtility.getCurrentHttpRequest;
import static com.ericsson.am.shared.http.HttpUtility.getHostUrl;

import com.ericsson.amonboardingservice.presentation.models.OnboardingResponseLinks;
import com.ericsson.amonboardingservice.presentation.models.URILink;

public final class LinksUtility {

    private static final String VNFM_ONBOARDING_PATH = "/vnfm/onboarding";
    private static final String VNFD_PATH = "/vnfd";
    private static final String PACKAGE_CONTENT_PATH = "/package_content";

    private LinksUtility() {
    }

    public static OnboardingResponseLinks getOnboardingResponseLinks(final String packageId) {
        final OnboardingResponseLinks links = new OnboardingResponseLinks();
        final URILink self = new URILink();
        final String uri = VNFM_ONBOARDING_PATH + getCurrentHttpRequest().getRequestURI();
        final String host = getHostUrl();
        self.setHref(host + uri + "/" + packageId);
        links.setSelf(self);
        return links;
    }

    public static String constructSelfLinkWithId(final String id) {
        final String uri = VNFM_ONBOARDING_PATH + getCurrentHttpRequest().getRequestURI();
        final String host = getHostUrl();
        return uri.contains(id) ? host + uri :  host + uri + "/" + id;
    }

    public static String constructVnfdLink(final String id) {
        final String selfLink = constructSelfLinkWithId(id);
        return selfLink + VNFD_PATH;
    }

    public static String constructPackageContentLink(final String id) {
        final String selfLink = constructSelfLinkWithId(id);
        return selfLink + PACKAGE_CONTENT_PATH;
    }
}
