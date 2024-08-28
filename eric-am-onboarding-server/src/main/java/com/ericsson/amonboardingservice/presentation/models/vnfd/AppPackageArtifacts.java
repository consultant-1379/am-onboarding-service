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

import com.ericsson.amonboardingservice.utils.logging.ExcludeFieldsFromToString;
import com.ericsson.amonboardingservice.utils.logging.ExcludeFieldsFromToStringGenerator;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import java.io.Serializable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "app_package_artifacts")
public class AppPackageArtifacts implements Serializable {
    @Getter(value = AccessLevel.NONE)
    @Setter(value = AccessLevel.NONE)
    private static final long serialVersionUID = 6105869139740029679L;

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 64, nullable = false, name = "id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false, referencedColumnName = "package_id")
    private AppPackage appPackage;

    @Column(name = "artifact_path")
    private String artifactPath;

    @Column(name = "artifact")
    @ExcludeFieldsFromToString
    private byte[] artifact;

    @Override
    public String toString() {
        ToStringStyle customStringStyle = ExcludeFieldsFromToStringGenerator.INSTANCE.getStyle(this.getClass());
        return ToStringBuilder.reflectionToString(this, customStringStyle);
    }
}
