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

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.model.ImageResponse;
import com.ericsson.amonboardingservice.presentation.exceptions.AuthorizationException;
import com.ericsson.amonboardingservice.presentation.exceptions.DataNotFoundException;
import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.utils.JsonUtils;
import com.ericsson.amonboardingservice.infrastructure.client.RestClient;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest()
@ActiveProfiles("test")
public class ImageServiceTest extends AbstractDbSetupTest {

    @Value("${docker.registry.address}")
    private String containerRegistryHost;

    @Value("${docker.registry.user.name}")
    private String username;

    @Value("${docker.registry.user.password}")
    private String password;

    private static final String DUMMY_REPOSITORY = "dummy_repository";

    @Autowired
    @InjectMocks
    private ImageService imageService;

    @MockBean
    private RestClient restClient;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RetryTemplate retryTemplate;

    String repository;
    String imageName;
    String project;
    List<String> imageTags;
    String catalogResp;
    String nginxImageResp;
    String nginxStableImageResp;
    String tokenResp;
    String testImageResp;

    @BeforeEach
    public void setUp() throws IOException {
        catalogResp = Resources.toString(Resources.getResource("get_catalog_response.json"), StandardCharsets.UTF_8);
        nginxImageResp = Resources.toString(Resources.getResource("get_image_response_nginx.json"), StandardCharsets.UTF_8);
        nginxStableImageResp = Resources.toString(Resources.getResource("get_image_response_nginx_stable.json"), StandardCharsets.UTF_8);
        testImageResp = Resources.toString(Resources.getResource("get_image_response_test.json"), StandardCharsets.UTF_8);
        repository = (String) ((List) JsonUtils.getJsonValue(catalogResp, "repositories", new ArrayList<>())).get(0);
        String[] temp = repository.split("/");
        imageName = temp[temp.length - 1];
        project = temp[0];
        imageTags = JsonUtils.cast(JsonUtils.getJsonValue(nginxImageResp, "tags", new ArrayList<>()));
    }

    @Test
    public void testSuccessGetHarborCatalog() {
        ResponseEntity<String> catalogResponseEntity = new ResponseEntity<>(catalogResp, HttpStatus.OK);
        when(restClient.get(anyString(), anyString(), anyString())).
                thenReturn(catalogResponseEntity);
        assertThat(imageService.getCatalog()).isEqualTo(catalogResp);
    }

    @Test
    public void testInvalidResponseForGetHarborCatalog() {
        when(restClient.get(anyString(), anyString(), anyString())).
                thenReturn(null);
        assertThrows(DataNotFoundException.class, () -> imageService.getCatalog());
    }

    @Test
    public void testUnAuthorizeForGetHarborCatalog() {
        ResponseEntity<String> catalogResponseEntity = new ResponseEntity<>("test", HttpStatus.UNAUTHORIZED);
        when(restClient.get(anyString(), anyString(), anyString())).
                thenReturn(catalogResponseEntity);
        assertThrows(AuthorizationException.class, () -> imageService.getCatalog());
    }

    @Test
    public void testSuccessGetImageTags() {
        ResponseEntity<String> imageTagResponseEntity = new ResponseEntity<>(catalogResp, HttpStatus.OK);
        when(restClient.get(anyString(), anyString(), anyString())).
                thenReturn(imageTagResponseEntity);
        assertThat(imageService.getImageTags(DUMMY_REPOSITORY))
                .isEqualTo(catalogResp);
    }

    @Test
    public void testInvalidResponseForGetImageTags() {
        when(restClient.get(anyString(), anyString(), anyString())).
                thenReturn(null);

        assertThrows(DataNotFoundException.class, () -> imageService.getImageTags(DUMMY_REPOSITORY));

    }

    @Test
    public void testUnAuthorizeForGetImageTags() {
        ResponseEntity<String> imageTagResponseEntity = new ResponseEntity<>("test", HttpStatus.UNAUTHORIZED);
        when(restClient.get(anyString(), anyString(), anyString())).
                thenReturn(imageTagResponseEntity);
        assertThrows(AuthorizationException.class, () -> imageService.getImageTags(DUMMY_REPOSITORY));
    }

    @Test
    public void testGetAllImages() {
        when(restClient.get(anyString(),
                            anyString(), anyString())).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                if (count == 0) {
                    count++;
                    return new ResponseEntity<>(catalogResp, HttpStatus.OK);
                } else if (count == 1) {
                    count++;
                    return new ResponseEntity<>(nginxImageResp, HttpStatus.OK);
                } else if (count == 2) {
                    count++;
                    return new ResponseEntity<>(testImageResp, HttpStatus.OK);
                } else {
                    return "Error Response";
                }
            }
        });
        ImageResponse imageResponse = imageService.getAllImages();
        assertThat(imageResponse.getProjects().get(0).getName()).isEqualTo(project);
        assertThat(imageResponse.getProjects().get(0).getImages().get(0).getName()).isEqualTo(imageName);
        assertThat(imageResponse.getProjects().get(0).getImages().get(0).getRepository()).isEqualTo(repository);
        assertThat(imageResponse.getProjects().get(0).getImages().get(0).getTags()).isEqualTo(imageTags);
        assertThat(imageResponse.getProjects().size()).isEqualTo(1);
        assertThat(imageResponse.getProjects().get(0).getImages().size()).isEqualTo(2);
        assertThat(imageResponse.getMetadata().getCount()).isEqualTo(2);
    }

    @Test
    public void testGetAllImagesWithInvalidCatalogResp() {
        when(restClient.get(anyString(),
                            anyString(), anyString())).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                if (count == 0) {
                    count++;
                    return new ResponseEntity<>(null, HttpStatus.OK);
                } else {
                    return "Error Response";
                }
            }
        });
        assertThrows(InternalRuntimeException.class, () -> imageService.getAllImages());
    }

    @Test
    public void testGetAllImagesWithInvalidImageTagRep() {
        when(restClient.get(anyString(),
                            anyString(), anyString())).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                if (count == 0) {
                    count++;
                    return new ResponseEntity<>(catalogResp, HttpStatus.OK);
                } else if (count == 1) {
                    count++;
                    return new ResponseEntity<>("", HttpStatus.OK);
                } else {
                    return "Error Response";
                }
            }
        });
        assertThrows(InternalRuntimeException.class, () -> imageService.getAllImages());
    }

    @Test
    public void testGetImageByName() {
        when(restClient.get(anyString(),
                            anyString(), anyString())).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                if (count == 0) {
                    count++;
                    return new ResponseEntity<>(catalogResp, HttpStatus.OK);
                } else if (count == 1) {
                    count++;
                    return new ResponseEntity<>(nginxImageResp, HttpStatus.OK);
                } else {
                    return "Error Response";
                }
            }
        });
        ImageResponse imageResponse = imageService.getAllImagesByName(imageName);
        assertThat(imageResponse.getProjects().get(0).getName()).isEqualTo(project);
        assertThat(imageResponse.getProjects().size()).isEqualTo(1);
        assertThat(imageResponse.getProjects().get(0).getImages().size()).isEqualTo(1);
        assertThat(imageResponse.getProjects().get(0).getImages().get(0).getName()).isEqualTo(imageName);
        assertThat(imageResponse.getProjects().get(0).getImages().get(0).getRepository()).isEqualTo(repository);
        assertThat(imageResponse.getProjects().get(0).getImages().get(0).getTags()).isEqualTo(imageTags);
        assertThat(imageResponse.getMetadata().getCount()).isEqualTo(1);
    }

    @Test
    public void testGetImageByNameNotPresent() {
        when(restClient.get(anyString(),
                            anyString(), anyString())).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                if (count == 0) {
                    count++;
                    return new ResponseEntity<>(catalogResp, HttpStatus.OK);
                } else if (count == 1) {
                    count++;
                    return new ResponseEntity<>(nginxImageResp, HttpStatus.OK);
                } else {
                    return "Error Response";
                }
            }
        });
        assertThrows(DataNotFoundException.class, () -> imageService.getAllImagesByName("dummy"));
    }

    @Test
    public void testGetImageByNameAndTag() {
        when(restClient.get(anyString(),
                            anyString(), anyString())).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                if (count == 0) {
                    count++;
                    return new ResponseEntity<>(catalogResp, HttpStatus.OK);
                } else if (count == 1) {
                    count++;
                    return new ResponseEntity<>(nginxImageResp, HttpStatus.OK);
                } else {
                    return "Error Response";
                }
            }
        });
        ImageResponse imageResponse = imageService.getAllImagesByNameAndTag(imageName, imageTags.get(0));
        assertThat(imageResponse.getProjects().get(0).getName()).isEqualTo(project);
        assertThat(imageResponse.getProjects().size()).isEqualTo(1);
        assertThat(imageResponse.getProjects().get(0).getImages().get(0).getName()).isEqualTo(imageName);
        assertThat(imageResponse.getProjects().get(0).getImages().get(0).getRepository()).isEqualTo(repository);
        assertThat(imageResponse.getProjects().get(0).getImages().get(0).getTags()).isEqualTo(imageTags);
        assertThat(imageResponse.getMetadata().getCount()).isEqualTo(1);
    }

    @Test
    public void testGetImageByNameWhereThereIsANullTag() {
        when(restClient.get(anyString(),
                            anyString(), anyString())).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) throws IOException {
                if (count == 0) {
                    count++;
                    return new ResponseEntity<>(Resources.toString(Resources.getResource(
                                                                           "get_catalog_response_null_tag"
                                                                                   + ".json"),
                                                                   StandardCharsets.UTF_8), HttpStatus.OK);
                } else if (count == 1) {
                    count++;
                    return new ResponseEntity<>(nginxStableImageResp, HttpStatus.OK);
                } else {
                    return "Error";
                }
            }
        });
        ImageResponse imageResponse = imageService.getAllImagesByName("nginx");
        assertThat(imageResponse.getProjects().get(0).getImages().get(0).getTags()).isEmpty();
    }

    @Test
    public void testGetImageByNameAndTagNotPresent() {
        ResponseEntity<String> tokenResponseEntity = new ResponseEntity<>(tokenResp, HttpStatus.OK);
        when(restClient.get(anyString(),
                            anyString(), anyString())).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                if (count == 0) {
                    count++;
                    return new ResponseEntity<>(catalogResp, HttpStatus.OK);
                } else if (count == 1) {
                    count++;
                    return new ResponseEntity<>(nginxImageResp, HttpStatus.OK);
                } else {
                    return "Error Response";
                }
            }
        });
        assertThrows(DataNotFoundException.class, () -> imageService.getAllImagesByNameAndTag(imageName, "dummy"));
    }

    @Test
    public void testGetImageByNameAndTagNameNotPresent() {
        when(restClient.get(anyString(),
                            anyString(), anyString())).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                if (count == 0) {
                    count++;
                    return new ResponseEntity<>(catalogResp, HttpStatus.OK);
                } else if (count == 1) {
                    count++;
                    return new ResponseEntity<>(nginxImageResp, HttpStatus.OK);
                } else {
                    return "Error Response";
                }
            }
        });
        assertThrows(DataNotFoundException.class, () -> imageService.getAllImagesByNameAndTag("dummy", "dummy"));


    }
}
