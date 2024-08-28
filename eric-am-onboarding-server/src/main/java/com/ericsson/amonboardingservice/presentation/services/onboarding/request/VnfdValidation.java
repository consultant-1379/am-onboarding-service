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
package com.ericsson.amonboardingservice.presentation.services.onboarding.request;

import com.ericsson.am.shared.vnfd.VnfdUtility;
import com.ericsson.am.shared.vnfd.model.VnfDescriptorDetails;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.Flavour;
import com.ericsson.amonboardingservice.presentation.exceptions.ErrorMessage;
import com.ericsson.amonboardingservice.presentation.exceptions.FailedOnboardingValidationException;
import com.ericsson.amonboardingservice.presentation.exceptions.UserInputException;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.services.filter.VnfPackageQuery;
import com.ericsson.amonboardingservice.presentation.services.packageservice.OnboardingPackageState;
import com.ericsson.amonboardingservice.presentation.services.vnfdservice.VnfdService;
import com.ericsson.amonboardingservice.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ericsson.amonboardingservice.presentation.services.onboarding.request.OnboardingPhase.VNFD_VALIDATION_PHASE;
import static com.ericsson.amonboardingservice.presentation.services.toscametaservice.ToscaMetaConstants.ENTRY_HELM_DEFINITIONS;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Slf4j
@Component
public class VnfdValidation implements RequestHandler {
    @Autowired
    private VnfdService vnfdService;
    @Autowired
    private VnfPackageQuery vnfPackageQuery;

    @Override
    public void handle(PackageUploadRequestContext context) {
        LOGGER.info("Starting VnfdValidation onboarding handler");

        LOGGER.info("VnfdValidation : Update package with new status {}", OnboardingPackageState.PROCESSING_NOT_IN_USE_DISABLED);

        if (context.isEtsiPackage()) {
            VnfDescriptorDetails vnfDescriptorDetails = vnfdService.validateAndGetVnfDescriptorDetails(context.getArtifactPaths());
            validateDescriptorAlreadyExists(vnfDescriptorDetails, context);
            validateEntryHelmDefinitionsValues(context);
            context.setVnfd(cleanVnfDescriptorDetails(vnfDescriptorDetails));
        }

        if (!context.getErrors().isEmpty()) {
            throw new FailedOnboardingValidationException(context.getErrors());
        }

        LOGGER.info("Successfully finished VnfdValidation onboarding handler");
    }

    @Override
    public String getName() {
        return VNFD_VALIDATION_PHASE.toString();
    }

    private void validateEntryHelmDefinitionsValues(PackageUploadRequestContext context) {
        Optional<Path> entryHelmDefinitionsPath = context.getArtifactPaths().entrySet().stream()
            .filter(file -> file.getKey().equals(ENTRY_HELM_DEFINITIONS)).map(Map.Entry::getValue).findFirst();
        if (entryHelmDefinitionsPath.isPresent()) {
            Supplier<Stream<Path>> entryHelmDefinitionsStreamSupplier = getEntryHelmDefinitionsStreamSupplier(entryHelmDefinitionsPath.get());

            List<Path> helmCharts = getHelmChartsFromEntryHelmDefinitions(entryHelmDefinitionsStreamSupplier.get());
            List<Path> definitionFiles = getDefinitionFilesFromEntryHelmDefinitions(entryHelmDefinitionsStreamSupplier.get());
            checkDefinitionFilesHasHelmCharts(definitionFiles, helmCharts, context);
            definitionFiles.forEach(VnfdUtility::validateYamlCanBeParsed);
        }
    }

    private List<Path> getHelmChartsFromEntryHelmDefinitions(Stream<Path> entryHelmDefinitionsStream) {
        return entryHelmDefinitionsStream.filter(Files::isRegularFile)
            .filter(path -> FilenameUtils.isExtension(path.toString().toLowerCase(), singletonList("tgz")))
            .collect(Collectors.toList());
    }

    private List<Path> getDefinitionFilesFromEntryHelmDefinitions(Stream<Path> entryHelmDefinitionsStream) {
        return entryHelmDefinitionsStream.filter(Files::isRegularFile)
            .filter(path -> !path.toString().contains(Constants.SCALING_MAPPING))
            .filter(path -> FilenameUtils.isExtension(path.toString().toLowerCase(), Arrays.asList("yaml", "yml")))
            .collect(Collectors.toList());
    }

    private Supplier<Stream<Path>> getEntryHelmDefinitionsStreamSupplier(Path entryHelmDefinitionsPath) {
        return () -> {
            try {
                return Files.walk(entryHelmDefinitionsPath);
            } catch (IOException e) {
                LOGGER.error("Unable to walk through the Entry-Helm-Definitions : {} due to : {}", entryHelmDefinitionsPath, e.getMessage());
                throw new UserInputException("Unable to walk through the Entry-Helm-Definitions due to exception: ", e);
            }
        };
    }

    private void checkDefinitionFilesHasHelmCharts(List<Path> definitionFiles, List<Path> helmCharts, PackageUploadRequestContext context) {
        List<String> helmChartBaseNames = helmCharts.stream()
            .map(helmChart -> FilenameUtils.getBaseName(helmChart.getFileName().toString()))
            .collect(Collectors.toList());
        List<Path> definitionFilesDoesNotContainCharts = definitionFiles.stream()
            .filter(definitionFile -> {
                String baseName = FilenameUtils.getBaseName(definitionFile.getFileName().toString());
                return !helmChartBaseNames.contains(baseName);
            })
            .map(Path::getFileName)
            .collect(Collectors.toList());
        if ((helmCharts.isEmpty() && !definitionFiles.isEmpty()) || !definitionFilesDoesNotContainCharts.isEmpty()) {
            context.addErrors(
                new ErrorMessage(format("Unable to Onboard: Following Entry-Helm-Definitions value file(s) does not contain chart(s): %s",
                                        definitionFilesDoesNotContainCharts)
                ));
        }
    }


    private void validateDescriptorAlreadyExists(VnfDescriptorDetails vnfDescriptorDetails, PackageUploadRequestContext context) {
        String vnfDescriptorId = vnfDescriptorDetails.getVnfDescriptorId();
        if (checkIfDescriptorIdPresent(vnfDescriptorId)) {
            context.addErrors(new ErrorMessage(
                format(Constants.PACKAGE_WITH_SAME_DESCRIPTOR_ERROR_MESSAGE, vnfDescriptorId)));
        }
    }

    private boolean checkIfDescriptorIdPresent(String descriptorId) {
        List<AppPackage> packageWithDescriptor = vnfPackageQuery.getAllWithFilter(
            "(eq,packages/appDescriptorId," + descriptorId + ")");
        return (packageWithDescriptor != null && !packageWithDescriptor.isEmpty());
    }

    private VnfDescriptorDetails cleanVnfDescriptorDetails(final VnfDescriptorDetails vnfDescriptorDetails) {
        // reset some fields as they are not designed for serialization and vnfd is serialized as part of onboarding context
        vnfDescriptorDetails.setAllInterfaceTypes(null);
        vnfDescriptorDetails.setAllDataTypes(null);
        // later in the handler chain it is only important if flavours is not empty or not null
        vnfDescriptorDetails.setFlavours(createFlavoursSubstitute(vnfDescriptorDetails));
        vnfDescriptorDetails.setDefaultFlavour(null);

        return vnfDescriptorDetails;
    }

    private static Map<String, Flavour> createFlavoursSubstitute(final VnfDescriptorDetails vnfDescriptorDetails) {
        if (vnfDescriptorDetails.getFlavours() == null) {
            return null;
        }

        return vnfDescriptorDetails.getFlavours().keySet().stream()
            .collect(toMap(identity(), key -> new Flavour()));
    }
}