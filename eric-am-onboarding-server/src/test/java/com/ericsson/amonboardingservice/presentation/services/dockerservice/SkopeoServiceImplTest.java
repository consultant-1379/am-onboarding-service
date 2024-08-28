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
package com.ericsson.amonboardingservice.presentation.services.dockerservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.presentation.exceptions.CommandTimedOutException;
import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.presentation.exceptions.SkopeoServiceException;
import com.ericsson.amonboardingservice.utils.executor.ProcessExecutor;
import com.ericsson.amonboardingservice.utils.executor.ProcessExecutorResponse;

@SpringBootTest(properties = {"onboarding.upload.concurrency=none", "skopeo.enabled=true"})
@ActiveProfiles("test")
class SkopeoServiceImplTest extends AbstractDbSetupTest {


    private static final String DOCKER_REPO_PATH = "/tmp/docker.tar";
    private static final String IMAGE_NAME = "proj-adp-eric-data-distributed-coordinator-ed-drop/eric-data-distributed-coordinator-ed-bragent";
    private static final String EMPTY_IMAGE_REPO = "";
    private static final String IMAGE_REPO = "armdocker.rnd.ericsson.se";
    private static final String IMAGE_TAG = "10.2.0-8";
    private static final int TIMEOUT_MINUTES = 5;

    @Autowired
    private SkopeoService skopeoService;
    @MockBean
    private ProcessExecutor processExecutor;

    @Test
    void pushImageFromDockerTar_shouldReturnNothing_whenProcessReturnZeroCode() throws CommandTimedOutException {
        // Mock configuration
        final int ZERO_EXIT_CODE = 0;
        ProcessExecutorResponse mockProcessResponse = new ProcessExecutorResponse();
        mockProcessResponse.setExitValue(ZERO_EXIT_CODE);

        when(processExecutor.executeProcessBuilder(anyString(), anyInt())).thenReturn(mockProcessResponse);

        // Test execution
        skopeoService.pushImageFromDockerTar(DOCKER_REPO_PATH, IMAGE_REPO, IMAGE_NAME, IMAGE_TAG, TIMEOUT_MINUTES);

        // Result assertion
        verify(processExecutor, times(1)).executeProcessBuilder(anyString(), anyInt());

    }

    @Test
    void pushImageFromDockerTar_shouldReturn503ServiceUnavailableExc_whenProcessReturnZeroCode() throws CommandTimedOutException {
        // Mock configuration
        final int ONE_EXIT_CODE = 1;
        ProcessExecutorResponse mockResponseWithError = new ProcessExecutorResponse();
        mockResponseWithError.setExitValue(ONE_EXIT_CODE);
        mockResponseWithError.setCmdErrorResult("time=\"2024-02-25T20:00:122\" level=fatal msg=\"trying to reuse blob "
                                                      + "sha256:acd64c6ce28b027133574b6a4581ee1d67b3de0e9f99acc2e72077f0e9be857 at destination:"
                                                      + " pinging container registry docker.zkulrus.haber002-vnfm11.ews.gic.ericsson.se:"
                                                      + " received unexpected HTTP status: 503 Service Unavailable");
        final int ZERO_EXIT_CODE = 0;
        ProcessExecutorResponse mockProcessResponse = new ProcessExecutorResponse();
        mockProcessResponse.setExitValue(ZERO_EXIT_CODE);


        when(processExecutor.executeProcessBuilder(anyString(), anyInt())).thenReturn(mockResponseWithError, mockProcessResponse);

        // Test execution
        skopeoService.pushImageFromDockerTar(DOCKER_REPO_PATH, IMAGE_REPO, IMAGE_NAME, IMAGE_TAG, TIMEOUT_MINUTES);

        // Result assertion
        verify(processExecutor, times(2)).executeProcessBuilder(anyString(), anyInt());

    }

    @Test
    void pushImageFromDockerTar_shouldCallProcessWithCmdContainingRepo() throws CommandTimedOutException {
        // Mock configuration
        String regexpWithRepo = "docker.tar:" + IMAGE_REPO + "/" + IMAGE_NAME;
        Pattern pattern = Pattern.compile(regexpWithRepo);


        final int ZERO_EXIT_CODE = 0;
        ProcessExecutorResponse mockProcessResponse = new ProcessExecutorResponse();
        mockProcessResponse.setExitValue(ZERO_EXIT_CODE);

        when(processExecutor.executeProcessBuilder(anyString(), anyInt())).thenReturn(mockProcessResponse);
        // Test execution
        skopeoService.pushImageFromDockerTar(DOCKER_REPO_PATH, IMAGE_REPO, IMAGE_NAME, IMAGE_TAG, TIMEOUT_MINUTES);

        // Result assertion
        verify(processExecutor, times(1)).executeProcessBuilder(matches(pattern), anyInt());
    }

    @Test
    void pushImageFromDockerTar_shouldCallProcessWithCmdNotContainingRepo_whenRepoIsEmpty() throws CommandTimedOutException {
        // Mock configuration
        String regexpWithoutRepo = "docker.tar:" + IMAGE_NAME;
        Pattern pattern = Pattern.compile(regexpWithoutRepo);

        final int ZERO_EXIT_CODE = 0;
        ProcessExecutorResponse mockProcessResponse = new ProcessExecutorResponse();
        mockProcessResponse.setExitValue(ZERO_EXIT_CODE);

        when(processExecutor.executeProcessBuilder(anyString(), anyInt())).thenReturn(mockProcessResponse);
        // Test execution
        skopeoService.pushImageFromDockerTar(DOCKER_REPO_PATH, EMPTY_IMAGE_REPO, IMAGE_NAME, IMAGE_TAG, TIMEOUT_MINUTES);

        // Result assertion
        verify(processExecutor, times(1)).executeProcessBuilder(matches(pattern), anyInt());
    }

    @Test
    void pushImageFromDockerTar_shouldThrowSkopeoException_whenProcessReturnNonZeroCode() throws CommandTimedOutException {
        // Mock configuration
        final int NON_ZERO_EXIT_CODE = 1;
        ProcessExecutorResponse mockProcessResponse = new ProcessExecutorResponse();
        mockProcessResponse.setExitValue(NON_ZERO_EXIT_CODE);
        when(processExecutor.executeProcessBuilder(anyString(), anyInt())).thenReturn(mockProcessResponse);

        // Test execution
        assertThrows(SkopeoServiceException.class, () ->
                skopeoService.pushImageFromDockerTar(DOCKER_REPO_PATH, IMAGE_REPO, IMAGE_NAME, IMAGE_TAG, TIMEOUT_MINUTES));

        verify(processExecutor, times(1)).executeProcessBuilder(anyString(), anyInt());

    }

    @Test
    void pushImageFromDockerTar_shouldThrowInternalRuntimeException_whenProcessExecutionThrowInternalRuntimeException() throws CommandTimedOutException {
        // Mock configuration
        InternalRuntimeException internalRuntimeException = new InternalRuntimeException("Error message");
        when(processExecutor.executeProcessBuilder(anyString(), anyInt())).thenThrow(internalRuntimeException);

        // Test execution
        try {
            skopeoService.pushImageFromDockerTar(DOCKER_REPO_PATH, IMAGE_REPO, IMAGE_NAME, IMAGE_TAG, TIMEOUT_MINUTES);
        } catch (Exception e) {
            assertEquals(InternalRuntimeException.class, e.getClass());
            assertEquals(internalRuntimeException.getMessage(), e.getMessage());
        }
        verify(processExecutor, times(1)).executeProcessBuilder(anyString(), anyInt());
    }

    @Test
    void pushImageFromDockerTar_shouldThrowInternalRuntimeException_whenProcessExecutionThrowsCommandTimeoutException() throws CommandTimedOutException {
        // Mock configuration
        when(processExecutor.executeProcessBuilder(anyString(), anyInt())).thenThrow(CommandTimedOutException.class);

        // Test execution
        assertThrows(InternalRuntimeException.class, () ->
                skopeoService.pushImageFromDockerTar(DOCKER_REPO_PATH, IMAGE_REPO, IMAGE_NAME, IMAGE_TAG, TIMEOUT_MINUTES));

        verify(processExecutor, times(1)).executeProcessBuilder(anyString(), anyInt());
    }

    @Test
    void deleteImageFromRegistry_shouldReturnNothing_whenProcessReturnZeroCode() throws CommandTimedOutException {
        // Mock configuration
        final int ZERO_EXIT_CODE = 0;
        ProcessExecutorResponse mockProcessResponse = new ProcessExecutorResponse();
        mockProcessResponse.setExitValue(ZERO_EXIT_CODE);

        when(processExecutor.executeProcessBuilder(anyString(), anyInt())).thenReturn(mockProcessResponse);

        // Test execution
        skopeoService.deleteImageFromRegistry(IMAGE_NAME, IMAGE_TAG);

        // Result assertion
        verify(processExecutor, times(1)).executeProcessBuilder(anyString(), anyInt());
    }

    @Test
    void deleteImageFromRegistry_shouldReturn503ServiceUnavailableExc_whenProcessReturnZeroCode() throws CommandTimedOutException {
        // Mock configuration
        final int ONE_EXIT_CODE = 1;
        ProcessExecutorResponse mockResponseWithError = new ProcessExecutorResponse();
        mockResponseWithError.setExitValue(ONE_EXIT_CODE);
        mockResponseWithError.setCmdErrorResult("time=\"2024-02-25T20:00:122\" level=fatal msg=\"trying to reuse blob "
                                                        + "sha256:acd64c6ce28b027133574b6a4581ee1d67b3de0e9f99acc2e72077f0e9be857 at destination:"
                                                        + " pinging container registry docker.zkulrus.haber002-vnfm11.ews.gic.ericsson.se:"
                                                        + " received unexpected HTTP status: 503 Service Unavailable");

        final int ZERO_EXIT_CODE = 0;
        ProcessExecutorResponse mockProcessResponse = new ProcessExecutorResponse();
        mockProcessResponse.setExitValue(ZERO_EXIT_CODE);

        when(processExecutor.executeProcessBuilder(anyString(), anyInt())).thenReturn(mockResponseWithError, mockProcessResponse);

        // Test execution
        skopeoService.deleteImageFromRegistry(IMAGE_NAME, IMAGE_TAG);

        // Result assertion
        verify(processExecutor, times(2)).executeProcessBuilder(anyString(), anyInt());
    }

    @Test
    void deleteImageFromRegistry_shouldThrowSkopeoException_whenProcessReturnNonZeroCode_andDoesNotContainManifestUnexistenceErrMessage() throws CommandTimedOutException {
        // Mock configuration
        final int NON_ZERO_EXIT_CODE = 1;
        ProcessExecutorResponse mockProcessResponse = mock(ProcessExecutorResponse.class);
        mockProcessResponse.setExitValue(NON_ZERO_EXIT_CODE);
        String errorMessage = "DUMMY ERROR MESSAGE";

        when(processExecutor.executeProcessBuilder(anyString(), anyInt())).thenReturn(mockProcessResponse);
        when(mockProcessResponse.getExitValue()).thenReturn(1);
        when(mockProcessResponse.getCmdErrorResult()).thenReturn(errorMessage);
        // Test execution
        assertThrows(SkopeoServiceException.class, () ->
                skopeoService.deleteImageFromRegistry(IMAGE_NAME, IMAGE_TAG));

        verify(processExecutor, times(1)).executeProcessBuilder(anyString(), anyInt());
    }

    @Test
    void deleteImageFromRegistry_shouldNotThrowAnyException_whenSkopeoCmdFailedWithManifestDoesNotExistMessage() throws CommandTimedOutException {
        // Mock configuration
        final int NON_ZERO_EXIT_CODE = 1;
        ProcessExecutorResponse mockProcessResponse = mock(ProcessExecutorResponse.class);
        mockProcessResponse.setExitValue(NON_ZERO_EXIT_CODE);
        String errorMessage = "time=\\\"2024-02-01T18:39:10Z\\\" level=fatal msg=\\\"Unable to delete docker.zokdfl.haber002-vnfm0.ews.gic.ericsson"
                + ".se/proj_policy/5g/docker_images/app/eric-ccpc-state-controller:1.6.0. Image may not exist or is not stored with a v2 Schema in a v2 registry";

        when(processExecutor.executeProcessBuilder(anyString(), anyInt())).thenReturn(mockProcessResponse);
        when(mockProcessResponse.getExitValue()).thenReturn(1);
        when(mockProcessResponse.getCmdErrorResult()).thenReturn(errorMessage);

        // Test execution
        skopeoService.deleteImageFromRegistry(IMAGE_NAME, IMAGE_TAG);

        verify(processExecutor, times(1)).executeProcessBuilder(anyString(), anyInt());
    }



    @Test
    void deleteImageFromRegistry_shouldThrowInternalRuntimeException_whenProcessExecutionThrowInternalRuntimeException() throws CommandTimedOutException {
        // Mock configuration
        InternalRuntimeException internalRuntimeException = new InternalRuntimeException("Error message");
        when(processExecutor.executeProcessBuilder(anyString(), anyInt())).thenThrow(internalRuntimeException);

        // Test execution
        try {
            skopeoService.deleteImageFromRegistry(IMAGE_NAME, IMAGE_TAG);
        } catch (Exception e) {
            assertEquals(InternalRuntimeException.class, e.getClass());
            assertEquals(internalRuntimeException.getMessage(), e.getMessage());
        }
        verify(processExecutor, times(1)).executeProcessBuilder(anyString(), anyInt());
    }

    @Test
    void deleteImageFromRegistry_shouldThrowInternalRuntimeException_whenProcessExecutionThrowsCommandTimeoutException() throws CommandTimedOutException {
        // Mock configuration
        when(processExecutor.executeProcessBuilder(anyString(), anyInt())).thenThrow(CommandTimedOutException.class);

        // Test execution
        assertThrows(InternalRuntimeException.class, () ->
                skopeoService.deleteImageFromRegistry(IMAGE_NAME, IMAGE_TAG));

        verify(processExecutor, times(1)).executeProcessBuilder(anyString(), anyInt());
    }

}