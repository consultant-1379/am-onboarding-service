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

import static java.lang.String.format;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.presentation.models.OnboardingDetail;
import com.ericsson.amonboardingservice.presentation.models.OnboardingHeartbeat;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import com.ericsson.amonboardingservice.presentation.repositories.OnboardingDetailRepository;
import com.ericsson.amonboardingservice.presentation.services.heartbeatservice.OnboardingHeartbeatService;
import com.ericsson.amonboardingservice.presentation.services.onboarding.request.PackageUploadRequestContext;
import com.ericsson.amonboardingservice.utils.Constants;

@Service
public class OnboardingDetailsService {

    @Autowired
    private AppPackageRepository appPackageRepository;

    @Autowired
    private OnboardingDetailRepository onboardingDetailRepository;

    @Autowired
    private OnboardingHeartbeatService onboardingHeartbeatService;

    @Autowired
    private PackageDatabaseService databaseService;

    public List<OnboardingDetail> findAllDetails() {
        return onboardingDetailRepository.findAll();
    }

    public void saveOnboardingContext(final String packageId, final PackageUploadRequestContext context) {
        final OnboardingDetail onboardingDetails = findOnboardingDetails(packageId);

        onboardingDetails.setPackageUploadContext(context);

        onboardingDetailRepository.save(onboardingDetails);
    }

    public OnboardingDetail findOnboardingDetails(final String packageId) {
        return onboardingDetailRepository
                .findByAppPackagePackageId(packageId)
                .orElseThrow(() -> new InternalRuntimeException(format(Constants.ONBOARDING_DETAIL_NOT_PRESENT_ERROR_MESSAGE, packageId)));
    }

    public PackageUploadRequestContext setPhaseAndRetrieveCurrentContext(final String packageId, final String phase) {
        final OnboardingDetail onboardingDetails = findOnboardingDetails(packageId);

        onboardingDetails.setOnboardingPhase(phase);

        final OnboardingDetail updatedOnboardingDetails = onboardingDetailRepository.save(onboardingDetails);

        return updatedOnboardingDetails.getPackageUploadContext();
    }

    @Transactional
    public void resetOnboardingDetails(final String packageId) {
        final AppPackage appPackage = databaseService.getAppPackageById(packageId);

        appPackage.setOnboardingDetail(null);

        appPackageRepository.save(appPackage);
    }

    public void setOnboardingHeartBeat(OnboardingDetail onboardingDetail) {

        OnboardingHeartbeat heartbeat = onboardingHeartbeatService.findOrCreateHeartbeat();

        saveOnboadingHeartbeat(onboardingDetail, heartbeat);
    }

    public void saveOnboadingHeartbeat(OnboardingDetail onboardingDetail, OnboardingHeartbeat onboardingHeartbeat) {
        onboardingDetail.setOnboardingHeartbeat(onboardingHeartbeat);
        onboardingDetailRepository.save(onboardingDetail);
    }
}
