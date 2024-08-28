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
package com.ericsson.amonboardingservice.presentation.services.imageservice;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.ericsson.amonboardingservice.model.ImageResponse;
import com.ericsson.amonboardingservice.model.ImageResponseImages;
import com.ericsson.amonboardingservice.model.ImageResponseMetadata;
import com.ericsson.amonboardingservice.model.ImageResponseProjects;
import com.ericsson.amonboardingservice.presentation.exceptions.AuthorizationException;
import com.ericsson.amonboardingservice.presentation.exceptions.DataNotFoundException;
import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.utils.Constants;
import com.ericsson.amonboardingservice.utils.JsonUtils;
import com.ericsson.amonboardingservice.infrastructure.client.RestClient;
import com.google.common.base.Strings;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ImageService {
    @Autowired
    private RestClient restClient;

    @Value("${docker.registry.address}")
    private String containerRegistryHost;

    @Value("${docker.registry.user.name}")
    private String username;

    @Value("${docker.registry.user.password}")
    private String password;

    public ImageResponse getAllImages() {
        final String allCatalog = getCatalog();
        if (allCatalog == null) {
            LOGGER.error(Constants.INVALID_RESPONSE_ERROR_MESSAGE);
            throw new InternalRuntimeException(Constants.INVALID_RESPONSE_ERROR_MESSAGE);
        }
        final List<String> imageRepositories = JsonUtils.cast(JsonUtils.getJsonValue(allCatalog,
                                                                                     Constants.REPOSITORY_KEY, new ArrayList<>()));
        return createImageResponse(imageRepositories, null);
    }

    public ImageResponse getAllImagesByName(final String imageName) {
        final String allCatalog = getCatalog();
        if (allCatalog == null) {
            LOGGER.error(Constants.INVALID_RESPONSE_ERROR_MESSAGE);
            throw new InternalRuntimeException(Constants.INVALID_RESPONSE_ERROR_MESSAGE);
        }
        final List<String> imageRepositories = getRepositoriesForImage(imageName,
                                                                       JsonUtils.cast(JsonUtils.getJsonValue(allCatalog,
                                                                                                             Constants.REPOSITORY_KEY,
                                                                                                             new ArrayList<>())));
        if (CollectionUtils.isEmpty(imageRepositories)) {
            LOGGER.error(Constants.NO_DATA_FOUND_ERROR_MESSAGE);
            throw new DataNotFoundException(Constants.NO_DATA_FOUND_ERROR_MESSAGE);
        }
        return createImageResponse(imageRepositories, null);
    }

    public ImageResponse getAllImagesByNameAndTag(final String imageName, final String imageTag) {
        final String allHarborCatalog = getCatalog();
        if (allHarborCatalog == null) {
            throw new InternalRuntimeException(Constants.INVALID_RESPONSE_ERROR_MESSAGE);
        }
        final List<String> imageRepositories = getRepositoriesForImage(imageName,
                                                                       JsonUtils.cast(JsonUtils.getJsonValue(allHarborCatalog,
                                                                                                             Constants.REPOSITORY_KEY,
                                                                                                             new ArrayList<>())));
        LOGGER.debug("Repositories: {}", imageRepositories);
        if (CollectionUtils.isEmpty(imageRepositories)) {
            throw new DataNotFoundException(Constants.NO_DATA_FOUND_ERROR_MESSAGE);
        }
        return createImageResponse(imageRepositories, imageTag);
    }

    private static int getImageCount(List<ImageResponseProjects> allProject) {
        int count = 0;
        for (ImageResponseProjects project : allProject) {
            List<ImageResponseImages> images = project.getImages();
            count += images.size();
        }
        return count;
    }

    private ImageResponse createImageResponse(final List<String> repositories, final String imageTag) {
        ImageResponse imageResp = new ImageResponse();
        List<ImageResponseProjects> allProject = new ArrayList<>();
        for (String repository : repositories) {
            String imageTagsJson = getImageTags(repository);
            ImageResponseProjects project = createProjects(imageTagsJson);
            if (!Strings.isNullOrEmpty(imageTag)) {
                List<String> tags = project.getImages().get(0).getTags();
                if (tags.contains(imageTag)) {
                    allProject = createProjects(allProject, project);
                }
            } else {
                allProject = createProjects(allProject, project);
            }
        }
        if (allProject.isEmpty()) {
            LOGGER.error(Constants.NO_DATA_FOUND_ERROR_MESSAGE);
            throw new DataNotFoundException(Constants.NO_DATA_FOUND_ERROR_MESSAGE);
        }
        imageResp.setProjects(allProject);
        ImageResponseMetadata imageResponseMetadata = new ImageResponseMetadata();
        imageResponseMetadata.setCount(getImageCount(allProject));
        imageResp.setMetadata(imageResponseMetadata);
        return imageResp;
    }

    private static List<ImageResponseProjects> createProjects(List<ImageResponseProjects> allProject, ImageResponseProjects project) {
        if (projectExist(allProject, project)) {
            appendImageInExistingProject(allProject, project);
        } else {
            allProject.add(project);
        }
        return allProject;
    }

    private static List<String> getRepositoriesForImage(final String imageName, final List<String> repositories) {
        List<String> imageRepository = new ArrayList<>();
        if (repositories != null) {
            for (String repo : repositories) {
                String[] projectNameAndImageName = getProjectNameAndImageName(repo);
                if (imageName.equals(projectNameAndImageName[projectNameAndImageName.length - 1])) {
                    imageRepository.add(repo);
                }
            }
        } else {
            throw new DataNotFoundException(Constants.NO_DATA_FOUND_ERROR_MESSAGE);
        }
        return imageRepository;
    }

    private static String[] getProjectNameAndImageName(final String repository) {
        String[] projectNameAndImageName = new String[2];
        if (repository.contains(Constants.PROJECT_AND_IMAGE_NAME_SEPARATOR)) {
            int lastIndex = repository.lastIndexOf(Constants.PROJECT_AND_IMAGE_NAME_SEPARATOR);
            projectNameAndImageName[0] = repository.substring(0, lastIndex);
            projectNameAndImageName[1] = repository.substring(lastIndex + 1);
        } else {
            projectNameAndImageName[0] = "";
            projectNameAndImageName[1] = repository;
        }
        return projectNameAndImageName;
    }

    private static void appendImageInExistingProject(final List<ImageResponseProjects> allProject, final ImageResponseProjects project) {
        for (ImageResponseProjects tmpProject : allProject) {
            if (tmpProject.getName().equals(project.getName())) {
                List<ImageResponseImages> images = tmpProject.getImages();
                images.addAll(project.getImages());
                tmpProject.setImages(images);
                break;
            }
        }
    }

    private static boolean projectExist(final List<ImageResponseProjects> allProject, final ImageResponseProjects project) {
        for (ImageResponseProjects tmpProject : allProject) {
            if (tmpProject.getName().equals(project.getName())) {
                return true;
            }
        }
        return false;
    }

    private static ImageResponseProjects createProjects(final String imageJson) {
        ImageResponseProjects project = new ImageResponseProjects();
        if (!Strings.isNullOrEmpty(imageJson)) {
            LOGGER.debug("Images: {}", imageJson);
            ImageResponseImages image = createImage(imageJson);
            List<ImageResponseImages> images = new ArrayList<>();
            images.add(image);
            project.setImages(images);
            project.setName(image.getRepository().split("/")[0]);
        } else {
            throw new InternalRuntimeException(Constants.INVALID_RESPONSE_ERROR_MESSAGE);
        }
        return project;
    }

    private static ImageResponseImages createImage(final String imageJson) {
        String repository = JsonUtils.getJsonValue(imageJson, Constants.IMAGE_NAME_KEY, "");
        ImageResponseImages image = new ImageResponseImages();
        String[] projectNameAndImageName = getProjectNameAndImageName(repository);
        image.setName(projectNameAndImageName[1]);
        image.setTags(JsonUtils.cast(JsonUtils.getJsonValue(imageJson, Constants.IMAGE_TAG_KEY, new ArrayList<>())));
        image.setRepository(repository);
        return image;
    }

    private ResponseEntity<String> getResponseViaBasicAuthentication(final String requestUrl) {
        LOGGER.debug("Getting response via basic authentication for url {}", requestUrl);
        final String completeRequestUrl = String.format(requestUrl, containerRegistryHost);
        return restClient.get(completeRequestUrl, username, password);
    }

    /**
     * Execute REST for getting catalog
     *
     * @return String responseBody
     */
    public String getCatalog() {
        ResponseEntity<String> allCatalog = getResponseViaBasicAuthentication(String.format("https://%s/v2/_catalog",
                                                                                            containerRegistryHost));
        LOGGER.debug("Catalog response entity {}", allCatalog);
        if (allCatalog != null && allCatalog.getStatusCode().equals(HttpStatus.OK)) {
            return allCatalog.getBody();
        } else if (allCatalog != null && allCatalog.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
            throw new AuthorizationException(Constants.INVALID_CREDENTIALS_ERROR_MESSAGE);
        } else {
            throw new DataNotFoundException("Unable to find catalog");
        }
    }

    /**
     * Execute REST for getting status of Harbor Registry
     *
     * @return String health
     */
    public String healthStatus() {
        try {
            ResponseEntity<String> health = getResponseViaBasicAuthentication(String.format("https://%s/v2/",
                                                                                            containerRegistryHost));
            if (health.getStatusCode().equals(HttpStatus.OK) || health.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
                return "{health : true}";
            } else {
                return "{health : false}";
            }
        } catch (Exception e) {
            LOGGER.debug("healthStatus:", e);
            return "{health : false}";
        }
    }

    /**
     * Execute REST for getting image tags
     *
     * @param repository
     *
     * @return String responseBody
     */
    public String getImageTags(final String repository) {
        ResponseEntity<String> allImageTags = getResponseViaBasicAuthentication(String.format(
                "https://%s/v2/%s/tags/list", containerRegistryHost, repository));
        if (allImageTags != null && allImageTags.getStatusCode().equals(HttpStatus.OK)) {
            return allImageTags.getBody();
        } else if (allImageTags != null && allImageTags.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
            throw new AuthorizationException(Constants.INVALID_CREDENTIALS_ERROR_MESSAGE);
        } else {
            throw new DataNotFoundException("Unable to find image tags");
        }
    }
}
