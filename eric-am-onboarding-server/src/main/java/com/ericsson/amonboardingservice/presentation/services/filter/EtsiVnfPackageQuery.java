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
package com.ericsson.amonboardingservice.presentation.services.filter;

import com.ericsson.am.shared.filter.CreateQueryFilter;
import com.ericsson.am.shared.filter.model.DataType;
import com.ericsson.am.shared.filter.model.FilterExpressionMultiValue;
import com.ericsson.am.shared.filter.model.FilterExpressionOneValue;
import com.ericsson.am.shared.filter.model.MappingData;
import com.ericsson.am.shared.filter.model.OperandMultiValue;
import com.ericsson.am.shared.filter.model.OperandOneValue;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.AppPackageRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EtsiVnfPackageQuery extends CreateQueryFilter<AppPackage, AppPackageRepository> {

    public static final String FILTER_KEY_NOT_SUPPORTED_ERROR_MESSAGE = "Filter key %s is not supported";

    private static final String USAGE_STATE = "usageState";
    private static final String ONBOARDING_STATE = "onboardingState";
    private static final String OPERATIONAL_STATE = "operationalState";
    private static final String PACKAGE_SECURITY_OPTION = "packageSecurityOption";
    private static final String USER_DEFINED_DATA = "userDefinedData/";
    private static final String VNFD_ID = "vnfdId";
    private static final String VNF_PROVIDER = "vnfProvider";
    private static final String VNF_PRODUCT_NAME = "vnfProductName";
    private static final String VNF_SOFTWARE_VERSION = "vnfSoftwareVersion";
    private static final String VNFD_VERSION = "vnfdVersion";

    public EtsiVnfPackageQuery(AppPackageRepository appPackageRepository) {
        super(createMappingData(), appPackageRepository);
    }

    @Override
    public Page<AppPackage> getPageWithFilter(String filter, Pageable pageable) {
        Specification<AppPackage> specification = createUserDefinedFilterSpecification(filter)
                .orElseGet(() -> getSpecification(filter));

        return jpaRepository.findAll(specification.and(isNotSetForDeletion()), pageable);
    }

    private Optional<Specification<AppPackage>> createUserDefinedFilterSpecification(String filter) {
        String notPresentFilter = filterNotPresent(filter);
        String filterPresent = filterPresent(filter);
        Specification<AppPackage> specification = null;

        if (notPresentFilter != null && notPresentFilter.contains(USER_DEFINED_DATA)) {
            List<String> userDefinedSingleFilter = getSingleValueFilter(notPresentFilter.substring(1,
                    notPresentFilter.length() - 1));
            List<FilterExpressionOneValue<String>> sortedUserDefinedData = getSortedUserDefinedExpressionOneFilter(
                    createSingleValueFilter(userDefinedSingleFilter, true));
            List<String> userDefinedMultiFilter = getMultiValueFilter(notPresentFilter.substring(1,
                    notPresentFilter.length() - 1));
            List<FilterExpressionMultiValue<String>> sortedUserDefinedDataMulti = getSortedUserDefinedExpressionMultiFilter(
                    createMultiValueFilter(userDefinedMultiFilter, true), sortedUserDefinedData);

            specification = createSpecification(sortedUserDefinedData, sortedUserDefinedDataMulti);

            if (filterPresent != null && !filterPresent.isEmpty()) {
                specification = specification.and(getSpecification(filterPresent));
            }
        }

        return Optional.ofNullable(specification);
    }

    private static Specification<AppPackage> isNotSetForDeletion() {
        return (root, query, criteriaBuilder) -> {
            Predicate isNull = criteriaBuilder.isNull(root.get("isSetForDeletion"));
            Predicate isFalse = criteriaBuilder.isFalse(root.get("isSetForDeletion"));
            return criteriaBuilder.or(isNull, isFalse);
        };
    }

    @SuppressWarnings("unchecked")
    private static List<FilterExpressionOneValue<String>> getSortedUserDefinedExpressionOneFilter(
            List<FilterExpressionOneValue<String>> userDefinedFilterExpressionOneValue) {
        List<FilterExpressionOneValue<String>> sortedUserDefinedExpressionOneFilter = new ArrayList<>();
        for (FilterExpressionOneValue<String> filterExpression : userDefinedFilterExpressionOneValue) {
            if (!filterExpression.getKey().startsWith(USER_DEFINED_DATA)) {
                throw new IllegalArgumentException(String.format(FILTER_KEY_NOT_SUPPORTED_ERROR_MESSAGE,
                        filterExpression.getKey()));
            }
            FilterExpressionOneValue<String> filterExpressionForKey = new FilterExpressionOneValue<>();
            filterExpressionForKey.setJoinType(filterExpression.getJoinType());
            filterExpressionForKey.setOperation(OperandOneValue.EQUAL);
            filterExpressionForKey.setKey("userDefinedData.key");
            String key = filterExpression.getKey();
            filterExpressionForKey.setValue(key.substring(USER_DEFINED_DATA.length()));

            FilterExpressionOneValue<String> filterExpressionForValue = new FilterExpressionOneValue<>();
            filterExpressionForValue.setJoinType(filterExpression.getJoinType());
            filterExpressionForValue.setOperation(filterExpression.getOperation());
            filterExpressionForValue.setKey("userDefinedData.value");
            filterExpressionForValue.setValue(filterExpression.getValue());
            sortedUserDefinedExpressionOneFilter.add(filterExpressionForKey);
            sortedUserDefinedExpressionOneFilter.add(filterExpressionForValue);
        }
        return sortedUserDefinedExpressionOneFilter;
    }

    @SuppressWarnings("unchecked")
    private static List<FilterExpressionMultiValue<String>> getSortedUserDefinedExpressionMultiFilter(
            List<FilterExpressionMultiValue<String>> userDefinedFilterExpressionMultiValue,
            List<FilterExpressionOneValue<String>> sortedUserDefinedData) {
        List<FilterExpressionMultiValue<String>> sortedUserDefinedExpressionMultiFilter = new ArrayList<>();
        for (FilterExpressionMultiValue<String> filterExpression : userDefinedFilterExpressionMultiValue) {
            if (!filterExpression.getKey().startsWith(USER_DEFINED_DATA)) {
                throw new IllegalArgumentException(String.format(FILTER_KEY_NOT_SUPPORTED_ERROR_MESSAGE,
                        filterExpression.getKey()));
            }
            FilterExpressionOneValue<String> filterExpressionForKey = new FilterExpressionOneValue<>();
            filterExpressionForKey.setJoinType(filterExpression.getJoinType());
            filterExpressionForKey.setOperation(OperandOneValue.EQUAL);
            filterExpressionForKey.setKey("userDefinedData.key");
            String key = filterExpression.getKey();
            filterExpressionForKey.setValue(key.substring(USER_DEFINED_DATA.length()));

            FilterExpressionMultiValue<String> filterExpressionForValue = new FilterExpressionMultiValue<>();
            filterExpressionForValue.setJoinType(filterExpression.getJoinType());
            filterExpressionForValue.setOperation(filterExpression.getOperation());
            filterExpressionForValue.setKey("userDefinedData.value");
            filterExpressionForValue.setValues(filterExpression.getValues());
            sortedUserDefinedData.add(filterExpressionForKey);
            sortedUserDefinedExpressionMultiFilter.add(filterExpressionForValue);
        }
        return sortedUserDefinedExpressionMultiFilter;
    }

    @Override
    @SuppressWarnings("squid:S1067")
    public FilterExpressionOneValue createFilterExpressionOneValue(String key, String value, String operand) {
        switch (key) {
            case USAGE_STATE -> {
                FilterExpressionOneValue<AppPackage.AppUsageStateEnum> usageState = new FilterExpressionOneValue<>();
                usageState.setKey(getMapping().get(key).getMapping());
                usageState.setOperation(OperandOneValue.fromFilterOperation(operand));
                validateDataType(DataType.ENUMERATION, value, AppPackage.AppUsageStateEnum.class);
                usageState.setValue(AppPackage.AppUsageStateEnum.valueOf(value));
                return usageState;
            }
            case ONBOARDING_STATE -> {
                FilterExpressionOneValue<AppPackage.OnboardingStateEnum> onboardingState = new FilterExpressionOneValue<>();
                onboardingState.setKey(getMapping().get(key).getMapping());
                onboardingState.setOperation(OperandOneValue.fromFilterOperation(operand));
                validateDataType(DataType.ENUMERATION, value, AppPackage.OnboardingStateEnum.class);
                onboardingState.setValue(AppPackage.OnboardingStateEnum.valueOf(value));
                return onboardingState;
            }
            case OPERATIONAL_STATE -> {
                FilterExpressionOneValue<AppPackage.OperationalStateEnum> operationalState = new FilterExpressionOneValue<>();
                operationalState.setKey(getMapping().get(key).getMapping());
                operationalState.setOperation(OperandOneValue.fromFilterOperation(operand));
                validateDataType(DataType.ENUMERATION, value, AppPackage.OnboardingStateEnum.class);
                operationalState.setValue(AppPackage.OperationalStateEnum.valueOf(value));
                return operationalState;
            }
            case PACKAGE_SECURITY_OPTION -> {
                FilterExpressionOneValue<AppPackage.PackageSecurityOption> operationalState = new FilterExpressionOneValue<>();
                operationalState.setKey(getMapping().get(key).getMapping());
                operationalState.setOperation(OperandOneValue.fromFilterOperation(operand));
                validateDataType(DataType.ENUMERATION, value, AppPackage.PackageSecurityOption.class);
                operationalState.setValue(AppPackage.PackageSecurityOption.valueOf(value));
                return operationalState;
            }
            case VNFD_ID, VNF_PROVIDER, VNF_PRODUCT_NAME, VNF_SOFTWARE_VERSION, VNFD_VERSION -> {
                FilterExpressionOneValue<String> stringValue = new FilterExpressionOneValue<>();
                stringValue.setKey(getMapping().get(key).getMapping());
                stringValue.setOperation(OperandOneValue.fromFilterOperation(operand));
                stringValue.setValue(value);
                return stringValue;
            }
            default -> {
                FilterExpressionOneValue<String> filterExpression = new FilterExpressionOneValue<>();
                filterExpression.setKey(key);
                filterExpression.setOperation(OperandOneValue.fromFilterOperation(operand));
                filterExpression.setValue(value);
                filterExpression.setJoinType(JoinType.LEFT);
                return filterExpression;
            }
        }
    }

    private List<AppPackage.AppUsageStateEnum> createAllUsageState(List<String> values) {
        List<AppPackage.AppUsageStateEnum> allUsageState = new ArrayList<>();
        for (String value : values) {
            validateDataType(DataType.ENUMERATION, value, AppPackage.AppUsageStateEnum.class);
            allUsageState.add(AppPackage.AppUsageStateEnum.valueOf(value));
        }
        return allUsageState;
    }

    private List<AppPackage.OnboardingStateEnum> createAllOnboardingState(List<String> values) {
        List<AppPackage.OnboardingStateEnum> allOnboardingState = new ArrayList<>();
        for (String value : values) {
            validateDataType(DataType.ENUMERATION, value, AppPackage.OnboardingStateEnum.class);
            allOnboardingState.add(AppPackage.OnboardingStateEnum.valueOf(value));
        }
        return allOnboardingState;
    }

    private List<AppPackage.OperationalStateEnum> createAllOperationState(List<String> values) {
        List<AppPackage.OperationalStateEnum> allOperationalStates = new ArrayList<>();
        for (String value : values) {
            validateDataType(DataType.ENUMERATION, value, AppPackage.OnboardingStateEnum.class);
            allOperationalStates.add(AppPackage.OperationalStateEnum.valueOf(value));
        }
        return allOperationalStates;
    }

    private List<AppPackage.PackageSecurityOption> createAllSecurityOptions(List<String> values) {
        List<AppPackage.PackageSecurityOption> allPackageSecurityOptions = new ArrayList<>();
        for (String value : values) {
            validateDataType(DataType.ENUMERATION, value, AppPackage.PackageSecurityOption.class);
            allPackageSecurityOptions.add(AppPackage.PackageSecurityOption.valueOf(value));
        }
        return allPackageSecurityOptions;
    }

    @Override
    @SuppressWarnings("squid:S1067")
    public FilterExpressionMultiValue createFilterExpressionMultiValue(String key,
                                                                       List<String> values,
                                                                       String operand
    ) {
        switch (key) {
            case USAGE_STATE -> {
                FilterExpressionMultiValue<AppPackage.AppUsageStateEnum> usageState = new FilterExpressionMultiValue<>();
                usageState.setKey(getMapping().get(key).getMapping());
                usageState.setOperation(OperandMultiValue.fromFilterOperation(operand));
                usageState.setValues(createAllUsageState(values));
                return usageState;
            }
            case ONBOARDING_STATE -> {
                FilterExpressionMultiValue<AppPackage.OnboardingStateEnum> onboardingState = new FilterExpressionMultiValue<>();
                onboardingState.setKey(getMapping().get(key).getMapping());
                onboardingState.setOperation(OperandMultiValue.fromFilterOperation(operand));
                onboardingState.setValues(createAllOnboardingState(values));
                return onboardingState;
            }
            case OPERATIONAL_STATE -> {
                FilterExpressionMultiValue<AppPackage.OperationalStateEnum> operationalState = new FilterExpressionMultiValue<>();
                operationalState.setKey(getMapping().get(key).getMapping());
                operationalState.setOperation(OperandMultiValue.fromFilterOperation(operand));
                operationalState.setValues(createAllOperationState(values));
                return operationalState;
            }
            case PACKAGE_SECURITY_OPTION -> {
                FilterExpressionMultiValue<AppPackage.PackageSecurityOption> operationalState = new FilterExpressionMultiValue<>();
                operationalState.setKey(getMapping().get(key).getMapping());
                operationalState.setOperation(OperandMultiValue.fromFilterOperation(operand));
                operationalState.setValues(createAllSecurityOptions(values));
                return operationalState;
            }
            case VNFD_ID, VNF_PROVIDER, VNF_PRODUCT_NAME, VNF_SOFTWARE_VERSION, VNFD_VERSION -> {
                FilterExpressionMultiValue<String> stringValue = new FilterExpressionMultiValue<>();
                stringValue.setKey(getMapping().get(key).getMapping());
                stringValue.setOperation(OperandMultiValue.fromFilterOperation(operand));
                stringValue.setValues(values);
                return stringValue;
            }
            default -> {
                FilterExpressionMultiValue<String> stringValue = new FilterExpressionMultiValue<>();
                stringValue.setKey(key);
                stringValue.setOperation(OperandMultiValue.fromFilterOperation(operand));
                stringValue.setValues(values);
                stringValue.setJoinType(JoinType.LEFT);
                return stringValue;
            }
        }
    }

    private static Map<String, MappingData> createMappingData() {
        Map<String, MappingData> mapping = new HashMap<>();
        mapping.put(VNFD_ID, new MappingData("descriptorId", DataType.STRING));
        mapping.put(VNF_PROVIDER, new MappingData("provider", DataType.STRING));
        mapping.put(VNF_PRODUCT_NAME, new MappingData("productName", DataType.STRING));
        mapping.put(VNF_SOFTWARE_VERSION, new MappingData("softwareVersion", DataType.STRING));
        mapping.put(VNFD_VERSION, new MappingData("descriptorVersion", DataType.STRING));
        mapping.put(ONBOARDING_STATE, new MappingData(ONBOARDING_STATE, DataType.ENUMERATION));
        mapping.put(USAGE_STATE, new MappingData(USAGE_STATE, DataType.ENUMERATION));
        mapping.put(OPERATIONAL_STATE, new MappingData(OPERATIONAL_STATE, DataType.ENUMERATION));
        mapping.put(PACKAGE_SECURITY_OPTION, new MappingData(PACKAGE_SECURITY_OPTION, DataType.ENUMERATION));
        return mapping;
    }

}
