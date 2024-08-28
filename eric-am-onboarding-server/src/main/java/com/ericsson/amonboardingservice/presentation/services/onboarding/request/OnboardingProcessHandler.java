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
package com.ericsson.amonboardingservice.presentation.services.onboarding.request;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.amonboardingservice.presentation.services.packageservice.OnboardingDetailsService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OnboardingProcessHandler {

    @Autowired
    private OnboardingDetailsService onboardingDetailService;

    public void startOnboardingProcess(final String packageId, List<RequestHandler> handlers) {
        LOGGER.info("Starting onboarding process chain");

        for (final RequestHandler currentHandler : handlers) {
            final PackageUploadRequestContext currentContext =
                    onboardingDetailService.setPhaseAndRetrieveCurrentContext(packageId, currentHandler.getName());

            currentHandler.handle(currentContext);

            onboardingDetailService.saveOnboardingContext(packageId, currentContext);
        }

        LOGGER.info("Completed onboarding process chain");
    }
}
