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
package com.ericsson.amonboardingservice.presentation.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static com.ericsson.amonboardingservice.TestUtils.getInMemoryAppender;
import static com.ericsson.amonboardingservice.presentation.models.license.Permission.CLUSTER_MANAGEMENT;
import static com.ericsson.amonboardingservice.utils.Utility.checkResponseMessage;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.Yaml;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.logging.InMemoryAppender;
import com.ericsson.amonboardingservice.model.AppUsageStateRequest;
import com.ericsson.amonboardingservice.model.CreateVnfPkgInfoRequest;
import com.ericsson.amonboardingservice.model.ProblemDetails;
import com.ericsson.amonboardingservice.model.VnfPackageArtifactInfo;
import com.ericsson.amonboardingservice.model.VnfPkgInfo;
import com.ericsson.amonboardingservice.presentation.models.license.Permission;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageService;
import com.ericsson.amonboardingservice.utils.Constants;
import com.ericsson.amonboardingservice.utils.UrlUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Level;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestPropertySource(properties = {"url=",
        "onboarding.logConcurrentRequests=true"})
public class VnfPackageControllerTest extends AbstractDbSetupTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppPackageRepository repository;

    @Autowired
    private FileService fileService;

    @Autowired
    private PackageService packageService;

    private MvcResult mvcResult;

    private final ObjectMapper objectMapper = new ObjectMapper();

    {
        objectMapper.registerModule(new JsonNullableModule());
    }

    private static final String UPDATE_USAGE_STATE_API = "/api/vnfpkgm/v1/vnf_packages/%s/update_usage_state";
    private static final String VNF_PACKAGE_API = "/api/vnfpkgm/v1/vnf_packages";
    private static final String VNF_HELMFILE_API = VNF_PACKAGE_API + "/%s/helmfile";
    private static final String VNF_PACKAGE_ID = "b3def1ce-4cf4-477c-aab3-21cb04e6a380";
    private static final String VNF_PACKAGE_ID_DELETE = "f3def1ce-4cf4-477c-aab3-21cb04e6a382";
    private static final String VNF_PACKAGE_ID_DELETE_IN_USE = "f3def1ce-4cf4-477c-aab3-21cb04e6a384";
    private static final String VNF_PACKAGE_ID_DELETE_PACKAGE_ONLY = "f3def1ce-4cf4-477c-aab3-21cb04e6a381";
    private static final String VNF_PACKAGE_ID_WITH_ERROR_DETAILS = "c2def1ce-4cf4-477c-aab3-21cb04e6a380";
    private static final String ERROR_PACKAGE_NOT_FOUND = "{\"type\":\"about:blank\",\"title\":\"Package not found\"," +
            "\"status\":404,\"detail\":\"Package with id: non-existent-package-id not found\",\"instance\":\"about:blank\"}";
    private static final String ERROR_INSTANCE_ID_EMPTY = "{\"type\":\"about:blank\",\"title\":" +
            "\"Mandatory parameter missing\",\"status\":422,\"detail\":\"vnfId size must be"
            + " " +
            "between 1 and 2147483647\",\"instance\":\"about:blank\"}";
    private static final String ERROR_INSTANCE_ID_NULL = "{\"type\":\"about:blank\",\"title\":" +
            "\"Mandatory parameter missing\",\"status\":422,\"detail\":\"vnfId must not be"
            + " null\"," +
            "\"instance\":\"about:blank\"}";
    private static final String VNF_PACKAGE_ERROR_DETAILS = "Onboarding of package c2def1ce-4cf4-477c-aab3-21cb04e6a380 failed due to "
            + "overloaded Onboarding service.";
    public static final String VNFD_ZIP = "vnfd.zip";

    @Test
    @Transactional
    public void shouldReturnListOfVnfPackages() throws Exception {
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get(VNF_PACKAGE_API).accept(MediaType.APPLICATION_JSON);
        mvcResult = mockMvc
                .perform(requestBuilder)
                .andExpect(header().doesNotExist(UrlUtils.PAGINATION_INFO))
                .andExpect(header().doesNotExist(HttpHeaders.LINK))
                .andExpect(status().isOk())
                .andReturn();
        List<VnfPkgInfo> infos = objectMapper.readValue(
                mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertThat(infos).isNotEmpty();
    }

    @Test
    @Transactional
    public void shouldReturnVnfPackagesPage() throws Exception {
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get(VNF_PACKAGE_API + "?size=1")
                .accept(MediaType.APPLICATION_JSON);
        mvcResult = mockMvc
                .perform(requestBuilder)
                .andExpect(header().exists(UrlUtils.PAGINATION_INFO))
                .andExpect(header().exists(HttpHeaders.LINK))
                .andExpect(status().isOk())
                .andReturn();
        List<VnfPkgInfo> infos = objectMapper.readValue(
                mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertThat(infos).isNotEmpty();
    }

    @Test
    @Transactional
    public void shouldReturnBadRequestWhenInvalidPageNumber() throws Exception {
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get(VNF_PACKAGE_API + "?nextpage_opaque_marker=100")
                .accept(MediaType.APPLICATION_JSON);

        String content = mockMvc
                .perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        ProblemDetails problemDetails = objectMapper.readValue(content, ProblemDetails.class);
        assertThat(problemDetails.getType().toString()).isEqualTo(Constants.TYPE_BLANK);
        assertThat(problemDetails.getStatus()).isEqualTo(400);
        assertThat(problemDetails.getTitle()).isEqualTo("Invalid Pagination Query Parameter Exception");
        assertThat(problemDetails.getDetail()).isEqualTo("Requested page number exceeds the total number " +
                "of pages. Requested page: 100. Total pages: 2");
    }

    @Test
    public void checkGetNonExistentPackage() throws Exception {
        final InMemoryAppender inMemoryAppender = getInMemoryAppender();

        mvcResult = getMvcResult(VNF_PACKAGE_API + "/1234");
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(404);
        checkResponseMessage(mvcResult, "Vnf package with id 1234 does not exist.");

        assertThat(inMemoryAppender.contains("Entering operation. Number of concurrent requests: %s. Async operations: %s"
                .formatted(1, 0), Level.INFO)).isTrue();
        assertThat(inMemoryAppender.contains("Exiting operation. Number of concurrent requests: %s. Async operations: %s"
                .formatted(0, 0), Level.INFO)).isTrue();

    }

    @Test
    public void shouldReturnSuccessForGetExistentPackage() throws Exception {
        mvcResult = getMvcResult(VNF_PACKAGE_API + "/" + VNF_PACKAGE_ID);
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(200);
        VnfPkgInfo vnfPkgInfo = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), VnfPkgInfo.class);
        assertThat(vnfPkgInfo.getId()).isEqualTo(VNF_PACKAGE_ID);
        assertThat(vnfPkgInfo.getOnboardingState()).isEqualTo(VnfPkgInfo.OnboardingStateEnum.CREATED);
    }

    @Test
    public void shouldReturnSuccessForGetExistentPackageWithArtifacts() throws Exception {
        String id = "f3def1ce-4cf4-477c-123";
        mvcResult = getMvcResult(VNF_PACKAGE_API + "/" + id);
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(200);
        VnfPkgInfo vnfPkgInfo = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), VnfPkgInfo.class);
        assertThat(vnfPkgInfo.getId()).isEqualTo(id);
        assertThat(vnfPkgInfo.getAdditionalArtifacts()).isNotNull();
        Optional<VnfPackageArtifactInfo> vnfPackageArtifactInfo = vnfPkgInfo.getAdditionalArtifacts().stream().findFirst();
        assertThat(vnfPackageArtifactInfo.isPresent()).isTrue();
        assertThat(vnfPackageArtifactInfo.get().getArtifactPath())
                .isEqualTo("Definitions/OtherTemplates/sample-vnf-0.1.2.tgz");
        assertThat(vnfPackageArtifactInfo.get().getMetadata()).isNull();
        assertThat(vnfPackageArtifactInfo.get().getChecksum()).isNull();
        assertThat(vnfPkgInfo.getOnboardingState()).isEqualTo(VnfPkgInfo.OnboardingStateEnum.ONBOARDED);
    }

    @Test
    public void shouldReturnSuccessForGetExistentPackageWithOutArtifacts() throws Exception {
        String id = "57c24601-e70d-418d-8477-64fb58a7563c";
        mvcResult = getMvcResult(VNF_PACKAGE_API + "/" + id);
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(200);
        VnfPkgInfo vnfPkgInfo = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), VnfPkgInfo.class);
        assertThat(vnfPkgInfo.getId()).isEqualTo(id);
        assertThat(vnfPkgInfo.getAdditionalArtifacts()).isNotNull();
        Optional<VnfPackageArtifactInfo> vnfPackageArtifactInfo = vnfPkgInfo.getAdditionalArtifacts().stream().findFirst();
        assertThat(vnfPackageArtifactInfo.isPresent()).isFalse();
        assertThat(vnfPkgInfo.getOnboardingState()).isEqualTo(VnfPkgInfo.OnboardingStateEnum.ONBOARDED);
    }

    @Test
    public void shouldReturnProblemDetailsForPackageWithError() throws Exception {
        mvcResult = getMvcResult(VNF_PACKAGE_API + "/" + VNF_PACKAGE_ID_WITH_ERROR_DETAILS);
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(400);
        ProblemDetails problemDetails = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getType().toString()).isEqualTo(Constants.TYPE_BLANK);
        assertThat(problemDetails.getStatus()).isEqualTo(400);
        assertThat(problemDetails.getTitle()).isEqualTo("Onboarding Failed");
        assertThat(problemDetails.getDetail()).isEqualTo(VNF_PACKAGE_ERROR_DETAILS);
    }

    @Test
    public void shouldReturnSuccessForUpdateUsageStateOnSave() throws Exception {
        AppUsageStateRequest appUsageStateRequest = new AppUsageStateRequest();
        appUsageStateRequest.setVnfId("a1def1ce-4cf4-477c-aab3-21cb04e6a379");
        appUsageStateRequest.setIsInUse(true);
        MvcResult mvcResult = putMvcResult(String.format(UPDATE_USAGE_STATE_API, "b3def1ce-4cf4-477c-aab3-21cb04e6a380"),
                objectMapper.writeValueAsString(appUsageStateRequest));
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    public void shouldReturnSuccessForUpdateUsageStateOnUpgrade() throws Exception {
        AppUsageStateRequest appUsageStateRequest = new AppUsageStateRequest();
        appUsageStateRequest.setVnfId("a1def1ce-4cf4-477c-aab3-21cb04e6a379");
        appUsageStateRequest.setIsInUse(true);
        MvcResult mvcResult = putMvcResult(String.format(UPDATE_USAGE_STATE_API, "e3def1ce-4cf4-477c-aab3-21cb04e6a379"),
                objectMapper.writeValueAsString(appUsageStateRequest));
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    public void updatePackageWithNoOnboardingLicensePermissionFail() throws Exception {
        when(licenseConsumerService.getPermissions()).thenReturn(EnumSet.of(CLUSTER_MANAGEMENT));

        AppUsageStateRequest appUsageStateRequest = new AppUsageStateRequest();
        appUsageStateRequest.setVnfId("a1def1ce-4cf4-477c-aab3-21cb04e6a379");
        appUsageStateRequest.setIsInUse(true);
        MvcResult mvcResult = putMvcResult(String.format(UPDATE_USAGE_STATE_API, "e3def1ce-4cf4-477c-aab3-21cb04e6a379"),
                objectMapper.writeValueAsString(appUsageStateRequest));

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(405);
        assertThat(mvcResult.getResponse().getContentAsString()).isNotBlank();
        assertThat(mvcResult.getResponse().getContentAsString()).contains(Constants.ILLEGAL_NUMBER_OF_RESOURCES_ERROR_MESSAGE);
    }

    @Test
    public void updatePackageWithoutLicensePermissionFail() throws Exception {
        when(licenseConsumerService.getPermissions()).thenReturn(EnumSet.noneOf(Permission.class));
        AppUsageStateRequest appUsageStateRequest = new AppUsageStateRequest();
        appUsageStateRequest.setVnfId("a1def1ce-4cf4-477c-aab3-21cb04e6a379");
        appUsageStateRequest.setIsInUse(true);
        MvcResult mvcResult = putMvcResult(String.format(UPDATE_USAGE_STATE_API, "e3def1ce-4cf4-477c-aab3-21cb04e6a379"),
                objectMapper.writeValueAsString(appUsageStateRequest));

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(405);
        assertThat(mvcResult.getResponse().getContentAsString()).isNotBlank();
        assertThat(mvcResult.getResponse().getContentAsString()).contains(Constants.ILLEGAL_NUMBER_OF_RESOURCES_ERROR_MESSAGE);
    }

    @Test
    public void shouldErrorForNonExistentPackageId() throws Exception {
        AppUsageStateRequest appUsageStateRequest = new AppUsageStateRequest();
        appUsageStateRequest.setVnfId("a1def1ce-4cf4-477c-aab3-21cb04e6a379");
        appUsageStateRequest.setIsInUse(true);
        MvcResult mvcResult = putMvcResult(String.format(UPDATE_USAGE_STATE_API, "non-existent-package-id"),
                objectMapper.writeValueAsString(appUsageStateRequest));
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(404);
        assertThat(mvcResult.getResponse().getContentAsString()).isEqualTo(ERROR_PACKAGE_NOT_FOUND);
    }

    @Test
    public void shouldReturnErrorForEmptyInstanceId() throws Exception {
        AppUsageStateRequest appUsageStateRequest = new AppUsageStateRequest();
        appUsageStateRequest.setVnfId("");
        appUsageStateRequest.setIsInUse(true);
        MvcResult mvcResult = putMvcResult(String.format(UPDATE_USAGE_STATE_API, "d3def1ce-4cf4-477c-aab3-21cb04e6a379"),
                objectMapper.writeValueAsString(appUsageStateRequest));
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(422);
        assertThat(mvcResult.getResponse().getContentAsString()).isEqualTo(ERROR_INSTANCE_ID_EMPTY);
    }

    @Test
    public void shouldReturnErrorForNullInstanceId() throws Exception {
        AppUsageStateRequest appUsageStateRequest = new AppUsageStateRequest();
        appUsageStateRequest.setVnfId(null);
        appUsageStateRequest.setIsInUse(true);
        MvcResult mvcResult = putMvcResult(String.format(UPDATE_USAGE_STATE_API, "d3def1ce-4cf4-477c-aab3-21cb04e6a379"),
                objectMapper.writeValueAsString(appUsageStateRequest));
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(422);
        assertThat(mvcResult.getResponse().getContentAsString()).isEqualTo(ERROR_INSTANCE_ID_NULL);
    }

    @Test
    public void shouldCreateVnfPackageWithUserDefinedData() throws Exception {
        CreateVnfPkgInfoRequest createVnfPkgInfoRequest = new CreateVnfPkgInfoRequest();
        Map<String, String> userDefinedData = new HashMap<>();
        userDefinedData.put("testKey", "testVal");
        createVnfPkgInfoRequest.setUserDefinedData(userDefinedData);

        MvcResult mvcResult = postMvcResult(VNF_PACKAGE_API, objectMapper.writeValueAsString(createVnfPkgInfoRequest));
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(201);
        VnfPkgInfo vnfPkgInfo = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), VnfPkgInfo.class);

        assertThat(vnfPkgInfo.getId()).isNotNull();
        assertThat(vnfPkgInfo.getOnboardingState()).isEqualTo(VnfPkgInfo.OnboardingStateEnum.CREATED);
        assertThat(vnfPkgInfo.getOperationalState()).isEqualTo(VnfPkgInfo.OperationalStateEnum.DISABLED);
        assertThat(vnfPkgInfo.getUsageState()).isEqualTo(VnfPkgInfo.UsageStateEnum.NOT_IN_USE);
        assertThat(vnfPkgInfo.getUserDefinedData().toString()).isEqualTo("{testKey=testVal}");
        assertThat(vnfPkgInfo.getLinks().getSelf()).isNotNull();
        // Put the database back into the state it was before, depending on execution order other tests were failing.
        MvcResult result = deleteMvcResult(VNF_PACKAGE_API + "/" + vnfPkgInfo.getId());
    }

    @Test
    public void shouldCreateVnfPackageWithoutUserDefinedData() throws Exception {
        MvcResult mvcResult = createVnfPackage();
        VnfPkgInfo vnfPkgInfo = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), VnfPkgInfo.class);

        assertThat(vnfPkgInfo.getId()).isNotNull();
        assertThat(vnfPkgInfo.getOnboardingState()).isEqualTo(VnfPkgInfo.OnboardingStateEnum.CREATED);
        assertThat(vnfPkgInfo.getOperationalState()).isEqualTo(VnfPkgInfo.OperationalStateEnum.DISABLED);
        assertThat(vnfPkgInfo.getUsageState()).isEqualTo(VnfPkgInfo.UsageStateEnum.NOT_IN_USE);
        assertThat(vnfPkgInfo.getLinks().getSelf()).isNotNull();
        // Put the database back into the state it was before, depending on execution order other tests were failing.
        MvcResult result = deleteMvcResult(VNF_PACKAGE_API + "/" + vnfPkgInfo.getId());
    }

    @Test
    public void createPackageWithNoOnboardingLicensePermissionFail() throws Exception {
        when(licenseConsumerService.getPermissions()).thenReturn(EnumSet.of(CLUSTER_MANAGEMENT));
        CreateVnfPkgInfoRequest createVnfPkgInfoRequest = new CreateVnfPkgInfoRequest();

        MvcResult mvcResult = postMvcResult(VNF_PACKAGE_API, objectMapper.writeValueAsString(createVnfPkgInfoRequest));
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(405);
        assertThat(mvcResult.getResponse().getContentAsString()).isNotBlank();
        assertThat(mvcResult.getResponse().getContentAsString()).contains(Constants.ILLEGAL_NUMBER_OF_RESOURCES_ERROR_MESSAGE);
    }

    @Test
    public void createPackageWithoutLicensePermissionFail() throws Exception {
        when(licenseConsumerService.getPermissions()).thenReturn(EnumSet.noneOf(Permission.class));
        CreateVnfPkgInfoRequest createVnfPkgInfoRequest = new CreateVnfPkgInfoRequest();

        MvcResult mvcResult = postMvcResult(VNF_PACKAGE_API, objectMapper.writeValueAsString(createVnfPkgInfoRequest));
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(405);
        assertThat(mvcResult.getResponse().getContentAsString()).isNotBlank();
        assertThat(mvcResult.getResponse().getContentAsString()).contains(Constants.ILLEGAL_NUMBER_OF_RESOURCES_ERROR_MESSAGE);
    }

    @Test
    public void shouldGetVnfd() throws Exception {
        String packageId = "c2def1ce-4cf4-477c-aab3-21cb04e6a386";
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.get(VNF_PACKAGE_API +
                "/" + packageId + "/vnfd").accept(MediaType.TEXT_PLAIN);
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(200);
        assertThat(mvcResult.getResponse().getContentType()).isNotNull().isEqualTo("text/plain");
        assertThat(mvcResult.getResponse().getContentAsString()).isNotBlank();
        checkIfVnfdContentIsCorrect(packageId, mvcResult.getResponse().getContentAsByteArray());
    }

    @Test
    public void shouldGetVnfdInYamlFormatUsingZipFile() throws Exception {
        String packageId = "c2def1ce-4cf4-477c";
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.get(VNF_PACKAGE_API +
                "/" + packageId + "/vnfd").accept(MediaType.TEXT_PLAIN);
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(200);
        assertThat(mvcResult.getResponse().getContentType()).isNotNull().isEqualTo("text/plain");
        assertThat(mvcResult.getResponse().getContentAsString()).isNotBlank();
        checkIfVnfdContentIsCorrect(packageId, mvcResult.getResponse().getContentAsByteArray());
    }

    @Test
    public void shouldGetVnfdForZipFormat() throws Exception {
        String packageId = "c2def1ce-4cf4-477c-aab3-21cb04e6a386";
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.get(VNF_PACKAGE_API
                + "/" + packageId + "/vnfd").accept("application/zip");
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(200);
        assertThat(mvcResult.getResponse().getContentType()).isNotNull().isEqualTo("application/zip");
        assertThat(mvcResult.getResponse().getContentAsString()).isNotBlank();
        String directoryName = null;
        Path directory = null;
        try {
            directoryName = UUID.randomUUID().toString();
            directory = fileService.createDirectory(directoryName);
            Path pathToPackage = fileService.storeFile(new ByteArrayInputStream(mvcResult.getResponse()
                    .getContentAsByteArray()), directory, VNFD_ZIP);
            fileService.unpack(pathToPackage, 15);
            Path vnfdPath = directory.resolve("cnf_vnfd.yaml");
            checkIfVnfdContentIsCorrect(packageId, Files.readAllBytes(vnfdPath));
        } finally {
            if (directory != null) {
                fileService.deleteDirectory(directoryName);
            }
        }
    }

    @Test
    public void shouldGetVnfdForZipFormatWithAcceptHeaderPlainText() throws Exception {
        String packageId = "c2def1ce-4cf4-477c-aab3-21cb0";
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.get(VNF_PACKAGE_API
                + "/" + packageId + "/vnfd").accept("text/plain");
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(200);
        assertThat(mvcResult.getResponse().getContentType()).isNotNull().isEqualTo("application/zip");
        assertThat(mvcResult.getResponse().getContentAsString()).isNotBlank();
        String directoryName = null;
        Path directory = null;
        try {
            directoryName = UUID.randomUUID().toString();
            directory = fileService.createDirectory(directoryName);
            Path pathToPackage = fileService.storeFile(new ByteArrayInputStream(mvcResult.getResponse()
                    .getContentAsByteArray()), directory, VNFD_ZIP);
            fileService.unpack(pathToPackage, 15);
            Path vnfdPath = directory.resolve("cnf_vnfd.yaml");
            checkIfVnfdContentIsCorrect(packageId, Files.readAllBytes(vnfdPath));
        } finally {
            if (directory != null) {
                fileService.deleteDirectory(directoryName);
            }
        }
    }

    @Test
    public void shouldGetVnfdForContentTypeApplicationJson() throws Exception {
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.get(VNF_PACKAGE_API
                + "/c2def1ce-4cf4-477c-aab3-21cb04e6a386/vnfd").accept("application/json");
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(400);
        ProblemDetails problemDetails = getProblemDetails(mvcResult.getResponse().getContentAsString());
        assertThat(problemDetails).isNotNull();
        assertThat(problemDetails.getType().toString()).isNotBlank().isEqualTo(Constants.TYPE_BLANK);
        assertThat(problemDetails.getTitle()).isNotBlank().isEqualTo("User input is not correct");
        assertThat(problemDetails.getStatus()).isEqualTo(400);
        assertThat(problemDetails.getDetail()).isNotBlank().isEqualTo("Invalid accept type provided application/json, only text/plain, application/zip or text/plain,application/zip is supported");
    }

    @Test
    public void shouldGetVnfdForInvalidContentType() throws Exception {
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.get(VNF_PACKAGE_API +
                "/c2def1ce-4cf4-477c-aab3-21cb04e6a386/vnfd").accept("gdsfgsd/f");
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(400);
        ProblemDetails problemDetails = getProblemDetails(mvcResult.getResponse().getContentAsString());
        assertThat(problemDetails).isNotNull();
        assertThat(problemDetails.getType().toString()).isNotBlank().isEqualTo(Constants.TYPE_BLANK);
        assertThat(problemDetails.getTitle()).isNotBlank().isEqualTo("Media type not supported for this api");
        assertThat(problemDetails.getStatus()).isEqualTo(400);
        assertThat(problemDetails.getDetail()).isNotBlank().isEqualTo("No acceptable representation");
    }

    @Test
    public void shouldGetVnfdForInvalidId() throws Exception {
        String packageId = "546454jfhfh";
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.get(VNF_PACKAGE_API +
                "/" + packageId + "/vnfd").accept(MediaType.TEXT_PLAIN);
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(404);
        assertThat(mvcResult.getResponse().getContentAsString()).isNotBlank();
        ProblemDetails problemDetails = getProblemDetails(mvcResult.getResponse().getContentAsString());
        assertThat(problemDetails).isNotNull();
        assertThat(problemDetails.getType().toString()).isNotBlank().isEqualTo(Constants.TYPE_BLANK);
        assertThat(problemDetails.getTitle()).isNotBlank().isEqualTo("Package not found");
        assertThat(problemDetails.getStatus()).isEqualTo(404);
        assertThat(problemDetails.getDetail()).isNotBlank().isEqualTo(String.format(Constants.PACKAGE_NOT_PRESENT_ERROR_MESSAGE, packageId));
    }

    @Test
    public void shouldDeletePackage() throws Exception {
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.delete(VNF_PACKAGE_API +
                        "/" + VNF_PACKAGE_ID_DELETE)
                .header(Constants.IDEMPOTENCY_KEY_HEADER, UUID.randomUUID());
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(204);
        createVnfPackage();
    }

    @Test
    public void deleteInvalidPackageDoesNotExist() throws Exception {
        String packageId = "546454jfhfh";
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.delete(VNF_PACKAGE_API +
                        "/" + packageId)
                .header(Constants.IDEMPOTENCY_KEY_HEADER, UUID.randomUUID());

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(404);
        assertThat(mvcResult.getResponse().getContentAsString()).isNotBlank();
        ProblemDetails problemDetails = getProblemDetails(mvcResult.getResponse().getContentAsString());
        assertThat(problemDetails).isNotNull();
        assertThat(problemDetails.getType().toString()).isNotBlank().isEqualTo(Constants.TYPE_BLANK);
        assertThat(problemDetails.getTitle()).isNotBlank().isEqualTo("Package not found");
        assertThat(problemDetails.getStatus()).isEqualTo(404);
        assertThat(problemDetails.getDetail()).isNotBlank().isEqualTo(String.format(Constants.VNF_PACKAGE_WITH_ID_DOES_NOT_EXIST, packageId));
    }

    @Test
    public void deletePackageInUse() throws Exception {
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.delete(VNF_PACKAGE_API +
                        "/" + VNF_PACKAGE_ID_DELETE_IN_USE)
                .header(Constants.IDEMPOTENCY_KEY_HEADER, UUID.randomUUID());
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(409);
        assertThat(mvcResult.getResponse().getContentAsString()).isNotBlank();
        ProblemDetails problemDetails = getProblemDetails(mvcResult.getResponse().getContentAsString());
        assertThat(problemDetails).isNotNull();
        assertThat(problemDetails.getType().toString()).isNotBlank().isEqualTo(Constants.TYPE_BLANK);
        assertThat(problemDetails.getTitle()).isNotBlank().isEqualTo("Invalid package state");
        assertThat(problemDetails.getStatus()).isEqualTo(409);
        assertThat(problemDetails.getDetail()).isNotBlank().isEqualTo(String.format(Constants.INVALID_PACKAGE_USAGE_STATE));
        createVnfPackage();
    }

    @Test
    public void shouldGetArtifactForTextPlainFormat() throws Exception {
        String packageId = "f3def1ce-4cf4-477c-456";
        String artifactPath = "TOSCA-Metadata/TOSCA.meta";

        final RequestBuilder requestBuilder = MockMvcRequestBuilders.get(VNF_PACKAGE_API
                        + "/" + packageId + "/artifacts/" + artifactPath)
                .accept("text/plain");
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        System.out.println(mvcResult.getResponse());
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(200);
        assertThat(mvcResult.getResponse().getContentType()).isNotNull().isEqualTo("text/plain");
        assertThat(mvcResult.getResponse().getContentAsString()).isEqualTo("Sample text/plain file content");
    }

    @Test
    public void shouldGetArtifactForZipFormat() throws Exception {
        String packageId = "f3def1ce-4cf4-477c-123";
        String artifactPath = "Definitions/OtherTemplates/sample-vnf-0.1.2.tgz";

        final RequestBuilder requestBuilder = MockMvcRequestBuilders.get(VNF_PACKAGE_API
                        + "/" + packageId + "/artifacts/" + artifactPath)
                .accept("application/zip");
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(200);
        assertThat(mvcResult.getResponse().getContentType()).isNotNull().isEqualTo("application/zip");
        assertThat(mvcResult.getResponse().getContentAsString()).isNotBlank();
    }

    @Test
    public void shouldNotGetPackageForInvalidId() throws Exception {
        String packageId = "1325463";
        String artifactPath = "Definitions/OtherTemplates/spider-app-2.216.9.tgz";
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.get(VNF_PACKAGE_API +
                "/" + packageId + "/artifacts/" + artifactPath).accept("application/zip");
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(404);
        ProblemDetails problemDetails = getProblemDetails(mvcResult.getResponse().getContentAsString());
        assertThat(problemDetails).isNotNull();
        assertThat(problemDetails.getType().toString()).isNotBlank().isEqualTo(Constants.TYPE_BLANK);
        assertThat(problemDetails.getTitle()).isNotBlank().isEqualTo("Package not found");
        assertThat(problemDetails.getStatus()).isEqualTo(404);
        assertThat(problemDetails.getDetail()).isNotBlank().isEqualTo(String.format(Constants.PACKAGE_NOT_PRESENT_ERROR_MESSAGE, packageId));
    }

    @Test
    public void shouldNotGetArtifactForInvalidPath() throws Exception {
        String packageId = "f3def1ce-4cf4-477c-123";
        String artifactPath = "invalidArtifactPath";
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.get(VNF_PACKAGE_API +
                "/" + packageId + "/artifacts/" + artifactPath).accept("application/zip");
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(404);
        assertThat(mvcResult.getResponse().getContentAsString()).isNotBlank();
        ProblemDetails problemDetails = getProblemDetails(mvcResult.getResponse().getContentAsString());
        assertThat(problemDetails).isNotNull();
        assertThat(problemDetails.getType().toString()).isNotBlank().isEqualTo(Constants.TYPE_BLANK);
        assertThat(problemDetails.getTitle()).isNotBlank().isEqualTo("ArtifactPath not found");
        assertThat(problemDetails.getStatus()).isEqualTo(404);
        assertThat(problemDetails.getDetail()).isNotBlank().isEqualTo(String.format(Constants.ARTIFACT_NOT_PRESENT_ERROR_MESSAGE, artifactPath));
    }

    @Test
    @Transactional
    public void shouldGetHelmfileContentByPackageId() throws Exception {
        String packageId = "c2def1ce-4cf4-477c-1234";
        final RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get(String.format(VNF_HELMFILE_API, packageId))
                .accept(MediaType.APPLICATION_JSON);
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(200);
        assertThat(mvcResult.getResponse().getContentAsString()).isNotEmpty();
    }

    @Test
    public void shouldReturnNotFoundForInvalidPackageId() throws Exception {
        String packageId = "not-found";
        final RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get(String.format(VNF_HELMFILE_API, packageId))
                .accept(MediaType.APPLICATION_JSON);
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    @Transactional
    public void shouldReturnNotFoundForWhenAppPackageNotContainHelmfile() throws Exception {
        String packageId = "f3def1ce-4cf4-477c-123";
        final RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get(String.format(VNF_HELMFILE_API, packageId))
                .accept(MediaType.APPLICATION_JSON);
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(404);
    }

    private void checkIfVnfdContentIsCorrect(String packageId, byte[] responseInByteArray) throws Exception {
        final Yaml yaml = new Yaml();
        final Map<String, Object> responseMap = yaml.load(new ByteArrayInputStream(responseInByteArray));
        ObjectMapper mapper = new ObjectMapper();
        Optional<AppPackage> appPackage = repository.findByPackageId(packageId);
        Map<String, Object> vnfd;
        if (appPackage.isPresent()) {
            vnfd = mapper.readValue(appPackage.get().getDescriptorModel(), new TypeReference<>() {});
            assertThat(responseMap).isEqualTo(vnfd);
        } else {
            fail("Unable to fetch model from entity");
        }
    }

    private MvcResult createVnfPackage() throws Exception {
        CreateVnfPkgInfoRequest createVnfPkgInfoRequest = new CreateVnfPkgInfoRequest();

        MvcResult mvcResult = postMvcResult(VNF_PACKAGE_API, objectMapper.writeValueAsString(createVnfPkgInfoRequest));
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(201);
        return mvcResult;
    }

    private void verifyChartTableEntries(int expectedSize) {
        String chartUrl = "http://10.210.53.96:31028/api/onboarded/charts/Ericsson.SGSN-MME/1.25";
        List<AppPackage> chartUrls = packageService.listPackagesWithChartUrl(chartUrl);
        assertThat(chartUrls.size()).isEqualTo(expectedSize);
    }

    private static ProblemDetails getProblemDetails(String problemDetails) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(problemDetails, ProblemDetails.class);
        } catch (Exception ex) {
            fail("Failure while converting json to ProblemDetails");
        }
        return null;
    }

    private MvcResult putMvcResult(String putUrl, String requestContent) throws Exception {
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.put(putUrl).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(requestContent);
        return mockMvc.perform(requestBuilder).andReturn();
    }

    private MvcResult postMvcResult(String postUrl, String requestContent) throws Exception {
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.post(postUrl).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).header(Constants.IDEMPOTENCY_KEY_HEADER, UUID.randomUUID())
                .content(requestContent);
        return mockMvc.perform(requestBuilder).andReturn();
    }

    private MvcResult getMvcResult(String getUrl) throws Exception {
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.get(getUrl).accept(MediaType.APPLICATION_JSON);
        return mockMvc.perform(requestBuilder).andReturn();
    }

    private MvcResult deleteMvcResult(String deleteUrl) throws Exception {
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.delete(deleteUrl)
                .header(Constants.IDEMPOTENCY_KEY_HEADER, UUID.randomUUID());
        return mockMvc.perform(requestBuilder).andReturn();
    }

}
