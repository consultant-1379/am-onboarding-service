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

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.amonboardingservice.presentation.services.onboarding.request.OnboardingPhase.CHARTS_ONBOARDING_PHASE;
import static com.ericsson.amonboardingservice.presentation.services.onboarding.request.OnboardingPhase.CSAR_UNPACKING_PHASE;
import static com.ericsson.amonboardingservice.presentation.services.onboarding.request.OnboardingPhase.IMAGES_ONBOARDING_PHASE;
import static com.ericsson.amonboardingservice.presentation.services.onboarding.request.OnboardingPhase.PERSIST_PHASE;
import static com.ericsson.amonboardingservice.presentation.services.onboarding.request.OnboardingPhase.TOSCA_VERSION_IDENTIFICATION_PHASE;
import static com.ericsson.amonboardingservice.presentation.services.onboarding.request.OnboardingPhase.VNFD_VALIDATION_PHASE;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.am.shared.vnfd.service.ToscaoService;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.services.ToscaHelper;
import com.ericsson.amonboardingservice.presentation.services.auditservice.AuditService;
import com.ericsson.amonboardingservice.presentation.services.dockerservice.DockerService;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.ericsson.amonboardingservice.presentation.services.filter.VnfPackageQuery;
import com.ericsson.amonboardingservice.presentation.services.helmservice.HelmService;
import com.ericsson.amonboardingservice.presentation.services.manifestservice.ManifestService;
import com.ericsson.amonboardingservice.presentation.services.packageservice.CleanUpOnFailureService;
import com.ericsson.amonboardingservice.presentation.services.packageservice.OnboardingDetailsService;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageDatabaseService;
import com.ericsson.amonboardingservice.presentation.services.servicemodelservice.ServiceModelService;
import com.ericsson.amonboardingservice.presentation.services.supportedoperationservice.SupportedOperationService;
import com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaService;
import com.ericsson.amonboardingservice.presentation.services.vnfdservice.VnfdService;
import com.ericsson.amonboardingservice.utils.PackageSignatureRetriever;
import com.ericsson.signatureservice.SignatureService;

@SpringBootTest(classes = {
        OnboardingChain.class,
        GenerateChecksum.class,
        UnpackZIP.class,
        CsarSignatureValidation.class,
        UnpackCSAR.class,
        ManifestSignatureValidation.class,
        ToscaVersionIdentification.class,
        ToscaValidation.class,
        VnfdValidation.class,
        OnboardCharts.class,
        OnboardImages.class,
        Persist.class,
        OnboardingSynchronization.class
})
@MockBean(classes = {
        FileService.class,
        PackageSignatureRetriever.class,
        SignatureService.class,
        ToscaMetaService.class,
        ManifestService.class,
        VnfdService.class,
        AppPackageRepository.class,
        VnfPackageQuery.class,
        ToscaoService.class,
        HelmService.class,
        DockerService.class,
        AuditService.class,
        ServiceModelService.class,
        SupportedOperationService.class,
        ToscaHelper.class,
        PackageDatabaseService.class,
        CleanUpOnFailureService.class,
        OnboardingDetailsService.class
})
public class OnboardingChainTest {

    @Autowired
    private OnboardingChain onboardingChain;

    @Test
    public void shouldBuildDefaultChain() {
        List<RequestHandler> requestHandlers = onboardingChain.buildDefaultChain();
        assertThat(requestHandlers.size()).isEqualTo(12);
        for (RequestHandler handler : requestHandlers) {
            assertThat(handler.getName()).isEqualTo(OnboardingPhase.values()[requestHandlers.indexOf(handler)].toString());
        }
    }

    @Test
    public void shouldBuildChainByLastPhaseWhenPhaseIsInitial() {
        List<RequestHandler> requestHandlers = onboardingChain.buildChainByLastPhase(CSAR_UNPACKING_PHASE.toString());
        assertThat(requestHandlers.size()).isEqualTo(12);
        for (RequestHandler handler : requestHandlers) {
            assertThat(handler.getName()).isEqualTo(OnboardingPhase.values()[requestHandlers.indexOf(handler)].toString());
        }
    }

    @Test
    public void shouldBuildChainByLastPhaseWhenPhaseIsUnknown() {
        List<RequestHandler> requestHandlers = onboardingChain.buildChainByLastPhase("Invalid values");
        assertThat(requestHandlers.size()).isEqualTo(12);
        for (RequestHandler handler : requestHandlers) {
            assertThat(handler.getName()).isEqualTo(OnboardingPhase.values()[requestHandlers.indexOf(handler)].toString());
        }
    }

    @Test
    public void shouldBuildChainByLastPhaseWhenPhaseIsToscaVersionIdentification() {
        List<RequestHandler> requestHandlers = onboardingChain.buildChainByLastPhase(TOSCA_VERSION_IDENTIFICATION_PHASE.toString());
        assertThat(requestHandlers.size()).isEqualTo(12);
        for (RequestHandler handler : requestHandlers) {
            assertThat(handler.getName()).isEqualTo(OnboardingPhase.values()[requestHandlers.indexOf(handler)].toString());
        }
    }

    @Test
    public void shouldBuildChainByLastPhaseWhenPhaseIsChartsOnboarding() {
        List<RequestHandler> requestHandlers = onboardingChain.buildChainByLastPhase(CHARTS_ONBOARDING_PHASE.toString());
        assertThat(requestHandlers.size()).isEqualTo(8);
        assertThat(requestHandlers).extracting(RequestHandler::getName).doesNotContain(TOSCA_VERSION_IDENTIFICATION_PHASE.toString(),
                                                                                       VNFD_VALIDATION_PHASE.toString());
    }

    @Test
    public void shouldBuildChainByLastPhaseWhenPhaseIsPersist() {
        List<RequestHandler> requestHandlers = onboardingChain.buildChainByLastPhase(PERSIST_PHASE.toString());
        assertThat(requestHandlers.size()).isEqualTo(6);
        assertThat(requestHandlers).extracting(RequestHandler::getName).doesNotContain(TOSCA_VERSION_IDENTIFICATION_PHASE.toString(),
                                                                                       VNFD_VALIDATION_PHASE.toString(),
                                                                                       CHARTS_ONBOARDING_PHASE.toString(),
                                                                                       IMAGES_ONBOARDING_PHASE.toString());
    }
}