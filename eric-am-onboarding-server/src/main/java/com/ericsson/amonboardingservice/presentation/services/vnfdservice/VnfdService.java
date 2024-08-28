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

import com.ericsson.am.shared.vnfd.model.VnfDescriptorDetails;
import org.json.JSONObject;

import java.nio.file.Path;
import java.util.Map;

public interface VnfdService {

    VnfDescriptorDetails validateAndGetVnfDescriptorDetails(Map<String, Path> artifactPaths);

    JSONObject getVnfDescriptor(Path descriptorPath);

    String getOrderedDescriptorModel(Path descriptorPath);

    String getDescriptorId(JSONObject vnfd);

    String getVnfdFileName(String metaData, String packageId);

    /**
     * This method is used to create a zip file for the provided yaml file
     *
     * @param packageId
     * @return returns path to a zip file
     */
    Path createVnfdZip(String packageId);

    /**
     * Fetches the vnfd and converts it to a yaml file and store it in local file system
     *
     * @param jsonVnfd
     * @return Path to vnfdFile
     */
    Path createVnfdYamlFile(String jsonVnfd);

    /**
     * Fetches the vnfd and converts it to a yaml file and store it in local file system
     *
     * @param vnfPkgId vnf package id
     * @return Path to vnfdFile
     */
    Path createVnfdYamlFileByPackageId(String vnfPkgId);

    /**
     * Method to get the vnfd yaml for the zip file if vnfd is not available in the zip file then the
     * vnfd is fetched in the old way (e.g. converting from JSON to YAML from vnfdModel)
     *
     * @param zipFile
     * @param unpackTimeout
     * @param packageId
     * @return path of the vnfd file
     */
    Path getVnfdYamlFileFromZip(Path zipFile,
                                int unpackTimeout,
                                String packageId);

    byte[] fetchArtifact(String vnfPkgId, String artifactPath);

    /**
     * Deletes the locally created zip file
     *
     * @param zipFile
     */
    void deleteVnfdZipFile(Path zipFile);

    /**
     * Deletes the locally created yaml file
     *
     * @param yamlFile
     */
    void deleteVnfdYamlFile(Path yamlFile);

    /**
     * Checks if the package contains multiple vnfd
     *
     * @param packageId
     * @return boolean, true if the package is multiple file vnfd
     */
    boolean isMultipleFileVnfd(String packageId);

}
