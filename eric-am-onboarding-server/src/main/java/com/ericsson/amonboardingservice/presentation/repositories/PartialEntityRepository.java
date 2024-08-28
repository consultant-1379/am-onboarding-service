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

import com.ericsson.amonboardingservice.presentation.services.mapper.TupleToEntityMapper;
import jakarta.persistence.TypedQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nullable;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public abstract class PartialEntityRepository<T, ID> {

    private static final String SERIAL_VERSION_UID_FIELD_NAME = "serialVersionUID";

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private TupleToEntityMapper<T> mapper;

    public Page<T> selectPageExcludeFields(Class<T> clazz,
                                           final List<String> excludedFields,
                                           Pageable pageable) {
        return selectTuples(clazz, excludedFields, null, pageable);
    }

    public Page<T> selectPageExcludeFields(Class<T> clazz,
                                           final List<String> excludedFields,
                                           @Nullable Specification<T> spec,
                                           Pageable pageable) {
        return selectTuples(clazz, excludedFields, spec, pageable);
    }

    protected abstract Map<String, JoinType> initAssociations(Root<T> root);

    protected abstract Path<ID> getIdField(Root<T> root);

    private Page<T> selectTuples(Class<T> clazz,
                                 final List<String> excludedFields,
                                 Specification<T> spec,
                                 Pageable pageable) {
        try {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<Tuple> criteriaQuery = criteriaBuilder.createTupleQuery();
            CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);

            Root<T> root = criteriaQuery.from(clazz);
            Root<T> countRoot = countQuery.from(clazz);

            List<Selection<?>> columnsToSelect = processAssociationsExcludedFields(clazz, root, excludedFields);
            criteriaQuery.multiselect(columnsToSelect).distinct(true);
            countQuery.select(criteriaBuilder.count(countRoot)).distinct(true);

            if (Objects.nonNull(spec)) {
                criteriaQuery
                        .where(spec.toPredicate(root, criteriaQuery, criteriaBuilder));
                countQuery
                        .where(spec.toPredicate(countRoot, countQuery, criteriaBuilder));
            }

            TypedQuery<Tuple> typedQuery = paginateQuery(entityManager.createQuery(criteriaQuery), pageable);
            List<Tuple> resultList = processDuplicateTuples(
                    typedQuery.getResultList(),
                    root);
            long totalCount = entityManager.createQuery(countQuery).getSingleResult();

            return new PageImpl<T>(
                    mapper.mapAll(resultList, root),
                    pageable,
                    totalCount
            );
        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    private List<Selection<?>> processAssociationsExcludedFields(Class<T> clazz,
                                                                 Root<T> root,
                                                                 List<String> excludedFields) {
        Map<String, JoinType> associations = initAssociations(root);

        if (associations.isEmpty()) {
            return getSelectionFieldsWithoutExcluded(root, clazz, excludedFields);
        }

        List<String> fieldsToExcludeWithAssociations = new ArrayList<>(associations.keySet());
        fieldsToExcludeWithAssociations.addAll(excludedFields);

        return Stream.concat(
                getSelectionFieldsWithoutExcluded(root, clazz, fieldsToExcludeWithAssociations).stream(),
                getAssociationsList(root, associations).stream()
        ).toList();
    }

    private List<Selection<?>> getAssociationsList(final Root<T> root, final Map<String, JoinType> associations) {
        List<Selection<?>> joins = new ArrayList<>();

        for (var e : associations.entrySet()) {
            joins.add(root.join(e.getKey(), e.getValue()).alias(e.getKey()));
        }
        return joins;
    }

    private List<Tuple> processDuplicateTuples(final List<Tuple> tuples, final Root<T> root) {
        Map<ID, Tuple> map = new HashMap<>();
        for (var tuple : tuples) {
            map.putIfAbsent(tuple.get(getIdField(root)), tuple);
        }
        return new ArrayList<>(map.values());
    }

    private List<Selection<?>> getSelectionFieldsWithoutExcluded(Root<T> root,
                                                                 Class<T> rootClass,
                                                                 final List<String> excludedFields) {
        return Arrays.stream(rootClass.getDeclaredFields())
                .map(Field::getName)
                .filter(name -> !excludedFields.contains(name) && !name.equals(SERIAL_VERSION_UID_FIELD_NAME))
                .map(root::get)
                .collect(Collectors.toList());
    }

    private static <T> TypedQuery<T> paginateQuery(TypedQuery<T> query, Pageable pageable) {
        if (pageable.isPaged()) {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
        }
        return query;
    }

}