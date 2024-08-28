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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "service_model_record")
public class ServiceModelRecordEntity implements Serializable {
    private static final long serialVersionUID = 3037919543708220787L;

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(unique = true, nullable = false, length = 64, name = "id")
    private String id;

    @OneToOne
    @JoinColumn(name = "package_id", nullable = false, referencedColumnName = "package_id")
    private AppPackage appPackage;

    @Column(nullable = false, name = "service_model_id", unique = true)
    private String serviceModelId;

    @Column(nullable = false, name = "descriptor_id", unique = true)
    private String descriptorId;

    @Column(nullable = false, name = "service_model_name", unique = true)
    private String serviceModelName;
}
