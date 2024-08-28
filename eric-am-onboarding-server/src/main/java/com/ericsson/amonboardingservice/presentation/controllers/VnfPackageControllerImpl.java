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

import com.ericsson.amonboardingservice.api.UsageStateApi;
import com.ericsson.amonboardingservice.api.VnfPackagesApi;
import com.ericsson.amonboardingservice.api.VnfPackagesOctetStreamApi;
import com.ericsson.amonboardingservice.model.AppPackageResponse;
import com.ericsson.amonboardingservice.model.AppUsageStateRequest;
import com.ericsson.amonboardingservice.model.CreateVnfPkgInfoRequest;
import com.ericsson.amonboardingservice.model.VnfPkgInfo;
import com.ericsson.amonboardingservice.presentation.exceptions.IllegalPackageStateException;
import com.ericsson.amonboardingservice.presentation.exceptions.PackageNotFoundException;
import com.ericsson.amonboardingservice.presentation.exceptions.StateConflictException;
import com.ericsson.amonboardingservice.presentation.exceptions.UnhandledException;
import com.ericsson.amonboardingservice.presentation.exceptions.UserInputException;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.services.PageableFactory;
import com.ericsson.amonboardingservice.presentation.services.idempotency.IdempotencyService;
import com.ericsson.amonboardingservice.presentation.services.packageinstanceservice.AppPackageInstanceService;
import com.ericsson.amonboardingservice.presentation.services.packageservice.PackageService;
import com.ericsson.amonboardingservice.presentation.services.vnfdservice.VnfdService;
import com.ericsson.amonboardingservice.presentation.services.vnfpackageservice.VnfPackageService;
import com.ericsson.amonboardingservice.utils.Constants;
import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.mime.MediaType;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static com.ericsson.am.shared.http.HttpUtility.getCurrentHttpRequest;
import static com.ericsson.amonboardingservice.utils.LinksUtility.constructSelfLinkWithId;
import static com.ericsson.amonboardingservice.utils.UrlUtils.createPaginationHeaders;
import static java.lang.String.format;

@Slf4j
@RestController
@RequestMapping("/api/vnfpkgm/v1")
@Tag(name = "vnf_packages", description = "The VNF packages API")
public class VnfPackageControllerImpl implements VnfPackagesApi, VnfPackagesOctetStreamApi, UsageStateApi {

    private static final String VNFD_CONFLICT_PACKAGE_STATE = "ID: %s is not in ONBOARDED state";
    private static final String INVALID_ACCEPT_TYPE_PROVIDED = "Invalid accept type provided %s, only text/plain," +
            " application/zip or text/plain,application/zip is supported";

    @Autowired
    private PackageService packageService;
    @Autowired
    private VnfPackageService vnfPackageService;
    @Autowired
    private VnfdService vnfdService;
    @Autowired
    private AppPackageInstanceService packageInstanceService;
    @Autowired
    private IdempotencyService idempotencyService;
    @Autowired
    private PageableFactory pageableFactory;

    @Override
    public ResponseEntity<List<VnfPkgInfo>> vnfPackagesGet(final String accept,
                                                           final String filter,
                                                           final String allFields,
                                                           final String fields,
                                                           final String excludeFields,
                                                           final Boolean excludeDefault,
                                                           final String nextpageOpaqueMarker,
                                                           final Integer size) {
        Pageable pageable = pageableFactory.createPageable(nextpageOpaqueMarker, size);
        Page<VnfPkgInfo> vnfPkgInfoPage = Strings.isNullOrEmpty(filter)
                ? vnfPackageService.listVnfPackages(pageable)
                : vnfPackageService.listVnfPackages(filter, pageable);

        return new ResponseEntity<>(vnfPkgInfoPage.getContent(), createPaginationHeaders(vnfPkgInfoPage), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<VnfPkgInfo> vnfPackagesPost(final String accept, final String contentType,
                                                      final CreateVnfPkgInfoRequest createVnfPkgInfoRequest,
                                                      final String idempotencyKey) {
        Supplier<ResponseEntity<VnfPkgInfo>> vnfPackagesPost = () -> {
            VnfPkgInfo response = packageService.createVnfPackage(createVnfPkgInfoRequest);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LOCATION, constructSelfLinkWithId(response.getId()));
            return new ResponseEntity<>(response, headers, HttpStatus.CREATED);
        };

        return idempotencyService.executeTransactionalIdempotentCall(vnfPackagesPost);
    }

    @Override
    public ResponseEntity<Void> vnfPackagesVnfPkgIdDelete(final String vnfPkgId, final String idempotencyKey) {
        Supplier<ResponseEntity<Void>> vnfPackagesVnfPkgIdDelete = () -> {
            AppPackageResponse appPackageResponse = packageService.getPackage(vnfPkgId).orElseThrow(
                () -> new PackageNotFoundException(format(Constants.VNF_PACKAGE_WITH_ID_DOES_NOT_EXIST, vnfPkgId)));
            if (appPackageResponse.getUsageState().equals(AppPackageResponse.UsageStateEnum.IN_USE)) {
                throw new IllegalPackageStateException(
                        Constants.INVALID_PACKAGE_USAGE_STATE);
            }
            packageService.deletePackage(vnfPkgId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        };

        return idempotencyService.executeTransactionalIdempotentCall(vnfPackagesVnfPkgIdDelete);
    }

    @Override
    public ResponseEntity<VnfPkgInfo> vnfPackagesVnfPkgIdGet(final String vnfPkgId, final String accept) {
        VnfPkgInfo vnfPkgInfo = vnfPackageService.getVnfPackage(vnfPkgId).orElseThrow(
            () -> new PackageNotFoundException(format(Constants.VNF_PACKAGE_WITH_ID_DOES_NOT_EXIST, vnfPkgId)));
        return new ResponseEntity<>(vnfPkgInfo, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Object> vnfPackagesVnfPkgIdHelmfileGet(final String vnfPkgId, final String accept) {
        byte[] helmfileContent = packageService.getHelmfileContentByPackageId(vnfPkgId);
        return new ResponseEntity<>(helmfileContent, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> vnfPackagesVnfPkgIdPackageContentPut(final String vnfPkgId, final MultipartFile file) {
        MDC.put("packageId", vnfPkgId);
        MDC.put("fileName", file.getOriginalFilename());
        LOGGER.info("Received a request to onboard");
        packageService.packageUpload(vnfPkgId, file);
        LOGGER.info("Onboarding request is accepted");

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @Override
    @SneakyThrows
    public ResponseEntity<Void> vnfPackagesVnfPkgIdPackageContentPut(final String vnfPkgId,
                                                                     Long contentLength,
                                                                     final InputStreamResource inputStream) {
        MDC.put("packageId", vnfPkgId);
        LOGGER.info("Received a request to onboard");
        packageService.packageUpload(vnfPkgId, inputStream.getInputStream(), contentLength);
        LOGGER.info("Onboarding request is accepted");

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @Override
    public ResponseEntity<Void> updatePackagesUsageState(String pkgId, AppUsageStateRequest appUsageStateRequest) {
        packageInstanceService.updatePackageUsageState(pkgId, appUsageStateRequest);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Object> vnfPackagesVnfPkgIdVnfdGet(String vnfPkgId, String accept) {
        validateAcceptTypeForFetchVnfd(accept);
        AppPackageResponse vnfPackage = packageService.getPackage(vnfPkgId)
                .orElseThrow(() -> new PackageNotFoundException(format(Constants.PACKAGE_NOT_PRESENT_ERROR_MESSAGE, vnfPkgId)));
        validateIsPackageOnboardedState(vnfPackage);

        Path zipFile = vnfdService.createVnfdZip(vnfPkgId);
        Path yamlFile = null;
        try {
            ByteArrayResource resource;
            if (MediaType.APPLICATION_ZIP.toString().equals(accept) || vnfdService.isMultipleFileVnfd(vnfPkgId)) {
                resource = new ByteArrayResource(Files.readAllBytes(zipFile));
                return ResponseEntity.ok()
                        .contentType(org.springframework.http.MediaType.parseMediaType(MediaType.APPLICATION_ZIP.toString()))
                        .body(resource);
            } else {
                yamlFile = Optional.ofNullable(vnfdService.createVnfdYamlFileByPackageId(vnfPkgId))
                        .orElseThrow(() -> new UnhandledException("Unable to create vnf descriptor file"));
                resource = new ByteArrayResource(Files.readAllBytes(yamlFile));
                return ResponseEntity.ok()
                        .contentType(org.springframework.http.MediaType.parseMediaType(MediaType.TEXT_PLAIN.toString()))
                        .body(resource);
            }
        } catch (IOException ioe) {
            throw new UnhandledException("Error %s creating descriptor file".formatted(ioe.getMessage()), ioe);
        } finally {
            vnfdService.deleteVnfdZipFile(zipFile);
            vnfdService.deleteVnfdYamlFile(yamlFile);
        }
    }

    @Override
    public ResponseEntity<Object> vnfPackagesVnfPkgIdArtifactsArtifactPathGet(String vnfPkgId, String artifactPath, final String accept) {
        validateAcceptTypeForFetchArtifact(accept);
        String uri = getCurrentHttpRequest().getRequestURI();
        String path = extractFilePath(uri, artifactPath);
        byte[] artifact = vnfdService.fetchArtifact(vnfPkgId, path);
        ByteArrayResource resource = new ByteArrayResource(artifact);
        if (MediaType.APPLICATION_ZIP.toString().equals(accept)) {
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(MediaType.APPLICATION_ZIP.toString()))
                    .body(resource);
        } else if (MediaType.TEXT_PLAIN.toString().equals(accept)) {
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(MediaType.TEXT_PLAIN.toString()))
                    .body(resource);
        } else {
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(MediaType.OCTET_STREAM.toString()))
                    .body(resource);
        }
    }

    private static String extractFilePath(String path, String artifact) {
        int pathLength = path.length();
        return path.substring(path.indexOf(artifact), pathLength);
    }

    private static void validateIsPackageOnboardedState(AppPackageResponse vnfPackage) {
        if (!AppPackage.OnboardingStateEnum.ONBOARDED.equals(AppPackage.OnboardingStateEnum.valueOf(vnfPackage
                .getOnboardingState()))) {
            throw new StateConflictException(format(VNFD_CONFLICT_PACKAGE_STATE, vnfPackage.getAppPkgId()));
        }
    }

    private static void validateAcceptTypeForFetchVnfd(final String accept) {
        if (Strings.isNullOrEmpty(accept)) {
            throw new UserInputException("Accept can't be null or empty");
        } else if (!MediaType.TEXT_PLAIN.toString().equals(accept) && !MediaType.APPLICATION_ZIP.toString().equals(accept)
                && !(MediaType.TEXT_PLAIN + "," + MediaType.APPLICATION_ZIP).equals(accept)) {
            throw new UserInputException(format(INVALID_ACCEPT_TYPE_PROVIDED, accept));
        }
    }

    private static void validateAcceptTypeForFetchArtifact(final String accept) {
        if (Strings.isNullOrEmpty(accept)) {
            throw new UserInputException("Accept can't be null or empty");
        } else if (!MediaType.TEXT_PLAIN.toString().equals(accept) && !MediaType.APPLICATION_ZIP.toString().equals(accept)
                && !MediaType.OCTET_STREAM.toString().equals(accept)
                && !(MediaType.TEXT_PLAIN + "," + MediaType.APPLICATION_ZIP
                + "," + MediaType.OCTET_STREAM).equals(accept)) {
            throw new UserInputException(format(INVALID_ACCEPT_TYPE_PROVIDED, accept));
        }
    }
}
