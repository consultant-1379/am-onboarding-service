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
package com.ericsson.amonboardingservice.presentation.models.entity;


import com.ericsson.amonboardingservice.presentation.models.ProcessingState;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "request_processing_details")
public class RequestProcessingDetails {

    @Id
    @Column(name = "request_id")
    private String id;

    @Column(name = "request_hash")
    private String requestHash;

    @Column(name = "response_code")
    private Integer responseCode;

    @Column(name = "response_headers")
    private String responseHeaders;

    @Column(name = "response_body")
    private String responseBody;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_state")
    private ProcessingState processingState;

    @Column(name = "retry_after")
    private Integer retryAfter;

    @Column(name = "creation_time")
    private LocalDateTime creationTime;
}
