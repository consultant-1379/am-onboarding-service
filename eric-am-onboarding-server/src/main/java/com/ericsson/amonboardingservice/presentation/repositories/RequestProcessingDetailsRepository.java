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
package com.ericsson.amonboardingservice.presentation.repositories;

import com.ericsson.amonboardingservice.presentation.models.entity.RequestProcessingDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RequestProcessingDetailsRepository
        extends JpaRepository<RequestProcessingDetails, String> {

    @Query("SELECT requestProcessingDetails FROM RequestProcessingDetails requestProcessingDetails "
            + "WHERE "
            + "requestProcessingDetails.creationTime < :permittedLastUpdateTime")
    List<RequestProcessingDetails> findAllExpiredRequestProcessingDetails(@Param("permittedLastUpdateTime") LocalDateTime permittedLastUpdateTime);
}