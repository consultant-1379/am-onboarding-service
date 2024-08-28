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
package com.ericsson.amonboardingservice.presentation.services.vnfdservice;

import com.ericsson.am.shared.vnfd.model.CustomInterfaceInputs;
import com.ericsson.am.shared.vnfd.model.CustomOperation;
import com.ericsson.am.shared.vnfd.model.DataType;
import com.ericsson.am.shared.vnfd.model.HelmChart;
import com.ericsson.am.shared.vnfd.model.ImageDetails;
import com.ericsson.am.shared.vnfd.model.InterfaceType;
import com.ericsson.am.shared.vnfd.model.InterfaceTypeImpl;
import com.ericsson.am.shared.vnfd.model.VnfDescriptorDetails;
import com.ericsson.am.shared.vnfd.model.policies.VnfPackageChangePolicyTosca1dot2;
import com.ericsson.am.shared.vnfd.validation.vnfd.VnfdValidationException;
import com.ericsson.amonboardingservice.presentation.exceptions.PackageNotFoundException;
import com.ericsson.amonboardingservice.presentation.exceptions.UnhandledException;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageArtifactsRepository;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.ericsson.amonboardingservice.presentation.services.packageservice.AppPackageValidator;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageDatabaseService;
import com.ericsson.amonboardingservice.utils.Constants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.ericsson.am.shared.vnfd.model.HelmChartType.CNF;
import static com.ericsson.amonboardingservice.TestUtils.getResource;
import static com.ericsson.amonboardingservice.TestUtils.readDataFromFile;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_DEFINITIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {VnfdServiceImpl.class, PackageDatabaseService.class})
@ActiveProfiles("test")
@MockBean(classes = {
        AppPackageArtifactsRepository.class,
        AppPackageValidator.class
})
public class VnfdServiceTest {
    private static final String VALID_VNFD_DESCRIPTOR = "valid_vnfd.json";
    private static final String TEST_VNFD = "vnfd/test_vnfd.yaml";
    private static final String INVALID_VNFD_REL4 = "vnfd/invalid_vnfd_rel4_with_incorrect_aspects.yaml";
    private static final String VALID_VNFD_NO_REL4 = "vnfd/valid_vnfd_with_correct_aspects.yaml";
    private static final String INVALID_VNFD_NO_REL4  = "vnfd/invalid_vnfd_with_incorrect_aspects.yaml";
    private static final String VNFD_WITH_NO_DESCRIPTOR = "vnfd_with_no_descriptor_id.json";
    private static final String VNFD_WITH_NO_PROPERTIES = "vnfd_with_no_properties_key.json";
    private static final String VALID_NESTED_VNFD_FILE = "vnfd/validNestedDescriptorFiles/sample-multichart-descriptor.yaml";
    private static final String VALID_VNFD_WITH_ROLLBACK_INTERFACE_DETAILS = "vnfd/valid_vnfd_with_multiple_rollback_operation.yaml";
    private static final String VALID_VNFD_WITH_CUSTOM_INTERFACE_DETAILS = "vnfd/valid_vnfd_with_multiple_custom_operation.yaml";
    private static final String VALID_VNFD_FOR_TESTING_VALIDATION = "vnfd/valid_vnfd_for_testing_validation.yaml";
    private static final String VALID_VNFD_WITH_DIFFERENT_MODIFIABLE_ATTRIBUTE_TYPE = "vnfd/valid_vnfd_with_different_modifiable_attribute_type.yaml";
    private static final String VALID_TOSCA_1_3_MULTI_CHART_VNFD = "vnfd/tosca_1_3/valid_tosca_1_3_multi_charts.yaml";
    private static final String VALID_VNFD_REL4 = "vnfd/valid_vnfd_rel4_with_scale_mapping.yaml";
    private static final String VALID_VNFD_REL4_WITHOUT_SCALE_MAPPING = "vnfd/valid_vnfd_rel4_without_scale_mapping.yaml";
    private static final String INVALID_VNFD_REL4_WITH_WRONG_SCALE_MAPPING_PATH = "vnfd/invalid_vnfd_rel4_with_wrong_scale_mapping_path.yaml";
    private static final String INVALID_VNFD_WITH_INDENTATION_PROBLEM_IN_NODE_TYPE = "vnfd/invalid_vnfd_with_indentation_problem_in_data_type.yaml";
    private static final String DATA_TYPES = "data_types";
    private static final String PROPERTIES = "properties";
    private static final String INSTANTIATE_ADDITIONAL_PARAMS_KEY = "ericsson.datatypes.nfv.InstantiateVnfOperationAdditionalParameters";
    private Path csarBasePath;
    private Path validVnfdPath;
    private Path validTestVnfdPath;
    private Path nestedVnfdPath;
    private Path vnfdWithRollbackInterfacePath;
    private Path vnfdWithCustomInterfacePath;
    private Path vnfdWithAllDetailsPath;
    private Path validTosca1dot3VnfdPath;
    private Path validVnfdWithDifferentModifiableAttributeTypePath;
    private Path invalidVnfdWithIndentationProblemPath;
    private Path validVnfdRel4;

    private Path validVnfdRel4WithoutScaleMapping;

    private Path invalidVnfdRel4WithWrongScaleMappingPath;

    @TempDir
    public Path tempFolder;

    @Autowired
    private PackageDatabaseService packageDatabaseService;
    @Autowired
    private VnfdService vnfdService;
    @MockBean
    private FileService fileService;
    @MockBean
    private AppPackageRepository appPackageRepository;


    @BeforeEach
    public void setup() throws URISyntaxException, IOException {
        csarBasePath = getResource(".");
        validVnfdPath = getResource(Constants.VALID_VNFD_FILE);
        validTestVnfdPath = getResource(TEST_VNFD);
        nestedVnfdPath = getResource(VALID_NESTED_VNFD_FILE);
        vnfdWithRollbackInterfacePath = getResource(VALID_VNFD_WITH_ROLLBACK_INTERFACE_DETAILS);
        vnfdWithCustomInterfacePath = getResource(VALID_VNFD_WITH_CUSTOM_INTERFACE_DETAILS);
        vnfdWithAllDetailsPath = getResource(VALID_VNFD_FOR_TESTING_VALIDATION);
        validTosca1dot3VnfdPath = getResource(VALID_TOSCA_1_3_MULTI_CHART_VNFD);
        validVnfdWithDifferentModifiableAttributeTypePath = getResource(VALID_VNFD_WITH_DIFFERENT_MODIFIABLE_ATTRIBUTE_TYPE);
        invalidVnfdWithIndentationProblemPath = getResource(INVALID_VNFD_WITH_INDENTATION_PROBLEM_IN_NODE_TYPE);
        validVnfdRel4 = getResource(VALID_VNFD_REL4);
        validVnfdRel4WithoutScaleMapping = getResource(VALID_VNFD_REL4_WITHOUT_SCALE_MAPPING);
        invalidVnfdRel4WithWrongScaleMappingPath = getResource(INVALID_VNFD_REL4_WITH_WRONG_SCALE_MAPPING_PATH);
    }

    @Test
    public void testGetDescriptorIdFromJsonObjectVnfd() throws IOException {
        JSONObject vnfd = new JSONObject(readDataFromFile(VALID_VNFD_DESCRIPTOR, StandardCharsets.UTF_8));
        String descriptorId = vnfdService.getDescriptorId(vnfd);

        assertThat(descriptorId).isEqualTo("def1ce-4cf4-477c-aab3-2b04e6a382");
    }

    @Test
    public void testGetDescriptorIdFromJsonObjectVnfdWhenNoPropertiesKey() throws IOException {
        JSONObject vnfd = new JSONObject(readDataFromFile(VNFD_WITH_NO_PROPERTIES, StandardCharsets.UTF_8));
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> vnfdService.getDescriptorId(vnfd))
                .withMessage("No properties value present in VNFD");
    }

    @Test
    public void testGetDescriptorIdFromJsonObjectVnfdWhenNoVnfdDescriptor() throws IOException {
        JSONObject vnfd = new JSONObject(readDataFromFile(VNFD_WITH_NO_DESCRIPTOR, StandardCharsets.UTF_8));
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> vnfdService.getDescriptorId(vnfd))
                .withMessage("No descriptor_id value present in VNFD");
    }

    @Test
    public void testValidateAndGetVnfDescriptorDetailsWithNestedVnfdException() {
        Map<String, Path> artifactPaths = new HashMap<>();
        artifactPaths.put(ENTRY_DEFINITIONS, nestedVnfdPath);
        assertThatThrownBy(() -> vnfdService.validateAndGetVnfDescriptorDetails(artifactPaths))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage(String.format("Failed to get VNF Descriptor from: %s. Nested vnfd is currently unsupported", nestedVnfdPath));
    }

    @Test
    public void testCreateYamlWithInvalidYaml() {
        Assertions.assertThatThrownBy(() -> vnfdService.createVnfdYamlFile("{\"dsda\""))
                .isInstanceOf(UnhandledException.class)
                .hasMessageStartingWith(String.format(Constants.INVALID_VNFD_JSON_ERROR_MESSAGE,
                        "Unexpected end-of-input within/between Object entries\n" +
                                " at [Source: (String)\"{\"dsda\"\"; line: 1, column: 8]"));
    }


    @Test
    public void testCreateYamlFileForNullJson() {
        Assertions.assertThatThrownBy(() -> vnfdService.createVnfdYamlFile(null))
                .isInstanceOf(UnhandledException.class)
                .hasMessageStartingWith(Constants.VNFD_CANT_BE_EMPTY_ERROR_MESSAGE);
    }

    @Test
    public void testValidateAndGetVnfDescriptorWithMappingfile() {
        Map<String, Path> artifactPaths = new HashMap<>();
        artifactPaths.put(ENTRY_DEFINITIONS, vnfdWithAllDetailsPath);
        artifactPaths.put(Constants.CSAR_DIRECTORY, csarBasePath);
        VnfDescriptorDetails vnfDescriptorDetails = vnfdService.validateAndGetVnfDescriptorDetails(artifactPaths);
        assertThat(artifactPaths).containsKey(Constants.SCALING_MAPPING);
        assertThat(vnfDescriptorDetails.getAllDataTypes().containsKey("ericsson.datatypes.nfv.VnfInfoModifiableAttributes")).isTrue();
        assertThat(vnfDescriptorDetails).isNotNull();
    }

    @Test
    public void testValidateAspectsSuccessWithValidRel4Vnfd() throws  URISyntaxException {
        Map<String, Path> artifactPaths = new HashMap<>();
        artifactPaths.put(ENTRY_DEFINITIONS, getResource(VALID_VNFD_REL4));
        artifactPaths.put(Constants.CSAR_DIRECTORY, csarBasePath);
        VnfDescriptorDetails vnfDescriptorDetails = vnfdService.validateAndGetVnfDescriptorDetails(artifactPaths);
        assertThat(vnfDescriptorDetails).isNotNull();
    }

    @Test
    public void testValidateAspectsSuccessWithNoRel4Vnfd() throws URISyntaxException {
        Map<String, Path> artifactPaths = new HashMap<>();
        artifactPaths.put(ENTRY_DEFINITIONS, getResource(VALID_VNFD_NO_REL4));
        artifactPaths.put(Constants.CSAR_DIRECTORY, csarBasePath);
        VnfDescriptorDetails vnfDescriptorDetails = vnfdService.validateAndGetVnfDescriptorDetails(artifactPaths);
        assertThat(vnfDescriptorDetails).isNotNull();
    }

    @Test
    public void testValidateAspectsFailWithInvalidNoRel4Vnfd() throws URISyntaxException {
        Map<String, Path> artifactPaths = new HashMap<>();
        artifactPaths.put(ENTRY_DEFINITIONS, getResource(INVALID_VNFD_NO_REL4));
        artifactPaths.put(Constants.CSAR_DIRECTORY, csarBasePath);
        assertThatThrownBy(() -> vnfdService.validateAndGetVnfDescriptorDetails(artifactPaths))
                .isInstanceOf(VnfdValidationException.class)
                .hasMessageStartingWith("invalid vnfd, as one of the VDUs is under the two DMs");
    }

    @Test
    public void testValidateAspectsFailWithInvalidRel4Vnfd() throws URISyntaxException {
        Map<String, Path> artifactPaths = new HashMap<>();
        artifactPaths.put(ENTRY_DEFINITIONS, getResource(INVALID_VNFD_REL4));
        artifactPaths.put(Constants.CSAR_DIRECTORY, csarBasePath);
        assertThatThrownBy(() -> vnfdService.validateAndGetVnfDescriptorDetails(artifactPaths))
                .isInstanceOf(VnfdValidationException.class)
                .hasMessageStartingWith("invalid vnfd, as one of the VDUs is under the two DMs");
    }

    @Test
    public void testValidateAndGetVnfDescriptorWithScaleMappingFileRel4() {
        Map<String, Path> artifactPaths = new HashMap<>();
        artifactPaths.put(ENTRY_DEFINITIONS, validVnfdRel4);
        artifactPaths.put(Constants.CSAR_DIRECTORY, csarBasePath);
        VnfDescriptorDetails vnfDescriptorDetails = vnfdService.validateAndGetVnfDescriptorDetails(artifactPaths);
        assertThat(artifactPaths).containsKey(Constants.SCALING_MAPPING);
        assertThat(vnfDescriptorDetails.getAllDataTypes().containsKey("ericsson.datatypes.nfv.VnfInfoModifiableAttributes")).isTrue();
        assertThat(vnfDescriptorDetails).isNotNull();
    }

    @Test
    public void testValidateAndGetVnfDescriptorWitWrongScaleMappingPathRel4() {
        Map<String, Path> artifactPaths = new HashMap<>();
        artifactPaths.put(ENTRY_DEFINITIONS, invalidVnfdRel4WithWrongScaleMappingPath);
        artifactPaths.put(Constants.CSAR_DIRECTORY, csarBasePath);
        assertThatThrownBy(() -> vnfdService.validateAndGetVnfDescriptorDetails(artifactPaths))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Scaling mapping path provided in VNFD is invalid:");
    }

    @Test
    public void testValidateAndGetVnfDescriptorWithoutScaleMappingFileRel4() {
        Map<String, Path> artifactPaths = new HashMap<>();
        artifactPaths.put(ENTRY_DEFINITIONS, validVnfdRel4WithoutScaleMapping);
        artifactPaths.put(Constants.CSAR_DIRECTORY, csarBasePath);
        vnfdService.validateAndGetVnfDescriptorDetails(artifactPaths);
    }

    @Test
    public void testValidateAndGetVnfDescriptorWithDifferentTypeOfModifiableAttribute() {
        Map<String, Path> artifactPaths = new HashMap<>();
        artifactPaths.put(ENTRY_DEFINITIONS, validVnfdWithDifferentModifiableAttributeTypePath);
        artifactPaths.put(Constants.CSAR_DIRECTORY, csarBasePath);
        VnfDescriptorDetails vnfDescriptorDetails = vnfdService.validateAndGetVnfDescriptorDetails(artifactPaths);
        assertThat(artifactPaths).containsKey("scaling_mapping");
        assertThat(vnfDescriptorDetails.getAllDataTypes()
                .containsKey("ericsson.PCC.AXB25019_1.CXP9037447_1.R42B.datatypes.nfv.VnfInfoModifiableAttributes"))
                .isTrue();
        assertThat(vnfDescriptorDetails).isNotNull();
    }

    @Test
    public void testValidateAndGetVnfDescriptorDetailsWithInvalidVnfd() {
        Map<String, Path> artifactPaths = new HashMap<>();
        artifactPaths.put(ENTRY_DEFINITIONS, invalidVnfdWithIndentationProblemPath);
        assertThatThrownBy(() -> vnfdService.validateAndGetVnfDescriptorDetails(artifactPaths))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ossTopology.transportProtocol is not valid property attribute for the property key " +
                        "ossTopology.timeZone");
    }

    @Test
    public void testValidateAndGetVnfDescriptorDetails() {
        Map<String, Path> artifactPaths = new HashMap<>();
        artifactPaths.put(ENTRY_DEFINITIONS, validVnfdPath);
        VnfDescriptorDetails vnfDescriptorDetails = vnfdService.validateAndGetVnfDescriptorDetails(artifactPaths);
        assertThat(vnfDescriptorDetails).isNotNull();
        assertThat(vnfDescriptorDetails.getVnfDescriptorId()).isEqualTo("d3def1ce-4cf4-477c-aab3-21cb04e6a379");
        assertThat(vnfDescriptorDetails.getVnfDescriptorVersion()).isEqualTo("cxp9025898_4r81e08");
        assertThat(vnfDescriptorDetails.getVnfProductName()).isEqualTo("SGSN-MME");
        assertThat(vnfDescriptorDetails.getVnfSoftwareVersion()).isEqualTo("1.20 (CXS101289_R81E08)");
        assertThat(vnfDescriptorDetails.getVnfProvider()).isEqualTo("Ericsson");
        assertThat(vnfDescriptorDetails.getVnfmInfo()).isEqualTo("3881:E-VNFM");
        assertThat(getImagePath(vnfDescriptorDetails)).isEqualTo("/path/to/swImageFile");
        List<HelmChart> helmCharts = vnfDescriptorDetails.getHelmCharts();
        assertThat(helmCharts.get(0).getPath()).isEqualTo("/path/to/helm");
        assertThat(helmCharts.get(0).getChartType()).isEqualTo(CNF);
        assertThat(helmCharts.get(0).getChartKey()).isEqualTo("helm_package");
        assertThat(vnfDescriptorDetails.getDefaultFlavour()).isNotNull();
        assertThat(vnfDescriptorDetails.getAllDataTypes()).hasSize(1);
        assertThat(vnfDescriptorDetails.getAllInterfaceTypes()).isEmpty();
    }

    @Test
    public void testValidateAndGetVnfDescriptorDetailsEmptyPath() {
        Map<String, Path> artifactPaths = new HashMap<>();
        artifactPaths.put(ENTRY_DEFINITIONS, Paths.get(""));
        assertThrows(IllegalArgumentException.class, () -> vnfdService.validateAndGetVnfDescriptorDetails(artifactPaths));
    }

    @Test
    public void testValidateAndGetVnfDescriptorDetailsFileNotPresent() {
        Map<String, Path> artifactPaths = new HashMap<>();
        artifactPaths.put(ENTRY_DEFINITIONS, Paths.get("/test"));
        assertThrows(IllegalArgumentException.class, () -> vnfdService.validateAndGetVnfDescriptorDetails(artifactPaths));
    }

    @Test
    public void testValidateAndGetVnfDescriptorDetailsWithRollBackInterfaceDetails() {
        Map<String, Path> artifactPaths = new HashMap<>();
        artifactPaths.put(ENTRY_DEFINITIONS, vnfdWithRollbackInterfacePath);
        VnfDescriptorDetails descriptorDetails = vnfdService.validateAndGetVnfDescriptorDetails(artifactPaths);
        assertThat(descriptorDetails.getDefaultFlavour()).isNotNull();
        assertThat(descriptorDetails.getAllDataTypes().size()).isEqualTo(3);
        Map<String, InterfaceType> allInterfaceType = descriptorDetails.getAllInterfaceTypes();
        Map<String, DataType> allDataType = descriptorDetails.getAllDataTypes();
        assertThat(allInterfaceType.size()).isEqualTo(1);
        for (String key : allInterfaceType.keySet()) {
            InterfaceType interfaceType = allInterfaceType.get(key);
            Map<String, InterfaceTypeImpl> customInterface = descriptorDetails.getDefaultFlavour()
                    .getTopologyTemplate().getNodeTemplate().getCustomInterface();
            for (String customInterfaceKey : customInterface.keySet()) {
                assertThat(customInterface.get(customInterfaceKey).getInterfaceType())
                        .isEqualTo(key);
            }
            assertThat(interfaceType.getDerivedFrom()).isEqualTo("tosca.interfaces.nfv.ChangeCurrentVnfPackage");
            Map<String, CustomOperation> allOperation = interfaceType.getOperation();
            assertThat(allOperation.size()).isEqualTo(2);
            for (String operationKey : allOperation.keySet()) {
                CustomOperation operation = allOperation.get(operationKey);
                CustomInterfaceInputs rollbackInput = (CustomInterfaceInputs) operation.getInput();
                String dataTypeName = rollbackInput.getAdditionalParams().getDataTypeName();
                assertThat(allDataType.get(dataTypeName)).isNotNull();
            }
        }
    }

    @Test
    public void testValidateAndGetVnfDescriptorDetailsWithCustomInterfaceDetails() {
        Map<String, Path> artifactPaths = new HashMap<>();
        artifactPaths.put(ENTRY_DEFINITIONS, vnfdWithCustomInterfacePath);
        VnfDescriptorDetails descriptorDetails = vnfdService.validateAndGetVnfDescriptorDetails(artifactPaths);
        assertThat(descriptorDetails.getDefaultFlavour()).isNotNull();
        assertThat(descriptorDetails.getAllDataTypes().size()).isEqualTo(4);
        Map<String, InterfaceType> allInterfaceType = descriptorDetails.getAllInterfaceTypes();
        Map<String, DataType> allDataType = descriptorDetails.getAllDataTypes();
        assertThat(allInterfaceType.size()).isEqualTo(1);
        for (String key : allInterfaceType.keySet()) {
            InterfaceType interfaceType = allInterfaceType.get(key);
            Map<String, InterfaceTypeImpl> customInterface = descriptorDetails.getDefaultFlavour()
                    .getTopologyTemplate().getNodeTemplate().getCustomInterface();
            for (String customInterfaceKey : customInterface.keySet()) {
                assertThat(customInterface.get(customInterfaceKey).getInterfaceType())
                        .isEqualTo(key);
            }
            assertThat(interfaceType.getDerivedFrom()).isEqualTo("tosca.interfaces.nfv.ChangeCurrentVnfPackage");
            Map<String, CustomOperation> allOperations = interfaceType.getOperation();
            assertThat(allOperations.size()).isEqualTo(2);
            for (String operationKey : allOperations.keySet()) {
                CustomOperation operation = allOperations.get(operationKey);
                CustomInterfaceInputs customInterfaceInputs = (CustomInterfaceInputs) operation.getInput();
                String dataTypeName = customInterfaceInputs.getAdditionalParams().getDataTypeName();
                assertThat(allDataType.get(dataTypeName)).isNotNull();
                Map<String, Object> staticAdditionalParams = customInterfaceInputs.getStaticAdditionalParams();
                if ("rollback_from_package6_to_package4".equals(operationKey)) {
                    assertThat(staticAdditionalParams).size().isEqualTo(1);
                    assertThat(staticAdditionalParams.containsKey("rollback_pattern")).isTrue();
                } else {
                    assertThat(staticAdditionalParams).isNull();
                }
            }
            Map<String, VnfPackageChangePolicyTosca1dot2> allVnfPackageChangePolicy = descriptorDetails.getDefaultFlavour()
                    .getTopologyTemplate().getPolicies().getAllVnfPackageChangePolicy();
            allVnfPackageChangePolicy.forEach((key1, value) -> assertThat(value.getTriggers().size()).isEqualTo(1));
        }
    }

    @Test
    public void testValidateAndGetVnfDescriptorDetailsWithCorrectAdditionalParamsOrder() throws JsonProcessingException {
        Map<String, Path> artifactPaths = new HashMap<>();
        artifactPaths.put(ENTRY_DEFINITIONS, validVnfdPath);
        VnfDescriptorDetails vnfDescriptorDetails = vnfdService.validateAndGetVnfDescriptorDetails(artifactPaths);
        assertThat(vnfDescriptorDetails).isNotNull();
        assertThat(vnfDescriptorDetails.getDescriptorModel()).isNotNull();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode descriptorModelJson = mapper.readTree(vnfDescriptorDetails.getDescriptorModel());
        assertTrue(descriptorModelJson.has(DATA_TYPES));
        JsonNode dataTypes = descriptorModelJson.get(DATA_TYPES);
        assertTrue(dataTypes.has(INSTANTIATE_ADDITIONAL_PARAMS_KEY));
        JsonNode instantiateAdditionalParamsNode = dataTypes.get(INSTANTIATE_ADDITIONAL_PARAMS_KEY);
        assertTrue(instantiateAdditionalParamsNode.has(PROPERTIES));
        JsonNode properties = instantiateAdditionalParamsNode.get(PROPERTIES);
        Iterator<String> paramKeys = properties.fieldNames();
        assertEquals("clusterIp", paramKeys.next());
        assertEquals("global.internalServiceEndpoint.port", paramKeys.next());
        assertEquals("server.service.loadBalancerIP", paramKeys.next());
    }

    @Test
    public void testGetVnfDescriptorJSONObjectFromPathToExistingFile() {
        JSONObject vnfDescriptor = vnfdService.getVnfDescriptor(validTosca1dot3VnfdPath);

        assertThat(vnfDescriptor).isNotNull();
        assertThat(vnfDescriptor.has(Constants.TOSCA_DEFINITIONS_VERSION)).isTrue();
        assertThat(vnfDescriptor.getString(Constants.TOSCA_DEFINITIONS_VERSION)).isEqualTo(Constants.TOSCA_1_3_DEFINITIONS_VERSION);
    }

    @Test
    public void testIsMultipleFileVnfd() {
        when(appPackageRepository.findIsMultipleVnfdFileForPackageId(any())).thenReturn(true);
        boolean packageIsMultipleVnfd = vnfdService.isMultipleFileVnfd("app-package-id");

        assertThat(packageIsMultipleVnfd).isTrue();
    }

    @Test
    public void testCreateVnfdZip() throws IOException {
        AppPackage appPackage = new AppPackage();
        byte[] bytes = appPackage.getVnfdZip();
        when(appPackageRepository.findVnfdZipForPackageId(any())).thenReturn(bytes);
        vnfdService.createVnfdZip("package-id");

        verify(appPackageRepository).findVnfdZipForPackageId(eq("package-id"));
        verify(fileService).createFileFromByte(eq(bytes), eq("vnfd.zip"));
    }

    @Test
    public void testCreateVnfdZipFails() throws IOException {
        AppPackage appPackage = new AppPackage();
        byte[] bytes = appPackage.getVnfdZip();
        when(appPackageRepository.findVnfdZipForPackageId(any())).thenReturn(bytes);
        when(fileService.createFileFromByte(any(), any())).thenThrow(new IOException());

        assertThatExceptionOfType(UnhandledException.class)
                .isThrownBy(() -> vnfdService.createVnfdZip("package-id"));

        verify(appPackageRepository).findVnfdZipForPackageId(eq("package-id"));
        verify(fileService).createFileFromByte(eq(bytes), eq("vnfd.zip"));
    }

    @Test
    public void testGetVnfdFileNameFailsWithEmptyTosca() {
        String vnfdFileName = vnfdService.getVnfdFileName(null, "package-id");

        assertThat(vnfdFileName).isNull();
    }

    @Test
    public void testGetVnfdFileNameReturnsValue() {
        String vnfdFileName = vnfdService.getVnfdFileName("Entry-Definitions:value", "package-id");

        assertThat(vnfdFileName).isEqualTo("value");
    }

    @Test
    public void testGetVnfdFileNameReturnsValueFromComplex() {
        String vnfdFileName = vnfdService.getVnfdFileName("Entry-Definitions:complex/value", "package-id");

        assertThat(vnfdFileName).isEqualTo("value");
    }

    @Test
    public void testGetVnfdFileNameReturnsValueFromComplexMeta() {
        String vnfdFileName = vnfdService.getVnfdFileName("Entry-Definitions:complex/value", "package-id");

        assertThat(vnfdFileName).isEqualTo("value");
    }

    @Test
    public void testDeleteVnfdZipFile() {
        Path path = tempFolder;
        vnfdService.deleteVnfdZipFile(path);

        verify(fileService).deleteDirectory(any());
    }

    @Test
    public void testCreateYamlFileByPackageIdSuccessful() throws IOException {
        // given
        var packageId = "def1ce-4cf4-477c-aab3-2b04e6a382";
        var appPackage = new AppPackage();
        readDataFromFile(VALID_VNFD_DESCRIPTOR, StandardCharsets.UTF_8);
        appPackage.setDescriptorModel(readDataFromFile(VALID_VNFD_DESCRIPTOR, StandardCharsets.UTF_8));
        when(appPackageRepository.findByPackageIdNotBeingDeleted(packageId)).thenReturn(Optional.of(appPackage));
        var captor = ArgumentCaptor.forClass(ByteArrayInputStream.class);

        // when
        vnfdService.createVnfdYamlFileByPackageId(packageId);

        // then
        verify(fileService).storeFile(captor.capture(), any(), any());
        var captorValue = captor.getValue();
        var expectedVnfd = readDataFromFile(TEST_VNFD, StandardCharsets.UTF_8);
        var actualVnfd = new BufferedReader(
                new InputStreamReader(captorValue, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));

        assertEquals(expectedVnfd, actualVnfd);
    }

    @Test
    public void testCreateYamlFileByPackageIdWillFailIfPackageNotFound() {
        // when / then
        assertThatThrownBy(() -> vnfdService.createVnfdYamlFileByPackageId("unknown"))
                .isInstanceOf(PackageNotFoundException.class)
                .hasMessage("Package with id: unknown not found");
    }

    private String getImagePath(VnfDescriptorDetails vnfDescriptorDetails) {
        return vnfDescriptorDetails.getImagesDetails().stream()
                .map(ImageDetails::getPath)
                .findFirst()
                .orElse("");
    }
}
