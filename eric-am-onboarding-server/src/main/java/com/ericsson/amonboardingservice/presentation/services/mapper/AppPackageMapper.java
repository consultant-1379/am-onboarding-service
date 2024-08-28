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
package com.ericsson.amonboardingservice.presentation.services.mapper;

import static com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage.OnboardingStateEnum.ONBOARDED;
import static com.ericsson.amonboardingservice.presentation.services.manifestservice.ManifestServiceImpl.SHA_512;
import static com.ericsson.amonboardingservice.utils.JsonUtils.convertStringToJSONObj;
import static com.ericsson.amonboardingservice.utils.LinksUtility.constructPackageContentLink;
import static com.ericsson.amonboardingservice.utils.LinksUtility.constructSelfLinkWithId;
import static com.ericsson.amonboardingservice.utils.LinksUtility.constructVnfdLink;
import static com.ericsson.amonboardingservice.utils.SupportedOperationUtils.mapOperationDetailsEntityToResponse;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;

import com.ericsson.am.shared.vnfd.model.VnfDescriptorDetails;
import com.ericsson.amonboardingservice.infrastructure.configuration.HelmRegistryConfig;
import com.ericsson.amonboardingservice.model.AppPackageResponse;
import com.ericsson.amonboardingservice.model.AppPackageResponseV2;
import com.ericsson.amonboardingservice.model.HelmPackage;
import com.ericsson.amonboardingservice.model.ProblemDetails;
import com.ericsson.amonboardingservice.model.VnfPackageArtifactInfo;
import com.ericsson.amonboardingservice.model.VnfPackageSoftwareImageInfo;
import com.ericsson.amonboardingservice.model.VnfPkgInfo;
import com.ericsson.amonboardingservice.model.VnfPkgInfoChecksum;
import com.ericsson.amonboardingservice.model.VnfPkgInfoLink;
import com.ericsson.amonboardingservice.model.VnfPkgInfoLinks;
import com.ericsson.amonboardingservice.model.VnfPkgInfoV2;
import com.ericsson.amonboardingservice.model.VnfPkgInfoV2Checksum;
import com.ericsson.amonboardingservice.model.VnfPkgInfoV2Links;
import com.ericsson.amonboardingservice.model.VnfPkgInfoV2LinksSelf;
import com.ericsson.amonboardingservice.presentation.models.ChartUrlsEntity;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackageArtifacts;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackageDockerImage;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppUserDefinedData;
import com.ericsson.amonboardingservice.utils.Constants;
import com.google.common.base.Strings;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = DockerImageMapper.class)
@Import(HelmRegistryConfig.class)
public abstract class AppPackageMapper {

    private static final Pattern HOST_PATTERN = Pattern.compile("^(https?://)?([^:/]+)?(:\\d+)?");
    private static final String EMPTY_STRING = "";

    @Autowired
    private HelmRegistryConfig helmRegistryConfig;

    @Mapping(source = "packageId", target = "appPkgId")
    @Mapping(source = "descriptorId", target = "appDescriptorId")
    @Mapping(source = "descriptorVersion", target = "descriptorVersion")
    @Mapping(source = "provider", target = "appProvider")
    @Mapping(source = "productName", target = "appProductName")
    @Mapping(source = "softwareVersion", target = "appSoftwareVersion")
    @Mapping(source = "appPackageDockerImages", target = "imagesURL")
    @Mapping(source = "onboardingState", target = "onboardingState")
    @Mapping(source = "errorDetails", target = "problemDetails")
    public abstract AppPackageResponse toAppPackageResponse(AppPackage appPackage);

    public abstract List<AppPackageResponse> toAppPackageResponse(List<AppPackage> appPackages);

    @Mapping(source = "packageId", target = "appPkgId")
    @Mapping(source = "descriptorId", target = "appDescriptorId")
    @Mapping(source = "descriptorVersion", target = "descriptorVersion")
    @Mapping(source = "provider", target = "appProvider")
    @Mapping(source = "productName", target = "appProductName")
    @Mapping(source = "softwareVersion", target = "appSoftwareVersion")
    @Mapping(source = "appPackageDockerImages", target = "imagesURL")
    public abstract AppPackageResponseV2 toAppPackageResponseV2(AppPackage appPackage);

    public abstract List<AppPackageResponseV2> toAppPackageResponseV2(List<AppPackage> appPackages);

    @Mapping(source = "packageId", target = "id")
    @Mapping(source = "descriptorId", target = "vnfdId")
    @Mapping(source = "descriptorVersion", target = "vnfdVersion")
    @Mapping(source = "provider", target = "vnfProvider")
    @Mapping(source = "productName", target = "vnfProductName")
    @Mapping(source = "softwareVersion", target = "vnfSoftwareVersion")
    @Mapping(source = "checksum", target = "checksum", qualifiedByName = "appPackageToVnfPkgInfo")
    public abstract VnfPkgInfo toVnfPkgInfo(AppPackage appPackage);

    public abstract List<VnfPkgInfo> toVnfPkgInfo(List<AppPackage> appPackages);

    @Mapping(source = "packageId", target = "id")
    @Mapping(source = "descriptorId", target = "vnfdId")
    @Mapping(source = "descriptorVersion", target = "vnfdVersion")
    @Mapping(source = "provider", target = "vnfProvider")
    @Mapping(source = "productName", target = "vnfProductName")
    @Mapping(source = "softwareVersion", target = "vnfSoftwareVersion")
    @Mapping(source = "checksum", target = "checksum", qualifiedByName = "appPackageToVnfPkgInfoV2")
    public abstract VnfPkgInfoV2 toVnfPkgInfoV2(AppPackage appPackage);

    public abstract List<VnfPkgInfoV2> toVnfPkgInfoV2(List<AppPackage> appPackages);

    @Mapping(target = "descriptorId", source = "vnfDescriptorId")
    @Mapping(target = "descriptorVersion", source = "vnfDescriptorVersion")
    @Mapping(target = "provider", source = "vnfProvider")
    @Mapping(target = "productName", source = "vnfProductName")
    @Mapping(target = "softwareVersion", source = "vnfSoftwareVersion")
    public abstract AppPackage toAppPackage(VnfDescriptorDetails vnfDescriptorDetails);

    @ValueMapping(source = "ERROR", target = MappingConstants.NULL)
    public abstract VnfPkgInfo.OnboardingStateEnum toOnboardingStateEnum(AppPackage.OnboardingStateEnum onboardingState);

    @ValueMapping(source = "ERROR", target = "CREATED")
    public abstract String onboardingStateToString(AppPackage.OnboardingStateEnum onboardingState);

    public AppPackage createPackageDetails(final VnfDescriptorDetails vnfDescriptorDetails, final List<ChartUrlsEntity> chartUri) {
        AppPackage appPackage = toAppPackage(vnfDescriptorDetails);
        chartUri.forEach(chartUrlsEntity -> chartUrlsEntity.setAppPackage(appPackage));
        appPackage.setChartsRegistryUrl(chartUri);
        appPackage.setOnboardingState(ONBOARDED);
        appPackage.setPackageId(vnfDescriptorDetails.getVnfDescriptorId());
        return appPackage;
    }

    public static <T> Page<T> toResourcePage(Page<AppPackage> appPackagePage,
                                             Function<List<AppPackage>, List<T>> mapper) {
        return new PageImpl<>(
                mapper.apply(appPackagePage.getContent()),
                appPackagePage.getPageable(),
                appPackagePage.getTotalElements()
        );
    }

    @AfterMapping
    protected void toAppPackageResponse(AppPackage appPackage, @MappingTarget AppPackageResponse appPackageResponse) {
        appPackageResponse
                .setUsageState(AppPackageResponse.UsageStateEnum.fromValue(appPackage.getUsageState().name()));
        appPackageResponse.setDescriptorModel(Strings.isNullOrEmpty(appPackage.getDescriptorModel()) ?
                null :
                convertStringToJSONObj(appPackage.getDescriptorModel()));
        appPackageResponse.setProblemDetails(Strings.isNullOrEmpty(appPackage.getErrorDetails()) ?
                null :
                appPackage.getErrorDetails());
        appPackageResponse.setHelmPackageUrls(
                CollectionUtils.isEmpty(appPackage.getChartsRegistryUrl()) ?
                        null :
                        mapChartUrlsAsList(appPackage.getChartsRegistryUrl()));
        if (appPackage.getServiceModelRecordEntity() != null &&
                !Strings.isNullOrEmpty(appPackage.getServiceModelRecordEntity().getServiceModelId())) {
            appPackageResponse.setServiceModelId(appPackage.getServiceModelRecordEntity().getServiceModelId());
        } else {
            appPackageResponse.setServiceModelId(null);
        }

        if (appPackage.getOperationDetails() != null) {
            appPackageResponse.setSupportedOperations(mapOperationDetailsEntityToResponse(appPackage.getOperationDetails()));
        }

        if (appPackage.getPackageSecurityOption() != null) {
            appPackageResponse
                    .setPackageSecurityOption(AppPackageResponse.PackageSecurityOptionEnum
                            .fromValue(appPackage.getPackageSecurityOption().name()));
        }
        if (StringUtils.isNoneBlank(appPackage.getErrorDetails())) {
            appPackageResponse.setOnboardingState(AppPackage.OnboardingStateEnum.CREATED.name());
        }
    }

    @AfterMapping
    protected void toAppPackageResponseV2(AppPackage appPackage, @MappingTarget AppPackageResponseV2 appPackageResponseV2) {
        appPackageResponseV2
                .setUsageState(AppPackageResponseV2.UsageStateEnum.fromValue(appPackage.getUsageState().name()));
        appPackageResponseV2.setDescriptorModel(Strings.isNullOrEmpty(appPackage.getDescriptorModel()) ?
                null :
                convertStringToJSONObj(appPackage.getDescriptorModel()));
        appPackageResponseV2.setHelmPackageUrls(
                CollectionUtils.isEmpty(appPackage.getChartsRegistryUrl()) ?
                        null :
                        mapChartUrlsAsList(appPackage.getChartsRegistryUrl()));
        if (appPackage.getServiceModelRecordEntity() != null &&
                !Strings.isNullOrEmpty(appPackage.getServiceModelRecordEntity().getServiceModelId())) {
            appPackageResponseV2.setServiceModelId(appPackage.getServiceModelRecordEntity().getServiceModelId());
        } else {
            appPackageResponseV2.setServiceModelId(null);
        }

        if (appPackage.getOperationDetails() != null) {
            appPackageResponseV2.setSupportedOperations(mapOperationDetailsEntityToResponse(appPackage.getOperationDetails()));
        }

        if (appPackage.getPackageSecurityOption() != null) {
            appPackageResponseV2
                    .setPackageSecurityOption(AppPackageResponseV2.PackageSecurityOptionEnum
                            .fromValue(appPackage.getPackageSecurityOption().name()));
        }
        if (StringUtils.isNoneBlank(appPackage.getErrorDetails())) {
            appPackageResponseV2.setOnboardingFailureDetails(new ProblemDetails(
                    HttpStatus.UNPROCESSABLE_ENTITY.value(),
                    appPackage.getErrorDetails()));
        }
    }

    @AfterMapping
    protected void toVnfPkgInfo(AppPackage appPackage, @MappingTarget VnfPkgInfo vnfPkgInfo) {
        vnfPkgInfo.setOnboardingState(VnfPkgInfo.OnboardingStateEnum.fromValue(appPackage.getOnboardingState().name()));
        vnfPkgInfo.setUsageState(VnfPkgInfo.UsageStateEnum.fromValue(appPackage.getUsageState().name()));
        vnfPkgInfo.setOperationalState(VnfPkgInfo.OperationalStateEnum.fromValue(appPackage.getOperationalState().name()));
        List<AppUserDefinedData> allUserDefinedData = appPackage.getUserDefinedData();

        if (allUserDefinedData != null && !allUserDefinedData.isEmpty()) {
            Map<String, String> allUserDefinedDataMap = new HashMap<>();
            for (AppUserDefinedData userDefinedData : allUserDefinedData) {
                allUserDefinedDataMap.put(userDefinedData.getKey(), userDefinedData.getValue());
            }
            vnfPkgInfo.setUserDefinedData(allUserDefinedDataMap);
        } else {
            vnfPkgInfo.setUserDefinedData(null);
        }
        vnfPkgInfo.setSoftwareImages((appPackage.getAppPackageDockerImages() == null)
                ? null : mapSoftwareImages(appPackage.getAppPackageDockerImages()));

        final String packageId = appPackage.getPackageId();
        vnfPkgInfo.setAdditionalArtifacts(mapAdditionalArtifacts(appPackage.getAppPackageArtifacts()));
        VnfPkgInfoLinks vnfPkgInfoLinks = constructVnfPkgInfoLinks(packageId);
        vnfPkgInfo.setLinks(vnfPkgInfoLinks);

        vnfPkgInfo.setHelmPackageUrls(CollectionUtils.isEmpty(
                appPackage.getChartsRegistryUrl()) ? null : mapChartUrlsAsList(appPackage.getChartsRegistryUrl()));
        if (StringUtils.isNoneBlank(appPackage.getErrorDetails())) {
            vnfPkgInfo.setOnboardingState(VnfPkgInfo.OnboardingStateEnum.CREATED);
        }
    }

    @AfterMapping
    protected void toVnfPkgInfoV2(AppPackage appPackage, @MappingTarget VnfPkgInfoV2 vnfPkgInfoV2) {
        vnfPkgInfoV2.setUsageState(VnfPkgInfoV2.UsageStateEnum.fromValue(
                appPackage.getUsageState().name()));
        vnfPkgInfoV2.setOnboardingState(VnfPkgInfoV2.OnboardingStateEnum.fromValue(
                appPackage.getOnboardingState().name()));
        vnfPkgInfoV2.setOperationalState(VnfPkgInfoV2.OperationalStateEnum.fromValue(
                appPackage.getOperationalState().name()));
        List<AppUserDefinedData> allUserDefinedData = appPackage.getUserDefinedData();
        if (allUserDefinedData != null && !allUserDefinedData.isEmpty()) {
            Map<String, String> allUserDefinedDataMap = new HashMap<>();
            for (AppUserDefinedData userDefinedData : allUserDefinedData) {
                allUserDefinedDataMap.put(userDefinedData.getKey(), userDefinedData.getValue());
            }
            vnfPkgInfoV2.setUserDefinedData(allUserDefinedDataMap);
        } else {
            vnfPkgInfoV2.setUserDefinedData(null);
        }
        vnfPkgInfoV2.setSoftwareImages((appPackage.getAppPackageDockerImages() == null)
                ? null : mapSoftwareImages(appPackage.getAppPackageDockerImages()));
        vnfPkgInfoV2.setAdditionalArtifacts(mapAdditionalArtifacts(appPackage.getAppPackageArtifacts()));
        VnfPkgInfoV2Links vnfPkgInfoLinks = new VnfPkgInfoV2Links(
                new VnfPkgInfoV2LinksSelf(constructSelfLinkWithId(appPackage.getPackageId())),
                null);
        vnfPkgInfoV2.setLinks(vnfPkgInfoLinks);
        vnfPkgInfoV2.setHelmPackageUrls(CollectionUtils.isEmpty(
                appPackage.getChartsRegistryUrl()) ? null : mapChartUrlsAsList(appPackage.getChartsRegistryUrl()));
        setFailureDetails(appPackage, vnfPkgInfoV2);
    }

    private List<HelmPackage> mapChartUrlsAsList(final List<ChartUrlsEntity> chartsRegistryUrl) {
        final String chartUrl = getChartHost(chartsRegistryUrl);
        final String registryUrl = Objects.nonNull(helmRegistryConfig.getUrl()) ? helmRegistryConfig.getUrl() : "";
        return chartsRegistryUrl.stream().map(chartUrlsEntity -> {
            HelmPackage helmPackage = new HelmPackage();
            helmPackage.setChartUrl(chartUrlsEntity.getChartsRegistryUrl().replace(chartUrl, registryUrl));
            helmPackage.setPriority(chartUrlsEntity.getPriority());
            helmPackage.setChartName(chartUrlsEntity.getChartName());
            helmPackage.setChartVersion(chartUrlsEntity.getChartVersion());
            helmPackage.setChartType(HelmPackage.ChartTypeEnum.valueOf(chartUrlsEntity.getChartType().toString()));
            helmPackage.setChartArtifactKey(chartUrlsEntity.getChartArtifactKey());
            return helmPackage;
        }).collect(Collectors.toList());
    }

    private static List<VnfPackageSoftwareImageInfo> mapSoftwareImages(List<AppPackageDockerImage> images) {
        List<VnfPackageSoftwareImageInfo> vnfPkgInfoSoftwareImagesList = new ArrayList<>();
        if (images.isEmpty()) {
            return vnfPkgInfoSoftwareImagesList;
        }
        for (AppPackageDockerImage image : images) {
            VnfPackageSoftwareImageInfo vnfImage = new VnfPackageSoftwareImageInfo();
            vnfImage.setContainerFormat(VnfPackageSoftwareImageInfo.ContainerFormatEnum.DOCKER);
            vnfImage.setId(String.valueOf(image.getId()));
            vnfImage.setImagePath(image.getImageId());
            vnfImage.setName(StringUtils.split(image.getImageId(), ":")[0]);
            vnfImage.setVersion(StringUtils.split(image.getImageId(), ":")[1]);
            vnfPkgInfoSoftwareImagesList.add(vnfImage);
        }
        return vnfPkgInfoSoftwareImagesList;
    }

    private static List<VnfPackageArtifactInfo> mapAdditionalArtifacts(final List<AppPackageArtifacts> appPackageArtifacts) {
        return (appPackageArtifacts == null)
                ? null : appPackageArtifacts.stream()
                .map(AppPackageMapper::mapAdditionalArtifacts)
                .collect(Collectors.toList());
    }

    private static void setFailureDetails(final AppPackage appPackage, final VnfPkgInfoV2 vnfPkgInfo) {
        if (StringUtils.isNoneBlank(appPackage.getErrorDetails())) {
            ProblemDetails problemDetails = new ProblemDetails(
                    HttpStatus.UNPROCESSABLE_ENTITY.value(),
                    appPackage.getErrorDetails())
                    .type(URI.create(Constants.TYPE_BLANK));
            vnfPkgInfo.setOnboardingFailureDetails(problemDetails);
        }
    }

    @Named("appPackageToVnfPkgInfo")
    protected static VnfPkgInfoChecksum mapAppPackageToVnfPkgInfo(final String checksum) {
        if (checksum == null) {
            return null;
        }
        return new VnfPkgInfoChecksum(SHA_512, checksum);
    }

    @Named("appPackageToVnfPkgInfoV2")
    protected static VnfPkgInfoV2Checksum mapAppPackageToVnfPkgInfoV2(final String checksum) {
        if (checksum == null) {
            return null;
        }
        return new VnfPkgInfoV2Checksum(SHA_512, checksum);
    }

    private static VnfPackageArtifactInfo mapAdditionalArtifacts(final AppPackageArtifacts appPackageArtifacts) {
        return new VnfPackageArtifactInfo(appPackageArtifacts.getArtifactPath(), null);
    }

    private String getChartHost(final List<ChartUrlsEntity> chartsRegistryUrl) {
        String chartDomain = EMPTY_STRING;
        if (Objects.nonNull(helmRegistryConfig.getUrl()) && !helmRegistryConfig.getUrl().isEmpty()) {
            final ChartUrlsEntity chartUrlsEntity = chartsRegistryUrl.stream().findFirst().orElse(null);
            if (chartUrlsEntity != null) {
                chartDomain = getHost(chartUrlsEntity.getChartsRegistryUrl());
            }
        }
        return chartDomain;
    }

    private static String getHost(final String url) {
        return HOST_PATTERN.matcher(url)
                .results()
                .map(mr -> mr.group(0))
                .findFirst()
                .orElse(EMPTY_STRING);
    }

    private static VnfPkgInfoLinks constructVnfPkgInfoLinks(String packageId) {
        return new VnfPkgInfoLinks(
                new VnfPkgInfoLink(constructSelfLinkWithId(packageId)),
                new VnfPkgInfoLink(constructPackageContentLink(packageId)))
                .vnfd(new VnfPkgInfoLink(constructVnfdLink(packageId)));
    }

}
