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
package com.ericsson.amonboardingservice.presentation.models.vnfd;

import com.ericsson.amonboardingservice.presentation.models.ChartUrlsEntity;
import com.ericsson.amonboardingservice.presentation.models.OnboardingDetail;
import com.ericsson.amonboardingservice.presentation.models.ServiceModelRecordEntity;
import com.ericsson.amonboardingservice.utils.logging.ExcludeFieldsFromToString;
import com.ericsson.amonboardingservice.utils.logging.ExcludeFieldsFromToStringGenerator;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.annotations.GenericGenerator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(
    exclude = {
        "serviceModelRecordEntity", "chartsRegistryUrl", "appPackageDockerImages",
        "userDefinedData", "operationDetails", "onboardingDetail", "appPackageArtifacts"
    })
@Entity
@Accessors(chain = true)
@Table(name = "app_packages")
public class AppPackage implements Serializable {
    @Getter(value = AccessLevel.NONE)
    @Setter(value = AccessLevel.NONE)
    private static final long serialVersionUID = 3797622909675880804L;

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(unique = true, nullable = false, length = 64, name = "package_id")
    private String packageId;
    @Column(name = "descriptor_id")
    private String descriptorId;
    private String descriptorVersion;
    @ExcludeFieldsFromToString
    private String descriptorModel;
    private String provider;
    private String productName;
    private String softwareVersion;
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "onboarding_state")
    private OnboardingStateEnum onboardingState = OnboardingStateEnum.CREATED;

    @ExcludeFieldsFromToString
    private byte[] files;

    @ExcludeFieldsFromToString
    private byte[] helmfile;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "appPackage", orphanRemoval = true)
    private ServiceModelRecordEntity serviceModelRecordEntity;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "appPackage", orphanRemoval = true)
    private List<ChartUrlsEntity> chartsRegistryUrl;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "app_usage_state")
    private AppUsageStateEnum usageState = AppUsageStateEnum.NOT_IN_USE;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "appPackage", orphanRemoval = true)
    private List<AppPackageDockerImage> appPackageDockerImages;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "operational_state")
    private OperationalStateEnum operationalState = OperationalStateEnum.DISABLED;

    /*
    * was @LazyCollection(LazyCollectionOption.FALSE)
    * but deprecated since="6.2"
    * as for recommendation used FetchType.EAGER
    * see https://docs.jboss.org/hibernate/orm/6.2/javadocs/org/hibernate/annotations/LazyCollection.html
    * */
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "appPackages")
    private List<AppUserDefinedData> userDefinedData;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "appPackage", orphanRemoval = true)
    private List<OperationDetailEntity> operationDetails;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "appPackage", orphanRemoval = true)
    private List<AppPackageArtifacts> appPackageArtifacts;

    private String errorDetails;

    @Column(columnDefinition = "vnfd_zip")
    private byte[] vnfdZip;

    @Column(columnDefinition = "is_multiple_vnfd")
    private boolean isMultipleVnfd;


    @Column(columnDefinition = "is_set_for_deletion")
    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    private Boolean isSetForDeletion;


    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "package_security_option")
    private PackageSecurityOption packageSecurityOption;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "appPackage", orphanRemoval = true)
    private OnboardingDetail onboardingDetail;

    public enum OnboardingStateEnum {
        CREATED, UPLOADING, PROCESSING, ONBOARDED, ERROR
    }

    public enum OperationalStateEnum {
        ENABLED, DISABLED
    }

    public enum AppUsageStateEnum {
        IN_USE, NOT_IN_USE
    }

    public enum PackageSecurityOption {
        OPTION_1, OPTION_2, UNSIGNED
    }

    @Override
    public String toString() {
        ToStringStyle customStringStyle = ExcludeFieldsFromToStringGenerator.INSTANCE.getStyle(this.getClass());
        return ToStringBuilder.reflectionToString(this, customStringStyle);
    }

    public Boolean isSetForDeletion() {
        return isSetForDeletion;
    }

    public void setForDeletion(final Boolean setForDeletion) {
        isSetForDeletion = setForDeletion;
    }

    public void setAppPackageDockerImages(List<AppPackageDockerImage> appPackageDockerImages) {
        if (this.appPackageDockerImages == null) {
            this.appPackageDockerImages = new ArrayList<>();
        } else {
            this.appPackageDockerImages.addAll(appPackageDockerImages);
        }
    }
}
