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
package com.ericsson.amonboardingservice.utils;

import java.util.List;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

    public final String INVALID_CREDENTIALS_ERROR_MESSAGE = "Invalid username and password provided";

    public final String INVALID_RESPONSE_ERROR_MESSAGE = "Invalid response from server";

    public final String NO_DATA_FOUND_ERROR_MESSAGE = "No data Found";

    public final String DOCKER_IMAGE_NOT_PRESENT_ERROR_MESSAGE = "Docker image %s is not present in CSAR package";

    public final String DOCKER_REGISTRY_PUSH_FAIL_MESSAGE
            = "Failed to push layer %s with size %s to repo %s in docker registry";

    public final String PACKAGE_NOT_PRESENT_ERROR_MESSAGE = "Package with id: %s not found";

    public final String ONBOARDING_DETAIL_NOT_PRESENT_ERROR_MESSAGE = "Unexpected problem: onboarding details for app package %s does not exist";

    public final String PARSING_VNFD_FOR_ADDITIONAL_ATTRIBUTES_MESSAGE
            = "Package with id %s not uploaded to Tosca O. Parsing VNFD for additional attributes for operation %s";

    public final String FAILED_TO_EXTRACT_LAYER_FROM_DOCKER_TAR_FILE_MESSAGE
            = "Failed to extract layer from docker tar file, failed due to timeout: %s";

    public final String FAILED_TO_UNPACK_FILE_FROM_ARCHIVE_MESSAGE
            = "Failed to unpack file %s, failed due to timeout: %s";

    public final String FAILED_TO_EXECUTE_COMMAND_MESSAGE
            = "Failed to execute command: %s due to: %s,\nerror details: %s";

    public final String VNFD_FILE_IS_NOT_FOUND_FOR_PACKAGE_ERROR_MESSAGE
            = "VNFD file is not found for package id %s";
    public final int ALLOWED_NUMBER_OF_PACKAGES = 5;
    public final String ILLEGAL_NUMBER_OF_RESOURCES_ERROR_MESSAGE =
            "The request has been declined due to exceeding the limit of " + ALLOWED_NUMBER_OF_PACKAGES +
                    " packages that can be uploaded without the license permission for onboarding or without the NeLs service.";
    public final String PROJECT_AND_IMAGE_NAME_SEPARATOR = "/";

    public final String REPOSITORY_KEY = "repositories";

    public final String IMAGE_NAME_KEY = "name";

    public final String IMAGE_TAG_KEY = "tags";

    public final String IMPORTS_KEY = "imports";

    public final String TYPE = "type";

    public final String SOFTWARE_VERSION = "softwareVersion";

    public final String PACKAGE_VERSION = "packageVersion";

    public final String PROVIDER = "provider";

    public final String PAGE_NUMBER = "pageNumber";

    public final String PAGE_SIZE = "pageSize";

    public final String SCALING_MAPPING = "scaling_mapping";

    public final String TYPE_BLANK = "about:blank";

    public final String CSAR_DIRECTORY = "csarDirectory";

    public final String DOCKER_LAYER_MEDIA_TYPE_TAR = "application/vnd.docker.image.rootfs.diff.tar.gzip";

    public final String DOCKER_LAYER_MEDIA_TYPE_JSON = "application/vnd.docker.container.image.v1+json";

    public final String DOCKER_LAYER_CONTENT_TYPE = "application/vnd.docker.distribution.manifest.v2+json";

    public final String DOCKER_ARCHIVE_EXTENSION = ".tar";

    public final String CHART_ARCHIVE_EXTENSION = ".tgz";

    public final String ZIP_ARCHIVE_EXTENSION = ".zip";

    public final String CSAR_ARCHIVE_EXTENSION = ".csar";

    public final String CERT_ARCHIVE_EXTENSION = ".cert";

    public final String CMS_ARCHIVE_EXTENSION = ".cms";

    public final String VNF_PACKAGE_ZIP = "vnfPackage.zip";

    public final String MANIFEST_JSON = "manifest.json";

    public final List<String> ETSI_NFV_SOL001_VNFD_TYPES = List.of("etsi_nfv_sol001_vnfd_2_5_1_types.yaml",
                                                                   "etsi_nfv_sol001_vnfd_3_3_1_types.yaml");

    public final String DEFAULT_PACKAGE_PROVIDER_NAME = "New";

    public final String DEFAULT_PACKAGE_PRODUCT_NAME = "package";

    public final String DEFAULT_PACKAGE_DESCRIPTOR_VERSION = "";

    public final String ERROR_DETAILS_TEMPLATE = "Can not onboard package: %s failed";

    public final String HELM_CHART_ALREADY_PRESENT_MESSAGE
            = "%s of the same version already exists in the helm chart registry, " +
            "but digest of the helm chart in package being onboarded is different. " +
            "Version of helm chart must be incremented if its content changed.";
    public final String PACKAGE_WITH_SAME_DESCRIPTOR_ERROR_MESSAGE = "%s vnfdId is already present";
    public final String VNFD_CANT_BE_EMPTY_ERROR_MESSAGE = "Vnfd can't be null or empty";
    public final String INVALID_VNFD_JSON_ERROR_MESSAGE
            = "Invalid vnfd json present in package, Failed due to %s";

    public final String VNFD_FILE_NAME = "cnf_vnfd.yaml";
    public final String VNF_DESCRIPTOR_MUST_NOT_BE_NULL = "Vnfd descriptor must not be null";
    public final String VNF_DESCRIPTOR_DOES_NOT_CONTAIN_DEFINITIONS_VERSION_FIELD
            = "Vnf descriptor does not contain tosca_definitions_version field";
    public final String NO_VALUE_PRESENT_EXCEPTION = "No %s value present in VNFD";

    public final String DESCRIPTOR_ID_KEY = "descriptor_id";

    public final String ARTIFACT_NOT_PRESENT_ERROR_MESSAGE = "ArtifactPath: %s not found";

    public final String TOSCA_DEFINITIONS_VERSION = "tosca_definitions_version";

    public final String TOSCA_1_3_DEFINITIONS_VERSION = "tosca_simple_yaml_1_3";

    public final String TOSCA_1_2_DEFINITIONS_VERSION = "tosca_simple_yaml_1_2";

    public final String HELM_CHART_NOT_PRESENT = "Helm chart : %s is not present in the mentioned path : %s.";
    public final String ZIP_SLIP_ATTACK_ERROR_MESSAGE
            = "CSAR file contains relative path structure (../), Relative path in CSAR is not supported";
    public final String USER_DEFINED_DATA_INVALID_FORMAT
            = "Invalid data provided in the userDefinedData, Only key value pair is supported.";

    public final String VALID_VNFD_FILE = "vnfd/valid_vnfd.yaml";

    public final String PATH_TO_PACKAGE = "pathToPackage";

    public final int MAX_SIZE_FILE_MULTIPLIER = 3;

    public final String OPERATION_NOT_SUPPORTED = "%s operation not supported";

    public final String ONBOARDING_TIMEOUT = "onboarding.timeout";
    public final String USER_DEFINED_FILENAME = "fileName";

    public final String VNFD_CONFLICT_PACKAGE_STATE = "ID: %s is not in %s state";

    public final String VNF_PACKAGE_WITH_ID_DOES_NOT_EXIST = "Vnf package with id %s does not exist.";
    public final String INVALID_PACKAGE_USAGE_STATE
            = "Invalid package usage state. Usage state must be set to NOT_IN_USE.";

    public final String SKIP_IMAGE_UPLOAD = "skipImageUpload";

    public final String IDEMPOTENCY_KEY_HEADER = "Idempotency-key";

    public final String SERVICE_UNAVAILABLE_EXCEPTION = "503 Service Unavailable";
}