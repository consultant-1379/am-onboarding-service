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

import static java.util.stream.Collectors.toList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.amonboardingservice.presentation.services.packageservice.OnboardingDetailsService;

@SpringBootTest(classes = {OnboardingProcessHandler.class, OnboardingChain.class})
public class OnboardingProcessHandlerTest {

    @Autowired
    private OnboardingProcessHandler onboardingProcessHandler;

    @Autowired
    private OnboardingChain onboardingChain;

    @MockBean
    private GenerateChecksum generateChecksum;

    @MockBean
    private UnpackCSAR unpackCSAR;

    @MockBean
    private UnpackZIP unpackZIP;

    @MockBean
    private VnfdValidation vnfdValidation;
    @MockBean
    private ToscaValidation toscaValidation;
    @MockBean
    private ManifestSignatureValidation manifestSignatureValidation;

    @MockBean
    private CsarSignatureValidation csarSignatureValidation;

    @MockBean
    private ToscaVersionIdentification toscaVersionIdentification;

    @MockBean
    private OnboardCharts onboardCharts;

    @MockBean
    private OnboardImages onboardImages;

    @MockBean
    private OnboardingSynchronization onboardingSynchronization;

    @MockBean
    private Persist persist;

    @MockBean
    private OnboardingDetailsService onboardingDetailService;

    @Test
    public void shouldRetrieveAndSaveContextForEachStage() {
        // given
        final List<PackageUploadRequestContext> contexts = IntStream.range(0, 12)
                .mapToObj(i -> new PackageUploadRequestContext(null, null, null, Integer.toString(i)))
                .collect(toList());

        final Queue<PackageUploadRequestContext> contextsQueue = new LinkedBlockingQueue<>(contexts);

        when(onboardingDetailService.setPhaseAndRetrieveCurrentContext(any(), any())).thenAnswer(invocation -> contextsQueue.remove());

        // when
        onboardingProcessHandler.startOnboardingProcess("package-id", onboardingChain.buildDefaultChain());

        // then
        verify(onboardingDetailService, times(12)).setPhaseAndRetrieveCurrentContext(any(), any());

        final List<RequestHandler> handlers = getRequestHandlers();
        for (int i = 0; i < contexts.size(); i++) {
            assertThat(captureHandlerContext(handlers.get(i))).isSameAs(contexts.get(i));
        }

        final ArgumentCaptor<PackageUploadRequestContext> captor = ArgumentCaptor.forClass(PackageUploadRequestContext.class);
        verify(onboardingDetailService, times(12)).saveOnboardingContext(any(), captor.capture());

        assertThat(captor.getAllValues()).containsExactlyElementsOf(contexts);
    }

    private List<RequestHandler> getRequestHandlers() {
        final List<RequestHandler> handlers = List.of(generateChecksum,
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
        return handlers;
    }

    private PackageUploadRequestContext captureHandlerContext(final RequestHandler handler) {
        final ArgumentCaptor<PackageUploadRequestContext> captor = ArgumentCaptor.forClass(PackageUploadRequestContext.class);
        verify(handler).handle(captor.capture());

        return captor.getValue();
    }
}
