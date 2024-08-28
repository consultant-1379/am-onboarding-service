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
package com.ericsson.amonboardingservice.presentation.services.onboarding.request;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.ericsson.am.shared.vnfd.model.VnfDescriptorDetails;
import com.ericsson.am.shared.vnfd.model.servicemodel.ServiceModel;
import com.ericsson.amonboardingservice.presentation.exceptions.ErrorMessage;
import com.ericsson.amonboardingservice.presentation.models.ToscaDefinitionsVersion;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.services.helmservice.HelmChartStatus;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class contains all the information which passed through the handlers in the package upload request.
 * If a new artifact in the package is to be handled any required information that is to pass between handlers can be
 * added here.
 */

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public final class PackageUploadRequestContext {
    private Set<Path> helmChartPaths;
    private byte[] helmfile;
    @Setter(value = AccessLevel.NONE)
    private String originalFileName;
    @Setter(value = AccessLevel.NONE)
    private Path packageContents;
    private String checksum;
    @Setter(value = AccessLevel.NONE)
    private LocalDateTime timeoutDate;
    private Map<String, Path> artifactPaths;
    @Setter(value = AccessLevel.NONE)
    private String packageId;
    private VnfDescriptorDetails vnfd;
    private Map<Path, HelmChartStatus> helmChartStatus;
    private boolean etsiPackage;
    private List<String> renamedImageList = new ArrayList<>();
    private ServiceModel serviceModel;
    private ToscaDefinitionsVersion toscaVersion;
    @Getter(value = AccessLevel.NONE)
    @Setter(value = AccessLevel.NONE)
    private List<ErrorMessage> errors;
    private boolean packageSigned;
    private AppPackage.PackageSecurityOption packageSecurityOption;

    public PackageUploadRequestContext(final String originalFilename,
                                       final Path packageContents,
                                       final LocalDateTime timeoutDate,
                                       final String packageId) {

        this.originalFileName = originalFilename;
        this.packageContents = packageContents;
        this.timeoutDate = timeoutDate;
        this.packageId = packageId;
    }

    public List<ErrorMessage> getErrors() {
        return List.copyOf(Optional.ofNullable(this.errors).orElseGet(Collections::emptyList));
    }

    public void addErrors(final ErrorMessage error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
    }

    @Override
    public String toString() {
        return "PackageUploadRequestContext{\n" +
                "packageContents=" + packageContents +
                ",\n timeoutDate=" + timeoutDate +
                ",\n packageId='" + packageId + '\'' +
                '}';
    }
}
