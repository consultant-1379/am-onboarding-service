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

import com.ericsson.am.shared.vnfd.CommonUtility;
import com.ericsson.am.shared.vnfd.DataTypeUtility;
import com.ericsson.am.shared.vnfd.NestedVnfdUtility;
import com.ericsson.am.shared.vnfd.NodeTemplateUtility;
import com.ericsson.am.shared.vnfd.PolicyUtility;
import com.ericsson.am.shared.vnfd.ScalingMapUtility;
import com.ericsson.am.shared.vnfd.TopologyTemplateUtility;
import com.ericsson.am.shared.vnfd.VnfdUtility;
import com.ericsson.am.shared.vnfd.model.ArtifactsPropertiesDetail;
import com.ericsson.am.shared.vnfd.model.DataType;
import com.ericsson.am.shared.vnfd.model.NodeProperties;
import com.ericsson.am.shared.vnfd.model.ScaleMapping;
import com.ericsson.am.shared.vnfd.model.VnfDescriptorDetails;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.Flavour;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.ParentVnfd;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.VduCompute;
import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.amonboardingservice.presentation.exceptions.ArtifactNotFoundException;
import com.ericsson.amonboardingservice.presentation.exceptions.FailedOnboardingException;
import com.ericsson.amonboardingservice.presentation.exceptions.UnhandledException;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackageArtifacts;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageArtifactsRepository;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.ericsson.amonboardingservice.presentation.services.packageservice.AppPackageValidator;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageDatabaseService;
import com.ericsson.amonboardingservice.utils.Constants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.ericsson.am.shared.vnfd.utils.Constants.DATA_TYPES_KEY;
import static com.ericsson.am.shared.vnfd.utils.Constants.DEFAULT_KEY;
import static com.ericsson.am.shared.vnfd.utils.Constants.INVALID_VNFD_JSON_ERROR_MESSAGE;
import static com.ericsson.am.shared.vnfd.utils.Constants.NODE_TYPES_KEY;
import static com.ericsson.am.shared.vnfd.utils.Constants.PROPERTIES_KEY;
import static com.ericsson.am.shared.vnfd.utils.Constants.TOPOLOGY_TEMPLATE_KEY;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_DEFINITIONS;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.RELATIVE_PATH_TO_TOSCA_META_FILE;
import static java.lang.String.format;

@Slf4j
@Service
public class VnfdServiceImpl implements VnfdService {

    @Autowired
    private AppPackageRepository appPackageRepository;

    @Autowired
    private PackageDatabaseService databaseService;

    @Autowired
    private AppPackageValidator packageValidator;

    @Autowired
    private FileService fileService;

    @Autowired
    private AppPackageArtifactsRepository appPackageArtifactsRepository;

    private static void validateVnfdWithMappingFile(final JSONObject vnfd, final Flavour flavour) {
        Map<String, DataType> allDataType = DataTypeUtility.buildDataTypesFromVnfd(vnfd);
        Policies policies = flavour.getTopologyTemplate().getPolicies();
        List<VduCompute> vduComputeList = flavour.getTopologyTemplate().getNodeTemplate().getVduCompute();
        vduComputeList.forEach(NodeTemplateUtility::validateVduComputeNode);
        NodeTemplateUtility.validatePoliciesModelTargets(flavour.getTopologyTemplate().getNodeTemplate(), policies);
        TopologyTemplateUtility.validateDataType(vnfd, flavour.getTopologyTemplate());
        TopologyTemplateUtility.validateModifiableAttributesDefaults(allDataType, policies);
    }

    private static void validateVnfdIfMappingFileIsPresent(final JSONObject vnfd, final Flavour flavour, final Path csarBasePath,
                                                           final Map<String, Path> artifactsPaths) {
        if (vnfd.has(DATA_TYPES_KEY) && vnfd.has(TOPOLOGY_TEMPLATE_KEY)
            && vnfd.has(NODE_TYPES_KEY) && vnfd.get(NODE_TYPES_KEY) instanceof JSONObject) {
            JSONObject nodeTypes = vnfd.getJSONObject(NODE_TYPES_KEY);
            var scalingMapping = CommonUtility.getArtifacts(nodeTypes).stream()
                .filter(artifact -> Constants.SCALING_MAPPING.equals(artifact.getId()))
                .findFirst();
            if (scalingMapping.isPresent()) {
                final ArtifactsPropertiesDetail artifact = scalingMapping.get();
                Path scaleMappingFilePath = csarBasePath.resolve(Paths.get(artifact.getFile()));
                LOGGER.info("Scaling Mapping file found {}", scaleMappingFilePath);
                artifactsPaths.put(Constants.SCALING_MAPPING, scaleMappingFilePath);
                Map<String, ScaleMapping> scalingMap = ScalingMapUtility.getScalingMap(scaleMappingFilePath);
                ScalingMapUtility.validateScalingMap(scalingMap, flavour.getTopologyTemplate().getNodeTemplate());
                validateVnfdWithMappingFile(vnfd, flavour);
            }
        }
    }

    private static String getNodeTypeFileJsonContent(JSONObject vnfd, Path descriptorPath) {
        JSONArray imports = vnfd.getJSONArray(Constants.IMPORTS_KEY);
        for (Object importObject : imports) {
            String importFileName = (String) importObject;
            if (!Constants.ETSI_NFV_SOL001_VNFD_TYPES.contains(importFileName)) {
                Path nodeTypePath = Paths.get(descriptorPath.getParent().toString(), importFileName);
                return VnfdUtility.validateYamlCanBeParsed(nodeTypePath).toString();
            }
        }
        throw new FailedOnboardingException("Unable to locate node type file for multiple file descriptor");
    }

    @SuppressWarnings("unchecked")
    private static void setDescriptorDetailsUsingNodeProperties(final VnfDescriptorDetails descriptorDetails, final NodeProperties nodeProperties) {
        LOGGER.info("Started setting descriptor details: {} using node properties: {}", descriptorDetails, nodeProperties);
        descriptorDetails.setVnfDescriptorId((String) (nodeProperties.getDescriptorId().getDefaultValue()));
        descriptorDetails.setVnfDescriptorVersion((String) (nodeProperties.getDescriptorVersion().
                getDefaultValue()));
        descriptorDetails.setVnfProvider((String) (nodeProperties.getProvider().getDefaultValue()));
        descriptorDetails.setVnfProductName((String) (nodeProperties.getProductName().getDefaultValue()));
        descriptorDetails.setVnfSoftwareVersion((String) (nodeProperties.getSoftwareVersion().
                getDefaultValue()));
        descriptorDetails.setVnfmInfo(((List<String>) (nodeProperties.getVnfmInfo().getDefaultValue())).get(0));
        LOGGER.info("Completed setting descriptor details: {} using node properties: {}", descriptorDetails, nodeProperties);
    }

    @Override
    public VnfDescriptorDetails validateAndGetVnfDescriptorDetails(final Map<String, Path> artifactPaths) {
        Path descriptorPath = artifactPaths.get(ENTRY_DEFINITIONS);
        LOGGER.info("Started validating and getting VNF Descriptor details from: {}", descriptorPath);
        Optional<Path> csarBasePathOpt = Optional.ofNullable(artifactPaths.getOrDefault(Constants.CSAR_DIRECTORY, null));
        JSONObject vnfd = getVnfDescriptor(descriptorPath);

        if (VnfdUtility.isNestedVnfd(vnfd)) {
            VnfDescriptorDetails descriptorDetails = new VnfDescriptorDetails();
            descriptorDetails.setDescriptorModel(getNodeTypeFileJsonContent(vnfd, descriptorPath));
            ParentVnfd parentVnfd = NestedVnfdUtility.createNestedVnfdObjects(descriptorPath, vnfd, descriptorDetails);
            setDescriptorDetailsUsingNodeProperties(descriptorDetails, parentVnfd.getNode().getNodeType().getNodeProperties());
            LOGGER.info("Completed validating and getting VNF Descriptor details: {} from: {}.", descriptorDetails.getVnfDescriptorId(),
                    descriptorPath);
            return descriptorDetails;
        }

        VnfdUtility.validateNodeType(vnfd);
        PolicyUtility.validateVnfPackageChangePoliciesSelectorsAndPatterns(vnfd);
        JSONObject nodeType = vnfd.getJSONObject(NODE_TYPES_KEY);
        NodeProperties nodeProperties = VnfdUtility.validateAndGetNodeProperties(nodeType);
        VnfDescriptorDetails vnfDescriptorDetails = VnfdUtility.buildVnfDescriptorDetails(vnfd);
        setDescriptorDetailsUsingNodeProperties(vnfDescriptorDetails, nodeProperties);
        vnfDescriptorDetails.setDescriptorModel(getOrderedDescriptorModel(descriptorPath));

        csarBasePathOpt.ifPresent(csarBasePath -> validateVnfdIfMappingFileIsPresent(
                vnfd, vnfDescriptorDetails.getDefaultFlavour(), csarBasePath, artifactPaths));

        csarBasePathOpt.ifPresent(csarBasePath -> VnfdUtility.validateAspects(vnfd, csarBasePath));

        LOGGER.info("Completed validating and getting VNF Descriptor details: {} from: {}.",
                vnfDescriptorDetails.getVnfDescriptorId(), descriptorPath);
        return vnfDescriptorDetails;
    }

    @Override
    public JSONObject getVnfDescriptor(final Path descriptorPath) {
        LOGGER.info("Started getting VNF Descriptor from: {}", descriptorPath);
        JSONObject vnfd = VnfdUtility.validateYamlCanBeParsed(descriptorPath);
        if (VnfdUtility.isNestedVnfd(vnfd)) {
            throw new UnsupportedOperationException(
                    "Failed to get VNF Descriptor from: %s. Nested vnfd is currently unsupported".formatted(descriptorPath));
        }
        LOGGER.info("Completed getting VNF Descriptor from: {}", descriptorPath);
        return vnfd;
    }

    @Override
    public String getOrderedDescriptorModel(Path descriptorPath) {
        if (!descriptorPath.toFile().exists()) {
            throw new IllegalArgumentException("Vnfd is not present in the path %s".formatted(descriptorPath));
        }
        Map<String, Object> vnfdMap = VnfdUtility.validateYamlAndConvertToMap(descriptorPath);
        ObjectMapper mapper = new ObjectMapper();

        /* Used JsonNode to avoid mixing order of additional params in the descriptor model*/
        return mapper.valueToTree(vnfdMap).toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public String getDescriptorId(final JSONObject vnfd) {
        HashMap<String, Object> parentNode = (HashMap<String, Object>) vnfd.getJSONObject(NODE_TYPES_KEY)
                .toMap()
                .entrySet()
                .stream()
                .findFirst()
                .get()
                .getValue();
        List<String> routeKeys = List.of(PROPERTIES_KEY, Constants.DESCRIPTOR_ID_KEY, DEFAULT_KEY);
        int el = 0;
        while (parentNode != null && !parentNode.containsKey(DEFAULT_KEY)) {
            parentNode = (HashMap<String, Object>) getValueByKeyOrThrowException(parentNode, routeKeys.get(el));
            el++;
        }
        return (String) getValueByKeyOrThrowException(parentNode, DEFAULT_KEY);
    }

    @SuppressWarnings("rawtypes")
    private Object getValueByKeyOrThrowException(HashMap<String, Object> parentNode, String key) {
        if (parentNode != null && !parentNode.isEmpty() && parentNode.containsKey(key)) {
            return parentNode.get(key).getClass() == HashMap.class
                ? (HashMap) parentNode.get(key)
                : (String) parentNode.get(key);
        } else {
            throw new RuntimeException(format(Constants.NO_VALUE_PRESENT_EXCEPTION, key));
        }
    }

    @Override
    public Path createVnfdZip(String packageId) {
        try {
            //Vnfd zip content should not be null, It is handled in the migration script to create the
            // bytearray and store it in database
            byte[] vnfdZipForPackageId = appPackageRepository.findVnfdZipForPackageId(packageId);
            return fileService.createFileFromByte(vnfdZipForPackageId, "vnfd.zip");
        } catch (IOException ex) {
            throw new UnhandledException(ex.getMessage(), ex);
        }
    }

    @Override
    @SuppressWarnings("squid:S4248")
    public String getVnfdFileName(String metaData, String packageId) {
        if (Strings.isNullOrEmpty(metaData)) {
            LOGGER.warn("TOSCA.meta file missing for package id {}", packageId);
            return null;
        }
        String[] allLines = null;
        if (metaData.contains("\\n")) {
            allLines = metaData.split("\\n");
        } else {
            String metaDataElementKeyValue = getEntryDefinition(metaData, packageId);
            if (!Strings.isNullOrEmpty(metaDataElementKeyValue)) {
                return metaDataElementKeyValue;
            }
        }
        if (allLines == null || allLines.length == 0) {
            LOGGER.warn("Invalid TOSCA.meta file provided for package id {}", packageId);
            return null;
        }
        for (String metaDataElement : allLines) {
            String metaDataElementKeyValue = getEntryDefinition(metaDataElement, packageId);
            if (!Strings.isNullOrEmpty(metaDataElementKeyValue)) {
                return metaDataElementKeyValue;
            }
        }
        return null;
    }

    @Override
    public Path createVnfdYamlFile(String jsonVnfd) {
        if (Strings.isNullOrEmpty(jsonVnfd)) {
            LOGGER.error(Constants.VNFD_CANT_BE_EMPTY_ERROR_MESSAGE);
            throw new UnhandledException(Constants.VNFD_CANT_BE_EMPTY_ERROR_MESSAGE);
        }
        try {
            JsonNode jsonNodeTree = new ObjectMapper().readTree(jsonVnfd);
            String jsonAsYaml = new YAMLMapper().writeValueAsString(jsonNodeTree);
            String directoryName = UUID.randomUUID().toString();
            Path directory = fileService.createDirectory(directoryName);
            return fileService.storeFile(new ByteArrayInputStream(jsonAsYaml.getBytes(StandardCharsets.UTF_8)), directory, Constants.VNFD_FILE_NAME);
        } catch (JsonProcessingException jme) {
            LOGGER.error(format(INVALID_VNFD_JSON_ERROR_MESSAGE, jme.getMessage()), jme);
            throw new UnhandledException(format(INVALID_VNFD_JSON_ERROR_MESSAGE, jme.getMessage()), jme);
        }
    }

    @Override
    public Path createVnfdYamlFileByPackageId(String packageId) {
        return createVnfdYamlFile(fetchVnfd(packageId));
    }

    @Override
    public Path getVnfdYamlFileFromZip(Path zipFile,
                                       int unpackTimeout,
                                       String packageId) {
        if (zipFile == null) {
            LOGGER.warn(Constants.VNFD_CANT_BE_EMPTY_ERROR_MESSAGE);
            return createVnfdYamlFileByPackageId(packageId);
        }
        fileService.unpack(zipFile, unpackTimeout);
        Path directoryPath = zipFile.getParent();
        try (Stream<Path> walk = Files.walk(directoryPath)) {
            List<Path> result = walk.filter(Files::isRegularFile).toList();
            if (result.isEmpty()) {
                LOGGER.warn("Vnfd not found in the zip file for package id {}", packageId);
                return createVnfdYamlFileByPackageId(packageId);
            }
            byte[] artifact = fetchArtifact(packageId, RELATIVE_PATH_TO_TOSCA_META_FILE);
            String metaData = new String(artifact, StandardCharsets.UTF_8);
            String entryDefinitionFileName = getVnfdFileName(metaData, packageId);
            if (Strings.isNullOrEmpty(entryDefinitionFileName)) {
                LOGGER.warn("Unable to locate Entry-Definitions in meta file for package id {}", packageId);
                return createVnfdYamlFileByPackageId(packageId);
            }
            for (Path path : result) {
                if (path.getFileName().toString().equals(entryDefinitionFileName)) {
                    return Files.move(path, path.resolveSibling(Constants.VNFD_FILE_NAME));
                }
            }
        } catch (final Exception ex) {
            LOGGER.error("Got exception when fetching the TOSCA.meta file %s".formatted(ex.getMessage()), ex);
        }
        return createVnfdYamlFileByPackageId(packageId);
    }

    @Override
    public byte[] fetchArtifact(String vnfPkgId, String artifactPath) {
        AppPackage appPackage = databaseService.getAppPackageByIdNotBeingDeleted(vnfPkgId);
        packageValidator.validateIsAppPackageOnboardedState(appPackage);
        AppPackageArtifacts appPackageArtifacts = appPackageArtifactsRepository.findByAppPackageAndArtifactPath(appPackage, artifactPath)
                .orElseThrow(() -> new ArtifactNotFoundException(format(Constants.ARTIFACT_NOT_PRESENT_ERROR_MESSAGE, artifactPath)));
        return appPackageArtifacts.getArtifact();
    }

    @Override
    public void deleteVnfdZipFile(Path zipFile) {
        if (zipFile != null) {
            fileService.deleteDirectory(zipFile.getParent().toString());
        }
    }

    @Override
    public void deleteVnfdYamlFile(Path yamlFile) {
        if (yamlFile != null) {
            fileService.deleteDirectory(yamlFile.getParent().toString());
        }
    }

    @Override
    public boolean isMultipleFileVnfd(String packageId) {
        return appPackageRepository.findIsMultipleVnfdFileForPackageId(packageId);
    }

    @SuppressWarnings("squid:S4248")
    private static String getEntryDefinition(String metaDataElement, String packageId) {
        String[] metaDataElementKeyValue = metaDataElement.split(":");
        if (metaDataElementKeyValue.length != 2) {
            LOGGER.warn("Invalid key or value in the TOSCA.meta file for package id {}", packageId);
            return null;
        }
        if (ENTRY_DEFINITIONS.equals(metaDataElementKeyValue[0])) {
            if (metaDataElementKeyValue[1].contains("/")) {
                String[] entryDefinitionFile = metaDataElementKeyValue[1].split("/");
                return entryDefinitionFile[entryDefinitionFile.length - 1];
            } else {
                return metaDataElementKeyValue[1];
            }
        }
        return null;
    }

    private String fetchVnfd(String vnfdId) {
        return databaseService.getAppPackageByIdNotBeingDeleted(vnfdId).getDescriptorModel();
    }
}