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
package com.ericsson.amonboardingservice.presentation.services.packageservice;

import com.ericsson.am.shared.vnfd.VnfdUtility;
import com.ericsson.am.shared.vnfd.model.VnfDescriptorDetails;
import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.amonboardingservice.presentation.services.mapper.AppPackageMapper;
import com.ericsson.amonboardingservice.model.AdditionalPropertyResponse;
import com.ericsson.amonboardingservice.model.AppPackageResponse;
import com.ericsson.amonboardingservice.model.CreateVnfPkgInfoRequest;
import com.ericsson.amonboardingservice.model.HelmPackage;
import com.ericsson.amonboardingservice.model.VnfPkgInfo;
import com.ericsson.amonboardingservice.presentation.exceptions.DataNotFoundException;
import com.ericsson.amonboardingservice.presentation.exceptions.FailedOnboardingException;
import com.ericsson.amonboardingservice.presentation.exceptions.FailedOnboardingValidationException;
import com.ericsson.amonboardingservice.presentation.exceptions.IllegalPackageStateException;
import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.presentation.exceptions.ObjectStorageException;
import com.ericsson.amonboardingservice.presentation.exceptions.UserInputException;
import com.ericsson.amonboardingservice.presentation.models.ChartUrlsEntity;
import com.ericsson.amonboardingservice.presentation.models.OnboardingDetail;
import com.ericsson.amonboardingservice.presentation.models.OnboardingHeartbeat;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppUserDefinedData;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.repositories.ChartUrlsRepository;
import com.ericsson.amonboardingservice.presentation.services.DeleteAppPackageHelper;
import com.ericsson.amonboardingservice.presentation.services.ToscaHelper;
import com.ericsson.amonboardingservice.presentation.services.dockerservice.DockerService;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileValidator;
import com.ericsson.amonboardingservice.presentation.services.filestorage.FileStorageService;
import com.ericsson.amonboardingservice.presentation.services.filter.VnfPackageQuery;
import com.ericsson.amonboardingservice.presentation.services.heartbeatservice.OnboardingHeartbeatService;
import com.ericsson.amonboardingservice.presentation.services.helmservice.HelmService;
import com.ericsson.amonboardingservice.presentation.services.objectstorage.ObjectStorageService;
import com.ericsson.amonboardingservice.presentation.services.onboarding.request.OnboardingChain;
import com.ericsson.amonboardingservice.presentation.services.onboarding.request.OnboardingProcessHandler;
import com.ericsson.amonboardingservice.presentation.services.onboarding.request.PackageUploadRequestContext;
import com.ericsson.amonboardingservice.utils.Constants;
import com.ericsson.amonboardingservice.utils.HelmChartUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsParametersEnum.isAdditionalParametersRequired;
import static com.ericsson.amonboardingservice.presentation.services.mapper.AppPackageMapper.toResourcePage;
import static java.lang.String.format;

@Slf4j
@Service
public class PackageServiceImpl implements PackageService {

    @Value("${onboarding.timeout}")
    private int onboardingTimeout;

    @Value("${container.registry.enabled}")
    private boolean isContainerRegistryEnabled;

    @Value("${onboarding.highAvailabilityMode}")
    private boolean isHighAvailabilityEnabled;

    @Autowired
    private FileService fileService;

    @Autowired
    private FileValidator fileValidator;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private AppPackageRepository appPackageRepository;

    @Autowired
    private ChartUrlsRepository chartUrlsRepository;

    @Autowired
    private AppPackageMapper appPackageMapper;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private VnfPackageQuery vnfPackageQuery;

    @Autowired
    private DockerService dockerService;

    @Autowired
    private HelmService helmService;

    @Autowired
    private ToscaHelper toscaHelper;

    @Autowired
    private OnboardingProcessHandler processHandler;

    @Autowired
    private OnboardingChain onboardingChain;

    @Autowired
    private OnboardingDetailsService onboardingDetailsService;

    @Autowired
    private OnboardingHeartbeatService onboardingHeartbeatService;

    @Autowired
    private PackageDatabaseService databaseService;

    @Lazy
    @Autowired
    private PackageService self;

    @Autowired
    private DeleteAppPackageHelper deletePackageHelper;

    @Autowired
    private ObjectStorageService objectStorageService;

    @Autowired
    private CleanUpOnFailureService failureService;

    @Autowired
    private AppPackageValidator packageValidator;

    @Override
    public AppPackage savePackageDetails(final VnfDescriptorDetails vnfDescriptorDetails,
                                         final List<ChartUrlsEntity> chartUri) {
        AppPackage appPackage = appPackageMapper.createPackageDetails(vnfDescriptorDetails, chartUri);
        return savePackage(appPackage);
    }

    @Override
    @Transactional
    public Page<AppPackageResponse> listPackages(Pageable pageable) {
        LOGGER.debug("Getting all package details");

        Page<AppPackage> appPackagePage = appPackageRepository.findAllIsSetForDeletionFalse(pageable);

        return toResourcePage(appPackagePage, appPackageMapper::toAppPackageResponse);
    }

    @Override
    @Transactional
    public Page<AppPackageResponse> listPackages(String filter, Pageable pageable) {
        LOGGER.debug("Getting all package matching filter {}", filter);

        Page<AppPackage> appPackagePage = vnfPackageQuery.getPageWithFilter(filter, pageable);

        return toResourcePage(appPackagePage, appPackageMapper::toAppPackageResponse);
    }

    @Override
    @Transactional
    public Optional<AppPackageResponse> getPackage(String id) {
        LOGGER.debug("Getting package details for id {}", id);
        return appPackageRepository.findByPackageIdNotBeingDeleted(id)
                .map(appPackageMapper::toAppPackageResponse);
    }

    @Override
    public void deletePackage(final String id) {
        LOGGER.info("Deleting package details for id {}", id);
        deletePackageHelper.setAndSaveAppPackageForDeletion(id);
        LOGGER.info("Package with id={} has been successfully removed", id);
    }

    @Override
    public void removeAppPackageWithResources(final String packageSetForDeletionId) {
        toscaHelper.deleteServiceModelFromToscaoByAppPackage(packageSetForDeletionId);
        deleteChartsByPackageResponse(deletePackageHelper.getAppPackageResponseById(packageSetForDeletionId));
        if (isContainerRegistryEnabled) {
            dockerService.removeDockerImagesByPackageId(packageSetForDeletionId);
        }
        appPackageRepository.deleteByPackageId(packageSetForDeletionId);
    }

    @Override
    public List<AppPackage> getPackagesSetForDeletion() {
        return appPackageRepository.findAllByIsSetForDeletionIsTrue();
    }

    @Override
    public void deleteChartsByPackageResponse(final Optional<AppPackageResponse> appPackageResponse) {
        List<HelmPackage> helmPackageUrls = Collections.emptyList();
        if (appPackageResponse.isPresent()) {
            helmPackageUrls = appPackageResponse.get().getHelmPackageUrls();
        }

        if (CollectionUtils.isEmpty(helmPackageUrls)) {
            LOGGER.info("No chart to delete");
        } else {
            for (HelmPackage helmPackage : helmPackageUrls) {
                String chartUrl = helmPackage.getChartUrl();
                if (listPackagesWithChartUrl(chartUrl).size() > 1) {
                    LOGGER.warn("Chart {} is currently referenced by other packages. Deletion skipped.", chartUrl);
                } else {
                    LOGGER.info("Chart to delete is {}", helmPackage);
                    String chartName = chartUrl.substring(chartUrl.lastIndexOf("/") + 1);
                    Optional<File> chartFile = helmService.getChart(chartUrl, chartName);
                    deleteChart(chartFile);
                }
            }
        }
    }

    private void deleteChart(final Optional<File> chartFile) {
        if (chartFile.isPresent()) {
            String chartName = HelmChartUtils.getChartYamlProperty(chartFile.get().toPath(), "name");
            String chartVersion = HelmChartUtils.getChartYamlProperty(chartFile.get().toPath(), "version");
            helmService.deleteChart(chartName, chartVersion);
            fileService.deleteFile(chartFile.get());
        }
    }

    @Override
    @Async
    public CompletableFuture<List<String>> getAutoCompleteResponse(String parameterName, String value, int pageNumber,
                                                                   int pageSize) {
        List<String> values;
        Pageable page;
        if (value != null) {
            switch (parameterName) {
                case Constants.TYPE:
                    page = PageRequest.of(pageNumber, pageSize, Sort.by("productName").ascending());
                    values = appPackageRepository.findDistinctProductName(value, page);
                    break;
                case Constants.SOFTWARE_VERSION:
                    page = PageRequest.of(pageNumber, pageSize, Sort.by("softwareVersion").ascending());
                    values = appPackageRepository.findDistinctSoftwareVersion(value, page);
                    break;
                case Constants.PACKAGE_VERSION:
                    page = PageRequest.of(pageNumber, pageSize, Sort.by("descriptorVersion").ascending());
                    values = appPackageRepository.findDistinctDescriptorVersion(value, page);
                    break;
                case Constants.PROVIDER:
                    page = PageRequest.of(pageNumber, pageSize, Sort.by("provider").ascending());
                    values = appPackageRepository.findDistinctProvider(value, page);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid parameter name provided " + parameterName);
            }
        } else {
            values = new ArrayList<>();
        }
        return CompletableFuture.completedFuture(values);
    }

    @Override
    public Path storePackage(final MultipartFile file) {
        String directoryName = UUID.randomUUID().toString();
        Path directory = fileService.createDirectory(directoryName);
        LOGGER.info("Storing {} to {}", file.getOriginalFilename(), directory);
        return fileStorageService.storeFile(file, directory, Constants.VNF_PACKAGE_ZIP);
    }

    private Path storePackage(InputStream fileInputStream) {
        String directoryName = UUID.randomUUID().toString();
        Path directory = fileService.createDirectory(directoryName);
        LOGGER.info("Storing package to {}", directory);
        return fileService.storeFile(fileInputStream, directory, Constants.VNF_PACKAGE_ZIP);
    }

    private String storePackageToObjectStore(InputStream inputStream, String originalFilename) {
        String directoryName = UUID.randomUUID().toString();
        Path directory = fileService.createDirectory(directoryName);
        String filename = directory.toString() + File.separator + originalFilename;
        LOGGER.info("Storing package {} to object store as {}", originalFilename, filename);
        try {
            objectStorageService.uploadFile(inputStream, filename);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return filename;
    }

    @Override
    public void packageUpload(final String vnfPkgId, final MultipartFile file) {
        AppPackage vnfPackage = databaseService.getAppPackageByIdNotBeingDeleted(vnfPkgId);
        packageValidator.validatePackageStateIsCreated(vnfPackage);
        fileValidator.validateFileOnPreUpload(file);

        int timeoutMinutes = getTimeout(vnfPackage);
        LocalDateTime timeoutDateTime = LocalDateTime.now().plusMinutes(timeoutMinutes);

        vnfPackage.setOnboardingDetail(createOnboardingDetail(vnfPackage, timeoutDateTime));
        updatePackageState(OnboardingPackageState.UPLOADING_NOT_IN_USE_DISABLED, vnfPackage);

        Path pathToPackage = storePackage(file);

        fileService.validateCsarForRelativePaths(pathToPackage, timeoutMinutes);

        self.asyncPackageUpload(file.getOriginalFilename(), pathToPackage, vnfPkgId, timeoutDateTime);
    }

    @Override
    public void packageUpload(final String vnfPkgId, final InputStream inputStream, long fileSize) {

        AppPackage vnfPackage = databaseService.getAppPackageByIdNotBeingDeleted(vnfPkgId);
        String originalFilename = getFilenameFromUserData(vnfPackage.getUserDefinedData());
        MDC.put("fileName", originalFilename);

        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

        packageValidator.validatePackageStateIsCreated(vnfPackage);
        fileValidator.validateFileOnPreUpload(bufferedInputStream, fileSize);

        int timeoutMinutes = getTimeout(vnfPackage);
        LocalDateTime timeoutDateTime = LocalDateTime.now().plusMinutes(timeoutMinutes);

        vnfPackage.setOnboardingDetail(createOnboardingDetail(vnfPackage, timeoutDateTime));
        updatePackageState(OnboardingPackageState.UPLOADING_NOT_IN_USE_DISABLED, vnfPackage);

        if (isHighAvailabilityEnabled) {
            String filename = storePackageToObjectStore(bufferedInputStream, Constants.VNF_PACKAGE_ZIP);
            self.asyncPackageUploadFromObjectStorage(filename, originalFilename, vnfPkgId, timeoutDateTime);
        } else {
            Path pathToPackage = storePackage(bufferedInputStream);
            fileService.validateCsarForRelativePaths(pathToPackage, timeoutMinutes);
            self.asyncPackageUpload(originalFilename, pathToPackage, vnfPkgId, timeoutDateTime);
        }
    }

    private int getTimeout(AppPackage appPackages) {
        List<AppUserDefinedData> userData = appPackages.getUserDefinedData();
        if (userData != null && !userData.isEmpty()) {
            return getTimeoutFromUserData(userData);
        } else {
            return onboardingTimeout;
        }
    }

    private Integer getTimeoutFromUserData(List<AppUserDefinedData> userData) {
        for (AppUserDefinedData data : userData) {
            if (Constants.ONBOARDING_TIMEOUT.equalsIgnoreCase(data.getKey())) {
                if (StringUtils.isNumeric(data.getValue())) {
                    return Integer.parseInt(data.getValue());
                } else {
                    LOGGER.warn("Using the default onboarding timeout because the value provided in the " +
                            "onboarding.timeOut is not a number. Value provided : {}", data.getValue());
                    return onboardingTimeout;
                }
            }
        }
        return onboardingTimeout;
    }

    private String getFilenameFromUserData(List<AppUserDefinedData> userDefinedData) {
        AppUserDefinedData data = userDefinedData.stream()
                .filter(entry -> Objects.equals(entry.getKey(), Constants.USER_DEFINED_FILENAME))
                .findFirst()
                .orElseThrow(() -> new IllegalPackageStateException("Filename for package wasn't provided"));

        return data.getValue();
    }

    private OnboardingDetail createOnboardingDetail(final AppPackage appPackage, final LocalDateTime timeoutDateTime) {
        OnboardingHeartbeat heartbeat = onboardingHeartbeatService.findOrCreateHeartbeat();

        final OnboardingDetail onboardingDetail = new OnboardingDetail();
        onboardingDetail.setAppPackage(appPackage);
        onboardingDetail.setExpiredOnboardingTime(timeoutDateTime);
        onboardingDetail.setOnboardingHeartbeat(heartbeat);

        return onboardingDetail;
    }

    @Async
    @Override
    public void asyncPackageUpload(final String originalFilename,
                                   final Path packageContents,
                                   final String packageId,
                                   final LocalDateTime timeoutDate) {

        packageUpload(originalFilename, packageContents, packageId, timeoutDate);
    }

    @Async
    @Override
    public void asyncPackageUploadFromObjectStorage(final String filename, final String originalFilename,
                                                    final String packageId,
                                                    final LocalDateTime localDateTime) {
        try {
            Path packageContents = fileStorageService.storePackageFromObjectStorage(filename, originalFilename);
            int timeoutMinutes = (int) ChronoUnit.MINUTES.between(LocalDateTime.now(), localDateTime);
            fileService.validateCsarForRelativePaths(packageContents, timeoutMinutes);
            packageUpload(originalFilename, packageContents, packageId, localDateTime);
        } catch (InternalRuntimeException | IllegalArgumentException | ObjectStorageException ex) {
            persistErrorInformation(createErrorDetails("package uploading", ex.getMessage()),
                    HttpStatus.BAD_REQUEST, packageId, ex);

            objectStorageService.deleteFile(filename);
        }
    }

    @SuppressWarnings("squid:S1141")
    private void packageUpload(final String originalFilename,
                               final Path packageContents,
                               final String packageId,
                               final LocalDateTime timeoutDate) {
        MDC.put("packageId", packageId);
        MDC.put("fileName", originalFilename);

        PackageUploadRequestContext context = new PackageUploadRequestContext(originalFilename, packageContents, timeoutDate, packageId);

        onboardingDetailsService.saveOnboardingContext(packageId, context);

        LOGGER.info("Starting onboarding processing: {}", context);

        try {
            processHandler.startOnboardingProcess(packageId, onboardingChain.buildDefaultChain());
        } catch (FailedOnboardingValidationException | FailedOnboardingException | DataNotFoundException
                 | UserInputException | IllegalArgumentException ex) {

            final OnboardingDetail onboardingDetails = onboardingDetailsService.findOnboardingDetails(packageId);

            persistErrorInformation(createErrorDetails(onboardingDetails.getOnboardingPhase(), ex.getMessage()),
                    HttpStatus.BAD_REQUEST, packageId, ex);
        } catch (Exception ex) {
            final OnboardingDetail onboardingDetails = onboardingDetailsService.findOnboardingDetails(packageId);

            persistErrorInformation(createErrorDetails(onboardingDetails.getOnboardingPhase(), ex.getMessage()),
                    HttpStatus.BAD_REQUEST, packageId, ex);
            failureService.cleanUpOnFailure(packageId, onboardingDetails.getPackageUploadContext());
        } finally {
            LOGGER.info("Finished onboarding processing");

            final OnboardingDetail onboardingDetails = onboardingDetailsService.findOnboardingDetails(packageId);
            final PackageUploadRequestContext updatedContext = onboardingDetails.getPackageUploadContext();

            if (!CollectionUtils.isEmpty(updatedContext.getArtifactPaths())) {
                updatedContext.getArtifactPaths().values()
                        .forEach(artifact -> fileService.deleteDirectory(artifact.getFileName().toString()));
            }
            fileStorageService.deleteFileFromObjectStorage(packageContents);
            onboardingDetailsService.resetOnboardingDetails(packageId);

            LOGGER.info("Finished cleanup resources after onboarding");
        }
    }

    private void persistErrorInformation(String errorDetails, HttpStatus statusCode, String packageId, Throwable throwable) {
        if (!StringUtils.isEmpty(packageId)) {
            updatePackageStateOnException(OnboardingPackageState.ERROR_NOT_IN_USE_DISABLED,
                    packageId,
                    errorDetails,
                    throwable);
        }
        LOGGER.error("Onboarding is failed with ErrorCode: {} and with Message: {}",
                statusCode.value(), errorDetails, throwable);
    }

    @Override
    public AppPackage savePackage(final AppPackage appPackage) {
        LOGGER.info("Saving package details {}", appPackage);
        Optional<AppPackage> existingAppPackage = appPackageRepository.findByPackageId(appPackage.getPackageId());
        if (existingAppPackage.isPresent()) {
            String message = format("A package with id %s already exists.", existingAppPackage.get().getPackageId());
            throw new IllegalPackageStateException(message);
        }
        return appPackageRepository.save(appPackage);
    }

    @Override
    public AppPackage updatePackageState(OnboardingPackageState onboardingPackageState, AppPackage appPackage) {
        onboardingPackageState.setPackageState(appPackage);
        return appPackageRepository.save(appPackage);
    }

    @Override
    public void updatePackageStateOnException(OnboardingPackageState onboardingPackageState, String packageId,
                                              String errorDetails, Throwable throwable) {
        try {
            AppPackage appPackage = databaseService.getAppPackageById(packageId);
            appPackage.setErrorDetails(errorDetails);
            onboardingPackageState.setPackageState(appPackage);
            appPackageRepository.save(appPackage);
        } catch (Exception e) {
            throwable.addSuppressed(e);
        }
    }

    @Override
    @Transactional
    public VnfPkgInfo createVnfPackage(CreateVnfPkgInfoRequest createVnfPkgInfoRequest) {
        AppPackage appPackage = new AppPackage();
        appPackage.setUserDefinedData(
                convertUserDefinedData(createVnfPkgInfoRequest.getUserDefinedData(), appPackage));
        final AppPackage savedAppPackage = appPackageRepository.save(appPackage);
        savedAppPackage.setProvider(Constants.DEFAULT_PACKAGE_PROVIDER_NAME);
        savedAppPackage.setProductName(Constants.DEFAULT_PACKAGE_PRODUCT_NAME);
        savedAppPackage.setSoftwareVersion(savedAppPackage.getPackageId());
        savedAppPackage.setDescriptorVersion(Constants.DEFAULT_PACKAGE_DESCRIPTOR_VERSION);
        appPackageRepository.save(savedAppPackage);
        LOGGER.info("AppPackage with id {} was created", savedAppPackage.getPackageId());
        return appPackageMapper.toVnfPkgInfo(savedAppPackage);
    }

    public static String createErrorDetails(String phaseDescription, String message) {
        StringBuilder errorDetails = new StringBuilder(format(Constants.ERROR_DETAILS_TEMPLATE, phaseDescription));
        if (StringUtils.isNotEmpty(message)) {
            errorDetails.append("\n").append(message);
        }
        return errorDetails.toString();
    }

    private List<AppUserDefinedData> convertUserDefinedData(Object userDefinedData, AppPackage appPackage) {
        List<AppUserDefinedData> allUserDefinedData = new ArrayList<>();
        if (userDefinedData == null) {
            return allUserDefinedData;
        }
        try {
            Map<String, String> userDefinedDataMap = mapper.convertValue(userDefinedData, new TypeReference<>() {
            });
            for (Map.Entry<String, String> entry : userDefinedDataMap.entrySet()) {
                AppUserDefinedData tempUserDefinedData = new AppUserDefinedData();
                tempUserDefinedData.setKey(entry.getKey());
                tempUserDefinedData.setAppPackages(appPackage);
                tempUserDefinedData.setValue(entry.getValue());
                allUserDefinedData.add(tempUserDefinedData);
            }
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException(
                    format("%s :: %s", Constants.USER_DEFINED_DATA_INVALID_FORMAT, iae.getMessage()), iae);
        }
        return allUserDefinedData;
    }

    @Override
    public List<AppPackage> listPackagesWithChartUrl(final String chartsRegistryUrl) {
        List<ChartUrlsEntity> byChartsRegistryUrl = chartUrlsRepository.findByChartsRegistryUrl(chartsRegistryUrl);
        return byChartsRegistryUrl.stream().map(ChartUrlsEntity::getAppPackage)
                .collect(Collectors.toList());
    }

    @Override
    public List<AdditionalPropertyResponse> getAdditionalParamsForOperationType(final String pkgId,
                                                                                final String operationType,
                                                                                final String destinationDescriptorId) {
        packageValidator.validateOperationSupported(operationType);
        if (!isAdditionalParametersRequired(operationType)) {
            return new ArrayList<>();
        }
        AppPackage appPackage = databaseService.getAppPackageByIdNotBeingDeleted(pkgId);
        packageValidator.validateIsAppPackageOnboardedState(appPackage);

        LOGGER.info(format(Constants.PARSING_VNFD_FOR_ADDITIONAL_ATTRIBUTES_MESSAGE, pkgId, operationType));
        return parseAdditionalParamsFromVnfd(operationType, destinationDescriptorId, appPackage);
    }

    @Override
    public byte[] getHelmfileContentByPackageId(final String vnfPkgId) {
        AppPackage appPackage = databaseService.getAppPackageByIdNotBeingDeleted(vnfPkgId);
        LOGGER.info("Checking if helmfile is present in package. {}", vnfPkgId);
        return Optional.ofNullable(appPackage.getHelmfile())
                .orElseThrow(() -> new DataNotFoundException(format("Helmfile for package %s does not exist. Try another package.", vnfPkgId)));
    }

    private List<AdditionalPropertyResponse> parseAdditionalParamsFromVnfd(final String operationType,
                                                                           final String destinationDescriptorId,
                                                                           final AppPackage appPackage) {
        String operation = operationType;
        JSONObject vnfd = new JSONObject(appPackage.getDescriptorModel());
        if (!toscaHelper.isTosca1Dot2(vnfd) && LCMOperationsEnum.CHANGE_VNFPKG.getOperation().equals(operationType)) {
            operation = LCMOperationsEnum.CHANGE_CURRENT_PACKAGE.getOperation();
        }
        JSONObject additionalParametersFromVnfd = VnfdUtility.getAdditionalParametersFromVnfd(vnfd, operation, destinationDescriptorId);
        return mapAdditionalParamsToPropertyList(additionalParametersFromVnfd);
    }

    private static List<AdditionalPropertyResponse> mapAdditionalParamsToPropertyList(JSONObject additionalParams) {
        List<AdditionalPropertyResponse> additionalPropertyResponseList = new ArrayList<>();

        for (String additionalParamKey : additionalParams.keySet()) {
            AdditionalPropertyResponse additionalPropertyResponse = getAdditionalPropertyResponse(additionalParams, additionalParamKey);
            additionalPropertyResponseList.add(additionalPropertyResponse);
        }

        return additionalPropertyResponseList;
    }

    private static AdditionalPropertyResponse getAdditionalPropertyResponse(JSONObject additionalParams, String additionalParamKey) {
        AdditionalPropertyResponse additionalPropertyResponse;
        Object additionalParam = additionalParams.get(additionalParamKey);
        try {
            additionalPropertyResponse = new ObjectMapper().readValue(additionalParam.toString(), AdditionalPropertyResponse.class);
        } catch (JsonProcessingException e) {
            throw new InternalRuntimeException("Cannot parse additional parameters from VNFD", e);
        }
        additionalPropertyResponse.setName(additionalParamKey);
        return additionalPropertyResponse;
    }

}
