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
package com.ericsson.amonboardingservice.presentation.models;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringExclude;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.annotations.GenericGenerator;

import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "chart_urls")
public final class ChartUrlsEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Setter(value = AccessLevel.NONE)
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 64, nullable = false, name = "id")
    private String id;

    @ToStringExclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false, referencedColumnName = "package_id")
    private AppPackage appPackage;

    @Column(name = "charts_registry_url")
    private String chartsRegistryUrl;

    @Column(name = "priority")
    private int priority;

    @Column(name = "chart_name")
    private String chartName;

    @Column(name = "chart_version")
    private String chartVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "chart_type")
    private ChartTypeEnum chartType;

    @Column(name = "chart_artifact_key")
    private String chartArtifactKey;

    public enum ChartTypeEnum {
        CRD(1),
        CNF(2);

        private final int priority;

        ChartTypeEnum(int priority) {
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
