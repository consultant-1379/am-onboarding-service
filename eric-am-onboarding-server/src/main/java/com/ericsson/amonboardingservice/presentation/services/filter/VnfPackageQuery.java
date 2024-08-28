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
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VnfPackageQuery extends CreateQueryFilter<AppPackage, AppPackageRepository> {

    private static final String USAGE_STATE = "packages/usageState";
    private static final String ONBOARDING_STATE = "packages/onboardingState";
    private static final String PACKAGE_SECURITY_OPTION = "packages/packageSecurityOption";

    public VnfPackageQuery(AppPackageRepository appPackageRepository) {
        super(createMappingData(), appPackageRepository);
    }

    @Override
    public Page<AppPackage> getPageWithFilter(String filter, Pageable pageable) {
        return jpaRepository.findAll(
                getSpecification(filter).and(isNotSetForDeletion()),
                pageable
        );
    }

    public Specification<AppPackage> isNotSetForDeletion() {
        return (root, query, criteriaBuilder) -> {
            Predicate isNull = criteriaBuilder.isNull(root.get("isSetForDeletion"));
            Predicate isFalse = criteriaBuilder.isFalse(root.get("isSetForDeletion"));
            return criteriaBuilder.or(isNull, isFalse);
        };
    }

    @Override
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
            case PACKAGE_SECURITY_OPTION -> {
                FilterExpressionOneValue<AppPackage.PackageSecurityOption> packageSecurityOption = new FilterExpressionOneValue<>();
                packageSecurityOption.setKey(getMapping().get(key).getMapping());
                packageSecurityOption.setOperation(OperandOneValue.fromFilterOperation(operand));
                validateDataType(DataType.ENUMERATION, value, AppPackage.PackageSecurityOption.class);
                packageSecurityOption.setValue(AppPackage.PackageSecurityOption.valueOf(value));
                return packageSecurityOption;
            }
            default -> {
                FilterExpressionOneValue<String> stringValue = new FilterExpressionOneValue<>();
                stringValue.setKey(getMapping().get(key).getMapping());
                stringValue.setOperation(OperandOneValue.fromFilterOperation(operand));
                stringValue.setValue(value);
                return stringValue;
            }
        }
    }

    @Override
    public FilterExpressionMultiValue createFilterExpressionMultiValue(
            String key, List<String> values, String operand) {
        switch (key) {
            case USAGE_STATE -> {
                FilterExpressionMultiValue<AppPackage.AppUsageStateEnum> usageState = new FilterExpressionMultiValue<>();
                usageState.setKey(getMapping().get(key).getMapping());
                usageState.setOperation(OperandMultiValue.fromFilterOperation(operand));
                List<AppPackage.AppUsageStateEnum> allUsageState = new ArrayList<>();
                for (String value : values) {
                    validateDataType(DataType.ENUMERATION, value, AppPackage.AppUsageStateEnum.class);
                    allUsageState.add(AppPackage.AppUsageStateEnum.valueOf(value));
                }
                usageState.setValues(allUsageState);
                return usageState;
            }
            case ONBOARDING_STATE -> {
                FilterExpressionMultiValue<AppPackage.OnboardingStateEnum> onboardingState = new FilterExpressionMultiValue<>();
                onboardingState.setKey(getMapping().get(key).getMapping());
                onboardingState.setOperation(OperandMultiValue.fromFilterOperation(operand));
                List<AppPackage.OnboardingStateEnum> allOnboardingState = new ArrayList<>();
                for (String value : values) {
                    validateDataType(DataType.ENUMERATION, value, AppPackage.OnboardingStateEnum.class);
                    allOnboardingState.add(AppPackage.OnboardingStateEnum.valueOf(value));
                }
                onboardingState.setValues(allOnboardingState);
                return onboardingState;
            }
            case PACKAGE_SECURITY_OPTION -> {
                FilterExpressionMultiValue<AppPackage.PackageSecurityOption> packageSecurityOption = new FilterExpressionMultiValue<>();
                packageSecurityOption.setKey(getMapping().get(key).getMapping());
                packageSecurityOption.setOperation(OperandMultiValue.fromFilterOperation(operand));
                List<AppPackage.PackageSecurityOption> allPackageSecurityOptions = new ArrayList<>();
                for (String value : values) {
                    validateDataType(DataType.ENUMERATION, value, AppPackage.PackageSecurityOption.class);
                    allPackageSecurityOptions.add(AppPackage.PackageSecurityOption.valueOf(value));
                }
                packageSecurityOption.setValues(allPackageSecurityOptions);
                return packageSecurityOption;
            }
            default -> {
                FilterExpressionMultiValue<String> stringValue = new FilterExpressionMultiValue<>();
                stringValue.setKey(getMapping().get(key).getMapping());
                stringValue.setOperation(OperandMultiValue.fromFilterOperation(operand));
                stringValue.setValues(values);
                return stringValue;
            }
        }
    }

    private static Map<String, MappingData> createMappingData() {
        Map<String, MappingData> mapping = new HashMap<>();
        mapping.put("packages/appPkgId", new MappingData("packageId", DataType.STRING));
        mapping.put("packages/appDescriptorId", new MappingData("descriptorId", DataType.STRING));
        mapping.put("packages/appProvider", new MappingData("provider", DataType.STRING));
        mapping.put("packages/appProductName", new MappingData("productName", DataType.STRING));
        mapping.put("packages/appSoftwareVersion", new MappingData("softwareVersion", DataType.STRING));
        mapping.put("packages/descriptorVersion", new MappingData("descriptorVersion", DataType.STRING));
        mapping.put(ONBOARDING_STATE, new MappingData("onboardingState", DataType.ENUMERATION));
        mapping.put(USAGE_STATE, new MappingData("usageState", DataType.ENUMERATION));
        mapping.put(PACKAGE_SECURITY_OPTION, new MappingData("packageSecurityOption", DataType.ENUMERATION));
        return mapping;
    }

}
