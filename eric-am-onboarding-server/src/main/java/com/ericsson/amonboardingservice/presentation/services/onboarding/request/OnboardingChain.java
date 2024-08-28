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

import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
/**
 * Configuration of the request handlers in the chain of responsibility for a package upload request.
 * If a new artifact in the package is to be handled the handler should be added to the chain here.
 */
@Component
public class OnboardingChain {

    @Autowired
    private GenerateChecksum generateChecksum;

    @Autowired
    private UnpackZIP unpackZIP;

    @Autowired
    private CsarSignatureValidation csarSignatureValidation;

    @Autowired
    private UnpackCSAR unpackCSAR;

    @Autowired
    private ManifestSignatureValidation manifestSignatureValidation;

    @Autowired
    private ToscaVersionIdentification toscaVersionIdentification;

    @Autowired
    private ToscaValidation toscaValidation;

    @Autowired
    private VnfdValidation vnfdValidation;

    @Autowired
    private OnboardCharts onboardCharts;

    @Autowired
    private OnboardImages onboardImages;

    @Autowired
    private OnboardingSynchronization onboardingSynchronization;

    @Autowired
    private Persist persist;

    private List<RequestHandler> allHandlersChain;
    private List<RequestHandler> initialHandlersChain;

    @PostConstruct
    public void initHandlersChain() {
        initialHandlersChain = List.of(generateChecksum,
                                       unpackZIP,
                                       csarSignatureValidation,
                                       unpackCSAR,
                                       manifestSignatureValidation);

        allHandlersChain = List.of(generateChecksum,
                                   unpackZIP,
                                   csarSignatureValidation,
                                   unpackCSAR,
                                   manifestSignatureValidation,
                                   toscaVersionIdentification,
                                   onboardingSynchronization,
                                   toscaValidation,
                                   vnfdValidation,
                                   onboardCharts,
                                   onboardImages,
                                   persist);
    }

    public List<RequestHandler> buildDefaultChain() {
        return allHandlersChain;
    }

    public List<RequestHandler> buildChainByLastPhase(String lastPhaseName) {
        RequestHandler failedPhase = allHandlersChain.stream()
                .filter(phase -> phase.getName().equals(lastPhaseName))
                .findFirst()
                .orElse(allHandlersChain.get(0));
        if (initialHandlersChain.contains(failedPhase)) {
            return allHandlersChain;
        } else {
            List<RequestHandler> handlers = new ArrayList<>(initialHandlersChain);
            int indexOfLastPhaseHandler = allHandlersChain.indexOf(failedPhase);
            for (int phaseIndex = indexOfLastPhaseHandler; phaseIndex < allHandlersChain.size(); phaseIndex++) {
                handlers.add(allHandlersChain.get(phaseIndex));
            }
            return handlers;
        }
    }
}
