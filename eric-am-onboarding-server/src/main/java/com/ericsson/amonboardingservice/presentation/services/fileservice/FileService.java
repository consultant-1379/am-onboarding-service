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
package com.ericsson.amonboardingservice.presentation.services.fileservice;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Service for working with files and directories
 */
public interface FileService {

    /**
     * Unpacks the zip file at the specified path
     *
     * @param fileToUnpack
     * @param onboardingTimeout
     */
    void unpack(Path fileToUnpack, int onboardingTimeout);

    /**
     * Checks if the zip file has a relative path
     *
     * @param zipFile
     * @param onboardingTimeout
     * @return boolean true if it has a relative path, False if does not have a relative path
     */
    boolean hasRelativePathContent(Path zipFile, int onboardingTimeout);

    /**
     * Repack a CSAR file but leave out the helm charts tgz files and the docker tar files.
     *
     * @param sourceDir
     *         Path to the source directory where the CSAR has been extracted.
     * @param destZipFile
     *         Path to the new CSAR file.
     *
     * @throws IOException
     */
    void repackCsarWithoutImagesAndCharts(Path sourceDir, Path destZipFile) throws IOException;

    /**
     * Creates directory with the specified name
     *
     * @param directoryName
     *
     * @return Path to created directory
     * @throws IOException
     */
    Path createDirectory(String directoryName);

    /**
     * Store the contents of the input stream to a file in the specified directory
     *
     * @param inputStream
     * @param directoryPath
     *
     * @return Path to the stored file
     * @throws IOException
     */
    Path storeFile(InputStream inputStream, Path directoryPath, String fileName);
    /**
     * Store the contents of the uploaded file to the specified file
     *
     * @param zipFile
     * @param directoryPath
     * @param fileName
     * @return the path to the stored file
     */
    Path storeFile(MultipartFile zipFile, Path directoryPath, String fileName);

    /**
     * Delete directory
     *
     * @param directoryName
     *         relative to root directory
     */
    void deleteDirectory(String directoryName);

    /**
     * Delete root directory
     */
    void deleteTempDirectories();

    /**
     * Delete the file
     * @param existingFile
     */
    void deleteFile(File existingFile);

    /**
     * Validates a zip file and prevents EVNFM from zip slip attack
     *
     * @param csarPath          path to csar file
     * @param onboardingTimeout
     */
    void validateCsarForRelativePaths(Path csarPath, int onboardingTimeout);

    /**
     * Return Path of Docker File to be loaded
     *
     * @param parentDirectory
     */
    Optional<Path> getDockerPackagePath(Path parentDirectory);

    /**
     * Return Path of Helm Chart to be loaded
     *
     * @param parentDirectory
     */
    Optional<Path> getChartPath(Path parentDirectory);

    /**
     * Return file content of the file

     * @param filePath
     */
    String readFile(Path filePath);

    /**
     * Return File Input Stream

     * @param path
     */
    InputStream readFileAsFileStream(Path path);

    /**
     * Create a temporary path for a file
     * @param filename
     * @return
     */
    Path createTempPath(String filename);

    /**
     * Zips a directory
     *
     * @param directory
     * @return path to zip file
     */
    Path zipDirectory(Path directory) throws IOException;

    /**
     * Finds a file based on file extension
     *
     * @param directoryPath
     * @param fileExtension
     * @return path to file
     */
    Optional<Path> getFileByExtension(Path directoryPath, String fileExtension);

    /**
     * Methods accepts a byte data and returns the path to the file
     *
     * @param fileData
     * @return path, To the zip file
     */
    Path createFileFromByte(byte[] fileData, String fileName) throws IOException;

    /**
     * Get file from path
     * @param path
     * @return file
     */
    byte[] getFileContent(Path path);

    /**
     * Get file name from path without extension
     * @param path
     * @return file name
     */
    String getFileNameWithoutExtension(Path path);

    /**
     * Get file extension from file name
     * @param fileName
     * @return file extension
     */
    String getFileExtension(String fileName);


    /**
     * Extract file from tar
     * @param pathToTar
     * @param fileName
     * @param pathToExtract
     * @param onboardingTimeout
     * @return path
     */
    Path extractFileFromTar(Path pathToTar, Path pathToExtract, String fileName, int onboardingTimeout);

    /**
     * Generates hash for artifact
     * @param artifact
     * @param algorithm
     * @return hash
     */
    String generateHash(Path artifact, String algorithm);

    Path create(Path directoryPath, String fileName);

    boolean isFileExist(Path filePath);

    boolean isEnoughSpaceInRootDirectory(long fileSize);
}
