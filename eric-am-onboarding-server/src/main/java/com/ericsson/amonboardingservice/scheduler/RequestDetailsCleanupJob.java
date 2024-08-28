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
package com.ericsson.amonboardingservice.scheduler;

import com.ericsson.amonboardingservice.presentation.models.entity.RequestProcessingDetails;
import com.ericsson.amonboardingservice.presentation.repositories.RequestProcessingDetailsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Component
public class RequestDetailsCleanupJob {

    @Autowired
    private RequestProcessingDetailsRepository requestProcessingDetailsRepository;

    @Value("${idempotency.requestDetailsExpirationSeconds}")
    private long requestDetailsExpirationSeconds;

    @Scheduled(fixedDelayString = "${idempotency.fixedDelay}")
    public void cleanUpOldRequestDetails() {

        List<RequestProcessingDetails> expiredRequestProcessingDetails =
                requestProcessingDetailsRepository.findAllExpiredRequestProcessingDetails(LocalDateTime.now(ZoneOffset.UTC)
                                                                                                  .minusSeconds(requestDetailsExpirationSeconds));
        if (!CollectionUtils.isEmpty(expiredRequestProcessingDetails)) {
            LOGGER.info("Cleaning {} expired request processing details", expiredRequestProcessingDetails.size());
            requestProcessingDetailsRepository.deleteAll(expiredRequestProcessingDetails);
        }

    }
}