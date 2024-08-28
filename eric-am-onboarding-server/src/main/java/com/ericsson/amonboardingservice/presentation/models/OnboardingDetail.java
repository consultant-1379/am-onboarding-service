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

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.ericsson.amonboardingservice.presentation.models.converter.PackageUploadContextConverter;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.services.onboarding.request.PackageUploadRequestContext;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "onboarding_details")
public class OnboardingDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "serial")
    private int id;

    @OneToOne
    @JoinColumn(name = "package_id", nullable = false)
    private AppPackage appPackage;

    @Column(name = "expired_onboarding_time", nullable = false)
    private LocalDateTime expiredOnboardingTime;

    @ManyToOne
    @JoinColumn(name = "onboarding_heartbeat_id")
    private OnboardingHeartbeat onboardingHeartbeat;

    @Column(name = "onboarding_phase")
    private String onboardingPhase;

    @Convert(converter = PackageUploadContextConverter.class)
    @Column(name = "package_upload_context")
    private PackageUploadRequestContext packageUploadContext;

    @Version
    private Long version;
}
