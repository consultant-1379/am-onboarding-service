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

import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.newOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.ericsson.amonboardingservice.presentation.exceptions.FailedOnboardingValidationException;
import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.presentation.exceptions.UnhandledException;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class HelmChartUtils {

    /**
     * Details: https://regex101.com/r/vkijKf/1/
     */
    private final String CHART_VERSION_REGEX =
            "(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\."
                    + "(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?";
    private final Pattern CHART_VERSION_PATTER;

    static {
        CHART_VERSION_PATTER = Pattern.compile(CHART_VERSION_REGEX);
    }

    public String getChartYamlProperty(Path chart, String prop) {
        File tempFile = new File("");
        try (InputStream is = newInputStream(chart);
                TarArchiveInputStream tarInput = new TarArchiveInputStream(new GZIPInputStream(is))) {
            tempFile = File.createTempFile("temp-file", ".tmp");
            String property = traverseTgzFile(prop, tarInput, tempFile);
            return property != null ? property : "";
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new InternalRuntimeException(
                    String.format("Failed to extract property '%s' from Chart.yaml in Helm Chart '%s'"
                                          + " check application logs for more details", prop, chart));
        } finally {
            if (tempFile.exists()) {
                tempFile.delete(); // NOSONAR
            }
        }
    }

    public boolean containHelmfile(Path chart) {
        if (!Files.exists(chart)) {
            throw new FailedOnboardingValidationException(String.format("Chart %s not found", chart.getFileName()));
        }
        try (InputStream is = newInputStream(chart)) {
            if (isGZipped(is)) {
                return false;
            }

            TarArchiveInputStream tarInput = new TarArchiveInputStream(is);
            TarArchiveEntry currentEntry;
            while ((currentEntry = (TarArchiveEntry) tarInput.getNextEntry()) != null) {
                final String name = currentEntry.getName();
                if (name.endsWith("/helmfile.yaml")) {
                    return true;
                }
            }

            return false;
        } catch (IOException e) {
            LOGGER.error("Error happened during package verification for helmfile. Details: ", e);
            throw new InternalRuntimeException(
                    String.format("Failed to extract helmfile.yaml from package %s. Check application logs for more "
                            + "details.", chart.getFileName()));
        }
    }

    public Pair<String, String> parseChartUrl(final String chartRegistryUrl) {
        if (chartRegistryUrl.endsWith(Constants.CHART_ARCHIVE_EXTENSION)) {
            return parseCharUrlWithExtension(chartRegistryUrl);
        } else {
            return parseCharUrlWithSlash(chartRegistryUrl);
        }
    }

    private String traverseTgzFile(final String prop,
                                          final TarArchiveInputStream tarInput, final File temp) throws IOException {
        TarArchiveEntry currentEntry;
        while ((currentEntry = (TarArchiveEntry) tarInput.getNextEntry()) != null) {
            final String name = currentEntry.getName();
            if (isAtTopLevel(name) && name.endsWith("/Chart.yaml")) {
                return getPropFromFile(prop, tarInput, temp);
            }
        }
        return null;
    }

    // The tgz files in the charts folder are also traversed so we need to ensure the Chart.yaml file of this chart is selected.
    private boolean isAtTopLevel(final String name) {
        return StringUtils.countMatches(name, "/") == 1;
    }

    private String getPropFromFile(final String prop, final TarArchiveInputStream tarInput,
                                          final File temp) throws IOException {
        int count;
        int bufferSize = 4096;
        byte[] data = new byte[bufferSize];
        try (OutputStream os = newOutputStream(temp.toPath());
                BufferedOutputStream dest = new BufferedOutputStream(os)) {
            while ((count = tarInput.read(data, 0, bufferSize)) != -1) {
                dest.write(data, 0, count);
            }
        }
        List<String> lines = Files.readAllLines(temp.toPath(), Charset.defaultCharset());
        String property = lines.stream()
                .filter(l -> l.startsWith(prop))
                .findFirst()
                .orElse("");
        if (property.isEmpty()) {
            throw new IllegalArgumentException(String.format("Failed to find '%s' in Chart.yaml", prop));
        }
        property = property.split(":")[1].trim();
        return property;
    }

    private Pair<String, String> parseCharUrlWithExtension(final String chartRegistryUrl) {
        int lastSlashIndex = chartRegistryUrl.lastIndexOf('/');
        if (lastSlashIndex == -1) {
            logErrorAndThrow(String.format("Chart url should contain '/' delimiter to separate chart archive name: %s", chartRegistryUrl));
        }
        int chartNameEnd = chartRegistryUrl.lastIndexOf(".");
        if (chartNameEnd == -1) {
            logErrorAndThrow(String.format("Chart url should contain file extension: %s", chartRegistryUrl));
        }
        String chartNameAndVersion = chartRegistryUrl.substring(lastSlashIndex + 1, chartNameEnd);
        Matcher versionMatcher = CHART_VERSION_PATTER.matcher(chartNameAndVersion);
        // first matching substring is chart version
        if (!versionMatcher.find()) {
            logErrorAndThrow(String.format("Could not find version in chart name %s", chartNameAndVersion));
        }
        String chartVersion = versionMatcher.group();
        if (versionMatcher.start() <= 1) {
            logErrorAndThrow(String.format("Chart name should not be empty for chart url %s and chart version %s", chartRegistryUrl, chartVersion));
        }
        String chartName = chartNameAndVersion.substring(0, versionMatcher.start() - 1);
        return Pair.of(chartName, chartVersion);
    }

    private Pair<String, String> parseCharUrlWithSlash(final String chartRegistryUrl) {
        int lastSlashIndex = chartRegistryUrl.lastIndexOf('/');
        if (lastSlashIndex == -1) {
            logErrorAndThrow(String.format("Chart url should contain '/' delimiter to separate chart version: %s", chartRegistryUrl));
        }
        int preLastSlashIndex = chartRegistryUrl.lastIndexOf('/', lastSlashIndex - 1);
        if (preLastSlashIndex == -1) {
            logErrorAndThrow(String.format("Chart url should contain '/' delimiter to separate chart name from chart version: %s", chartRegistryUrl));
        }
        String chartName = chartRegistryUrl.substring(preLastSlashIndex + 1, lastSlashIndex);
        String chartVersion = chartRegistryUrl.substring(lastSlashIndex + 1);
        return Pair.of(chartName, chartVersion);
    }

    private void logErrorAndThrow(String errorMessage) {
        LOGGER.error(errorMessage);
        throw new UnhandledException(errorMessage);
    }

    private boolean isGZipped(InputStream in) {
        InputStream tmpStream = in;
        if (!tmpStream.markSupported()) {
            tmpStream = new BufferedInputStream(in);
        }
        tmpStream.mark(2);
        int magic;
        try {
            magic = tmpStream.read() & 0xff | ((tmpStream.read() << 8) & 0xff00);
            tmpStream.reset();
        } catch (IOException e) {
            LOGGER.warn("Can't read stream", e);
            return false;
        }
        return magic == GZIPInputStream.GZIP_MAGIC;
    }

}
