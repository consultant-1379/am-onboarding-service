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

import static java.nio.file.Files.createFile;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.walk;
import static java.util.Collections.reverseOrder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import jakarta.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.input.MessageDigestCalculatingInputStream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.amonboardingservice.presentation.exceptions.CommandTimedOutException;
import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.utils.Constants;
import com.ericsson.amonboardingservice.utils.executor.ProcessExecutor;
import com.ericsson.amonboardingservice.utils.executor.ProcessExecutorResponse;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteStreams;

import lombok.extern.slf4j.Slf4j;

/**
 * Working with local files and directories
 */
@Slf4j
@Service
public class FileSystemService implements FileService {
    private static final String TEMP_FILE_ZIP_NAME = "temp_file.zip";
    private static final String RELATIVE_PATH_STRING = "../";
    private static final String UUID_PATTERN = "^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}$";
    private static final FileFilter TMP_DIRECTORY_FILTER = DirectoryFileFilter.DIRECTORY.and(new RegexFileFilter(UUID_PATTERN));

    @Autowired
    private ProcessExecutor processExecutor;

    @Value("${vnf.packages.root}")
    private String rootDirectory;

    @Value("${chart.sub.dir}")
    private String chartSubDir;

    @Value("${chart.file.ext}")
    private String chartExt;

    @Value("${docker.sub.dir}")
    private String dockerSubDir;

    @Value("${docker.file.ext}")
    private String dockerExt;

    public void setRootDirectory(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public void unpack(Path fileToUnpack, final int onboardingTimeout) {
        LOGGER.info("Going to unpack archive entries in {}:", fileToUnpack);
        String command = String.format("unzip %s -d %s", fileToUnpack.toString(), fileToUnpack.getParent().toString());
        try {
            final ProcessExecutorResponse response = processExecutor.executeProcessBuilder(command, onboardingTimeout);
            checkForErrorProcessOutput(response, command);
        } catch (CommandTimedOutException e) {
            throw new InternalRuntimeException(String.format(Constants.FAILED_TO_UNPACK_FILE_FROM_ARCHIVE_MESSAGE,
                                                             fileToUnpack, onboardingTimeout), e);
        } finally {
            deleteFile(fileToUnpack);
        }
        LOGGER.info("Completed unpacking archive entries in: {}", fileToUnpack);
    }

    @Override
    public boolean hasRelativePathContent(Path zipFile, final int onboardingTimeout) {
        LOGGER.info("Going to validate zip file for relative path {}:", zipFile);
        String command = String.format("unzip -l %s | grep %s", zipFile.toString(), RELATIVE_PATH_STRING);
        try {
            ProcessExecutorResponse response = processExecutor.executeProcessBuilder(command, onboardingTimeout);
            checkForErrorProcessOutput(response, command);
            return response.getCmdResult() != null && response.getCmdResult().contains(RELATIVE_PATH_STRING);
        } catch (CommandTimedOutException e) {
            throw new InternalRuntimeException(String.format(Constants.FAILED_TO_UNPACK_FILE_FROM_ARCHIVE_MESSAGE, zipFile, onboardingTimeout), e);
        }
    }

    private static void deleteFile(Path fileName) {
        try {
            LOGGER.info("Started deletion of file: {}", fileName);
            Files.delete(fileName);
            LOGGER.info("Completed deletion of file: {}", fileName);
        } catch (IOException e) {
            String message = String.format("Failed to delete file %s", fileName);
            LOGGER.warn(message, e);
        }
    }

    @Override
    public Path createDirectory(final String directoryName) {
        Path directoryPath = getPath(directoryName);
        try {
            Files.createDirectory(directoryPath);
        } catch (IOException e) {
            throw new InternalRuntimeException("Failed to create directory with name: " + directoryName, e);
        }
        return directoryPath;
    }

    @Override
    public Path storeFile(final InputStream inputStream, final Path directoryPath, String fileName) {
        final Path packagePath = create(directoryPath, fileName);
        try (
                InputStream dataBuffer = new BufferedInputStream(inputStream);
                OutputStream fileOutputStream = Files.newOutputStream(packagePath);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream)
        ) {
            ByteStreams.copy(dataBuffer, bufferedOutputStream);
            bufferedOutputStream.flush();
        } catch (IOException e) {
            throw new InternalRuntimeException(String.format("Failed to store file %s in directory %s due to: ", fileName, directoryPath), e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return packagePath;
    }

    @Override
    public Path storeFile(final MultipartFile zipFile, final Path directoryPath, String fileName) {
        final Path packagePath = create(directoryPath, fileName);
        try {
            zipFile.transferTo(packagePath.toFile());
        } catch (IOException e) {
            throw new InternalRuntimeException(String.format("Failed to store file %s in directory %s due to: ", fileName, directoryPath), e);
        }
        return packagePath;
    }

    public Path create(final Path directoryPath, String fileName) {
        Path directory;
        try {
            directory = createFile(directoryPath.resolve(fileName));
        } catch (IOException e) {
            LOGGER.error(e.toString(), e);
            throw new InternalRuntimeException(String.format("Failed to create file %s in directory %s due to: ", fileName, directoryPath), e);
        }
        return directory;
    }

    @Override
    public void deleteDirectory(final String directoryName) {
        deleteDirectory(getPath(directoryName));
    }

    @Override
    public void deleteTempDirectories() {
        final Path directory = Paths.get(rootDirectory);

        LOGGER.info("Deleting temp directories from root directory {}", directory.toAbsolutePath());

        final File[] tempDirectories = directory.toFile().listFiles(TMP_DIRECTORY_FILTER);
        if (tempDirectories == null) {
            return;
        }

        Arrays.stream(tempDirectories)
                .map(File::toPath)
                .forEach(FileSystemService::deleteDirectory);
    }

    private static void deleteDirectory(final Path directoryPath) {
        LOGGER.debug("Deleting directory {}", directoryPath.toAbsolutePath());
        if (directoryPath.toFile().exists()) {
            try (Stream<Path> walk = walk(directoryPath)) {
                walk.sorted(reverseOrder()).forEach(deleteFile());
            } catch (IOException e) {
                LOGGER.error(String.format("Failed to delete directory %s due to: ", directoryPath), e);
            }
        }
    }

    @Override
    public void deleteFile(final File existingFile) {
        try {
            LOGGER.info("Started deletion of file: {}", existingFile);
            if (existingFile != null && existingFile.exists()) {
                delete(existingFile.toPath());
            } else {
                LOGGER.warn("File {} is not present in local filesystem, Hence skipping deletion", existingFile);
            }
            LOGGER.info("Completed deletion of file: {}", existingFile);
        } catch (IOException e) {
            LOGGER.error(String.format("Failed to delete the downloaded file %s", existingFile), e);
        }
    }

    @Override
    public Optional<Path> getChartPath(Path parentDirectory) {
        return getFilePathWithinExtractedPath(parentDirectory, chartSubDir, chartExt);
    }

    @Override
    public Optional<Path> getDockerPackagePath(Path parentDirectory) {
        return getFilePathWithinExtractedPath(parentDirectory, dockerSubDir, dockerExt);
    }

    @VisibleForTesting
    public static Optional<Path> getFilePathWithinExtractedPath(Path parentDirectory, String subDirectory,
                                                                String extension) {
        File parentDir = parentDirectory.toFile();
        File directory = new File(parentDir, subDirectory);
        if (!directory.isDirectory()) {
            return Optional.empty();
        }
        return Arrays.stream(Objects.requireNonNull(directory.listFiles()))
                .filter(file -> file.toString().contains(extension))
                .findFirst()
                .map(File::toPath);
    }

    @Override
    public String readFile(final Path filePath) {
        if (filePath != null && filePath.toFile().exists()) {
            StringBuilder contentBuilder = new StringBuilder();
            try (Stream<String> stream
                    = Files.lines(filePath, StandardCharsets.UTF_8)) {
                stream.forEach(s -> contentBuilder.append(s).append("\n"));
                return contentBuilder.toString();
            } catch (final IOException ioe) {
                throw new InternalRuntimeException(String.format("Unable to read the file %s", filePath), ioe);
            }
        } else {
            return StringUtils.EMPTY;
        }
    }

    @Override
    public InputStream readFileAsFileStream(final Path path) {
        try {
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new InternalRuntimeException(String.format("Unable to read the file %s", path.getFileName()), e);
        }
    }

    @Override
    public Path createTempPath(final String filename) {
        final Path temporaryPath;
        try {
            temporaryPath = Paths.get(rootDirectory).resolve("temporary/" + UUID.randomUUID() + "/");
            return Files.createDirectories(temporaryPath).resolve(filename);
        } catch (IOException e) {
            throw new InternalRuntimeException(String.format("Failed to create temporary directory for file name %s", filename), e);
        }
    }

    @Override
    public void repackCsarWithoutImagesAndCharts(Path sourceDir, Path destZipFile) throws IOException {
        Files.deleteIfExists(destZipFile);
        Files.createFile(destZipFile);
        try (ZipOutputStream zipOutStream = new ZipOutputStream(Files.newOutputStream(destZipFile))) {
            walkFiles(sourceDir, zipOutStream);
        }
    }

    @Override
    public Path zipDirectory(Path directoryToZip) throws IOException {
        String directoryName = UUID.randomUUID().toString();
        Path directory = createDirectory(directoryName);
        Path newZipFile = Paths.get(directory.toString(), TEMP_FILE_ZIP_NAME);
        try (ZipOutputStream zipOutStream = new ZipOutputStream(Files.newOutputStream(newZipFile))) {
            walkFiles(directoryToZip, zipOutStream);
        }
        return newZipFile;
    }

    @Override
    public Path createFileFromByte(byte[] fileData, String fileName) throws IOException {
        String directoryName = UUID.randomUUID().toString();
        Path directory = createDirectory(directoryName);
        Path newFile = Paths.get(directory.toString(), fileName);
        return Files.write(newFile, fileData);
    }

    @Override
    public byte[] getFileContent(final Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new InternalRuntimeException(String.format("Error happened during extracting content from the file %s. Details: %s", path,
                                                             e.getMessage()), e);
        }
    }

    @Override
    public Optional<Path> getFileByExtension(Path directoryPath, String fileExtension) {
        LOGGER.info("Started getting file by extension {} from directory {}", fileExtension, directoryPath);

        final File directoryFile = directoryPath.toFile();

        for (File file : Objects.requireNonNull(directoryFile.listFiles())) {
            final String fileName = file.toString();
            if (fileName.endsWith(fileExtension)) {
                return Optional.of(Paths.get(fileName));
            }
        }

        LOGGER.info("No suitable file is found by extension {} in directory {}", fileExtension, directoryFile);

        return Optional.empty();
    }

    @Override
    public String getFileNameWithoutExtension(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    @Override
    public String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf('.'));
    }

    @Override
    public Path extractFileFromTar(Path pathToTar, Path pathToExtract, String fileName, int onboardingTimeout) {
        String directory = pathToExtract.toString();
        String command = String.format("tar -C %s --extract --no-same-owner --file=%s %s", directory, pathToTar, fileName);
        try {
            final ProcessExecutorResponse response = processExecutor.executeProcessBuilder(command, onboardingTimeout);
            checkForErrorProcessOutput(response, command);
            return Paths.get(directory, fileName);
        } catch (CommandTimedOutException e) {
            throw new InternalRuntimeException(String.format(Constants.FAILED_TO_EXTRACT_LAYER_FROM_DOCKER_TAR_FILE_MESSAGE, onboardingTimeout), e);
        }
    }

    @Override
    public boolean isFileExist(final Path filePath) {
        return filePath.toFile().exists();
    }

    @Override
    public void validateCsarForRelativePaths(Path csarPath, final int onboardingTimeout) {
        if (hasRelativePathContent(csarPath, onboardingTimeout)) {
            deleteDirectory(csarPath.getParent().toString());
            throw new IllegalArgumentException(Constants.ZIP_SLIP_ATTACK_ERROR_MESSAGE);
        }
    }

    @Override
    public String generateHash(final Path artifact, final String algorithm) {
        try (MessageDigestCalculatingInputStream stream =
                new MessageDigestCalculatingInputStream(new BufferedInputStream(Files.newInputStream(artifact)),
                                                        algorithm)) {
            stream.consume();
            byte[] hash = stream.getMessageDigest().digest();
            return DatatypeConverter.printHexBinary(hash).toLowerCase();
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new InternalRuntimeException(
                    String.format("Failed to generate digest for %s due to %s", artifact.getFileName().toString(), e.getMessage()), e);
        }
    }

    @Override
    public boolean isEnoughSpaceInRootDirectory(long fileSize) {
        final long requiredSpace = fileSize * Constants.MAX_SIZE_FILE_MULTIPLIER;
        long freeSpace = new File(rootDirectory).getFreeSpace();
        LOGGER.info("Space required to onboard csar with size {} is {}. There are {} available for onboarding",
                    FileUtils.byteCountToDisplaySize(fileSize),
                    FileUtils.byteCountToDisplaySize(requiredSpace),
                    FileUtils.byteCountToDisplaySize(freeSpace));
        return freeSpace > requiredSpace;
    }

    private Path getPath(final String directoryName) {
        return Paths.get(rootDirectory).resolve(directoryName);
    }

    private static Consumer<Path> deleteFile() {
        return file -> {
            try {
                delete(file);
            } catch (IOException e) {
                LOGGER.error(String.format("Failed to delete the downloaded file %s", file), e);
            }
        };
    }

    private static void walkFiles(final Path sourceDir, final ZipOutputStream zipOutStream) throws IOException {
        try (Stream<Path> stream = Files.walk(sourceDir)) {
            stream.filter(path -> !path.toFile().isDirectory())
                    .filter(path -> !path.toString().endsWith(Constants.CHART_ARCHIVE_EXTENSION))
                    .filter(path -> !path.toString().endsWith(Constants.DOCKER_ARCHIVE_EXTENSION))
                    .filter(path -> !path.toString().endsWith(Constants.ZIP_ARCHIVE_EXTENSION))
                    .filter(path -> !path.toString().endsWith(Constants.CSAR_ARCHIVE_EXTENSION))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(sourceDir.relativize(path).toString());
                        addToZip(zipOutStream, path, zipEntry);
                    });
        }
    }

    private static void addToZip(final ZipOutputStream zipOutStream, final Path path, final ZipEntry zipEntry) {
        try {
            zipOutStream.putNextEntry(zipEntry);
            Files.copy(path, zipOutStream);
            zipOutStream.closeEntry();
        } catch (IOException e) {
            throw new InternalRuntimeException(String.format("Failed to add file %s to zip %s", zipEntry, path), e);
        }
    }

    private void checkForErrorProcessOutput(ProcessExecutorResponse processExecutorResponse, String command) {
        if (!processExecutorResponse.isProcessTerminatedNormally()) {
            final String errorMessage = String.format(Constants.FAILED_TO_EXECUTE_COMMAND_MESSAGE, command,
                                                      processExecutorResponse.getCmdResult(), processExecutorResponse.getCmdErrorResult());
            if (processExecutorResponse.getExitValue() == 9) {
                throw new IllegalArgumentException(errorMessage);
            } else {
                throw new InternalRuntimeException(errorMessage);
            }
        }
    }
}