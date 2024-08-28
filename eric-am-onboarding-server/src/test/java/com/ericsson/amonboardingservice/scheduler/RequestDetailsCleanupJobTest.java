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

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.presentation.models.ProcessingState;
import com.ericsson.amonboardingservice.presentation.models.entity.RequestProcessingDetails;
import com.ericsson.amonboardingservice.presentation.repositories.RequestProcessingDetailsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringBootTest
@DirtiesContext
@ActiveProfiles("test")
@TestPropertySource(properties = { "idempotency.requestDetailsExpirationSeconds=1",
        "idempotency.fixedDelay=500"
})
public class RequestDetailsCleanupJobTest extends AbstractDbSetupTest {

    @SpyBean
    private RequestProcessingDetailsRepository requestProcessingDetailsRepository;

    @Test
    public void testCleanUpOldRequestDetails() {
        RequestProcessingDetails details = createDummyRequestDetails();
        requestProcessingDetailsRepository.findById(details.getId()).orElseThrow();

        await().atMost(3, SECONDS)
                .untilAsserted(() -> verify(requestProcessingDetailsRepository, atLeastOnce())
                        .deleteAll(any()));
        boolean contains = requestProcessingDetailsRepository.findAll().contains(details);

        assertFalse(contains, String.format("RequestProcessingDetails with id:[%s] mut not be present but it still is in database", details.getId()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RequestProcessingDetails createDummyRequestDetails() {
        RequestProcessingDetails processingDetails = new RequestProcessingDetails();
        processingDetails.setId(UUID.randomUUID().toString());
        processingDetails.setProcessingState(ProcessingState.FINISHED);
        processingDetails.setResponseCode(201);
        processingDetails.setRetryAfter(5);
        processingDetails.setRequestHash("dummy-hash-sum");
        processingDetails.setCreationTime(LocalDateTime.now());
        return requestProcessingDetailsRepository.save(processingDetails);
    }
}