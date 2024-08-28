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
package com.ericsson.amonboardingservice.presentation.services.manifestservice;

import com.ericsson.amonboardingservice.presentation.exceptions.ErrorMessage;
import com.ericsson.amonboardingservice.presentation.exceptions.FailedOnboardingException;
import com.ericsson.amonboardingservice.presentation.exceptions.FailedOnboardingValidationException;
import com.google.common.annotations.VisibleForTesting;

import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_MANIFEST;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.xml.bind.DatatypeConverter;

import org.apache.commons.io.input.MessageDigestCalculatingInputStream;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ManifestServiceImpl implements ManifestService {
    private static final String SOURCE_MANIFEST_PREFIX = "Source";
    private static final String HASH_MANIFEST_PREFIX = "Hash";
    private static final String ALGORITHM_MANIFEST_PREFIX = "Algorithm";
    public static final String SHA_256 = "SHA-256";
    public static final String SHA_512 = "SHA-512";

    @Override
    public void validateDigests(final Path unpackedDirectory, final Map<String, Path> artifactPaths) {
        LOGGER.info("Validate the artifacts in the package against the digests in the Manifest file, "
                            + "VNF package artifacts: {}, all artifacts in the VNF package: {}", unpackedDirectory, artifactPaths);
        Optional<Path> manifestPath = getPathToManifestFile(artifactPaths);
        if (!manifestPath.isPresent()) {
            LOGGER.warn("Manifest file not found in package: {}, skipping artifact validation", artifactPaths);
            return;
        }
        validateFile(manifestPath.get());
        List<String> relevantLines = getManifestLinesWithHashes(manifestPath.get());
        if (relevantLines.isEmpty()) {
            LOGGER.warn("Manifest file did not contain any digests, skipping artifact validation");
            return;
        }
        List<Digest> digests = createDigests(relevantLines, unpackedDirectory);
        List<ErrorMessage> errors = validateDigests(digests);
        if (!errors.isEmpty()) {
            throw new FailedOnboardingValidationException(errors);
        }
        LOGGER.info("Completed validation of the artifacts in the package against the digests in the Manifest file, "
                            + "VNF package artifacts: {}, all artifacts in the VNF package: {}", unpackedDirectory, artifactPaths);
    }

    @Override
    public boolean compareArtifacts(final Path newArtifact, final Path existingArtifact) {
        final String newHash = generateHash(newArtifact, SHA_512);
        final String existingHash = generateHash(existingArtifact, SHA_512);
        return newHash.equals(existingHash);
    }

    public static String generateHash(final Path artifact, final String algorithm, final boolean catchException) {
        LOGGER.info("Started generating hash for package content path: {}", artifact);
        try (MessageDigestCalculatingInputStream stream =
                new MessageDigestCalculatingInputStream(new BufferedInputStream(Files.newInputStream(artifact)), algorithm)) {
            stream.consume();
            byte[] hash = stream.getMessageDigest().digest();
            LOGGER.info("Completed generating hash for: {}", artifact);
            return DatatypeConverter.printHexBinary(hash);
        } catch (NoSuchAlgorithmException | IOException e) {
            String errorMessage = String.format("Failed to generate digest of file %s due to %s", artifact.getFileName(), e.getCause());
            if (catchException) {
                LOGGER.error(errorMessage, e);
            } else {
                throw new FailedOnboardingException(errorMessage, e);
            }
        }
        return UUID.randomUUID().toString();
    }

    public static String generateHash(final Path artifact, final String algorithm) {
        return generateHash(artifact, algorithm, true);
    }

    private static void validateFile(final Path manifestFile) {
        if (manifestFile.toFile().length() == 0) {
            String errorMessage = String.format("The Manifest file %s was found to be empty", manifestFile);
            LOGGER.error(errorMessage);
            throw new FailedOnboardingValidationException(errorMessage);
        }
    }

    @VisibleForTesting
    Optional<Path> getPathToManifestFile(Map<String, Path> artifacts) {
        LOGGER.info("Started getting path to manifest file from artifacts: {}", artifacts);
        final Path manifestPath = artifacts.get(ENTRY_MANIFEST);
        LOGGER.info("Completed getting path to manifest file from artifacts: {}", artifacts);
        return Optional.ofNullable(manifestPath);
    }

    @VisibleForTesting
    List<String> getManifestLinesWithHashes(final Path path) {
        LOGGER.info("Started getting manifest lines with hashes from: {}", path);
        try (Stream<String> lines = Files.lines(path)) {
            List<String> manifestLinesWithHashes = lines.filter(isDigestLinePredicate()).collect(Collectors.toList());
            LOGGER.info("Completed getting manifest lines with hashes from: {}", path);
            return manifestLinesWithHashes;
        } catch (IOException e) {
            throw new FailedOnboardingValidationException(
                    String.format("There was an issue reading the Manifest file: %s", e.getMessage()), e);
        }
    }

    private static Predicate<String> isDigestLinePredicate() {
        return line -> line.startsWith(SOURCE_MANIFEST_PREFIX)
                || line.startsWith(HASH_MANIFEST_PREFIX)
                || line.startsWith(ALGORITHM_MANIFEST_PREFIX);
    }

    @VisibleForTesting
    List<Digest> createDigests(final List<String> relevantLines, final Path unpackedDirectory) {
        LOGGER.info("Get digests from Manifest file");
        if (relevantLines.size() % 3 != 0) {
            throw new FailedOnboardingValidationException("An entry in the Manifest file is missing a required field");
        }
        List<Digest> digests = new ArrayList<>();
        for (int i = 0; i < relevantLines.size(); i += 3) {
            Digest currentDigest = new Digest();
            setDigestLine(currentDigest, relevantLines.get(i), unpackedDirectory);
            setDigestLine(currentDigest, relevantLines.get(i + 1), unpackedDirectory);
            setDigestLine(currentDigest, relevantLines.get(i + 2), unpackedDirectory);
            digests.add(currentDigest);
        }
        return digests;
    }

    private static void setDigestLine(Digest digest, String line, final Path unpackedDirectory) {
        String[] split = line.split(":");
        if (split.length != 2) {
            throw new FailedOnboardingValidationException(
                    String.format("An entry in the Manifest file has a malformed field: %s", line));
        }
        if (SOURCE_MANIFEST_PREFIX.equalsIgnoreCase(split[0])) {
            Path source = unpackedDirectory.resolve(split[1].trim());
            digest.setSource(source);
        }
        if (HASH_MANIFEST_PREFIX.equalsIgnoreCase(split[0])) {
            digest.setHash(split[1].trim());
        }
        if (ALGORITHM_MANIFEST_PREFIX.equalsIgnoreCase(split[0])) {
            digest.setAlgorithm(split[1].trim());
        }
    }

    @VisibleForTesting
    List<ErrorMessage> validateDigests(final List<Digest> digests) {
        LOGGER.info("Generate hash for artifact and validate against hash from Manifest file");
        List<ErrorMessage> errors = new ArrayList<>();
        for (Digest digest : digests) {
            final Path source = digest.getSource();
            LOGGER.info("Validate {}", source);
            if (!source.toFile().exists()) {
                errors.add(new ErrorMessage(String.format("Artifact %s was not found in the package", source)));
                continue;
            }
            String generatedHash = generateHash(source, getAlgorithm(digest));
            if (!generatedHash.equalsIgnoreCase(digest.getHash())) {
                LOGGER.error("{} failed validation", source.toFile().getName());
                errors.add(new ErrorMessage(
                        String.format("Artifact %s did not match the generated sha %s", source.toFile().getName(), generatedHash)));
            }
        }
        return errors;
    }

    private static String getAlgorithm(final Digest digest) {
        if (digest.getAlgorithm().endsWith("256")) {
            return SHA_256;
        } else if (digest.getAlgorithm().endsWith("512")) {
            return SHA_512;
        }
        String message = String.format("Unsupported hash algorithm in manifest file: %s", digest.getAlgorithm());
        LOGGER.error(message);
        throw new FailedOnboardingValidationException(message);
    }
}
