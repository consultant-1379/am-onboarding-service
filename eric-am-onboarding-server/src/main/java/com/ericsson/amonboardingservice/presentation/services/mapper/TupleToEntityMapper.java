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
package com.ericsson.amonboardingservice.presentation.services.mapper;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.Root;
import java.util.List;

public interface TupleToEntityMapper<T> {
    T map(Tuple tuple, Root<T> root);
    List<T> mapAll(List<Tuple> tuples, Root<T> root);
}
