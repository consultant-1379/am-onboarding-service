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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.ericsson.amonboardingservice.presentation.exceptions.CommandTimedOutException;
import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.presentation.exceptions.SkopeoServiceException;
import com.ericsson.amonboardingservice.utils.executor.ProcessExecutor;
import com.ericsson.amonboardingservice.utils.executor.ProcessExecutorResponse;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "skopeo", name = "enabled", havingValue = "true")
public class SkopeoServiceImpl implements SkopeoService {

    private static final String MANIFEST_DOES_NOT_EXIST_ERROR_MESSAGE = "Image may not exist or is not stored";

    @Autowired
    private ProcessExecutor processExecutor;

    @Autowired
    @Qualifier("retrySkopeoCommand")
    private RetrySkopeoCommand retrySkopeoCommand;

    @Value("${docker.registry.address}")
    private String privateDockerRegistryHost;

    @Value("${docker.registry.user.name}")
    private String registryUser;

    @Value("${docker.registry.user.password}")
    private String registryPassword;

    @Override
    public void pushImageFromDockerTar(String pathToDockerTar, String repoArtifactorySubPath, String imageName, String imageTag,
                                       int timeoutMinutes)  {
        String cmdPattern = "skopeo copy " +
                "--retry-times=5 " +
                "--dest-creds={registryUser}:{registryPass} " +
                "--dest-tls-verify=false " +
                "--preserve-digests " +
                "--all " +
                "docker-archive:{pathToDockerTar}:{imageRepoName}/{imageName}:{imageTag} " +
                "docker://{dockerRegistryHost}/{imageName}:{imageTag}";

        String cmd = prepareCmd(cmdPattern, pathToDockerTar, repoArtifactorySubPath, imageName, imageTag);

        ProcessExecutorResponse processExecutorResponse = retrySkopeoCommand.retry(() -> {
            try {
                return processExecutor.executeProcessBuilder(cmd, timeoutMinutes);
            } catch (CommandTimedOutException e) {
                throw new InternalRuntimeException(e);
            }
        });

        if (processExecutorResponse.getExitValue() != 0) {
            throw new SkopeoServiceException(
                    String.format("The image %s:%s has not been pushed successfully due to %s",
                                  imageName, imageTag, processExecutorResponse.getCmdErrorResult()
                    )
            );
        }
    }

    @Override
    public void deleteImageFromRegistry(String imageName, String imageTag) {
        final int deleteTimeoutMinutes = 1;

        String cmdPattern = "skopeo delete " +
                "--retry-times=5 " +
                "--creds={registryUser}:{registryPass} "  +
                "--tls-verify=false "  +
                "docker://{dockerRegistryHost}/{imageName}:{imageTag}";
        String cmd = prepareCmd(cmdPattern, imageName, imageTag);

        ProcessExecutorResponse processExecutorResponse = retrySkopeoCommand.retry(() -> {
            try {
                return processExecutor.executeProcessBuilder(cmd, deleteTimeoutMinutes);
            } catch (CommandTimedOutException e) {
                throw new InternalRuntimeException(e);
            }
        });

        if (processExecutorResponse.getExitValue() != 0) {
            if (processExecutorResponse.getCmdErrorResult().contains(MANIFEST_DOES_NOT_EXIST_ERROR_MESSAGE)) {
                LOGGER.info("Image {}:{} had already been deleted or did not exist", imageName, imageTag);
            } else {
                throw new SkopeoServiceException(
                        String.format("The image %s:%s has not been deleted successfully due to %s",
                                      imageName, imageTag, processExecutorResponse.getCmdErrorResult()
                        )
                );
            }
        }
    }

    private String prepareCmd(String cmdTemplate, String imageName, String imageTag) {
        return cmdTemplate
                .replace("{imageName}", imageName)
                .replace("{imageTag}", imageTag)
                .replace("{dockerRegistryHost}", privateDockerRegistryHost)
                .replace("{registryUser}", registryUser)
                .replace("{registryPass}", registryPassword);
    }

    private String prepareCmd(String cmdTemplate, String pathToDockerTar, String imageRepoName, String imageName, String imageTag) {
        String modifiedCmd;
        if (imageRepoName.isEmpty()) {
            modifiedCmd = cmdTemplate.replace("{imageRepoName}/", "");
        } else {
            modifiedCmd = cmdTemplate.replace("{imageRepoName}", imageRepoName);
        }

        return modifiedCmd
                .replace("{pathToDockerTar}", pathToDockerTar)
                .replace("{imageName}", imageName)
                .replace("{imageTag}", imageTag)
                .replace("{dockerRegistryHost}", privateDockerRegistryHost)
                .replace("{registryUser}", registryUser)
                .replace("{registryPass}", registryPassword);
    }

}
