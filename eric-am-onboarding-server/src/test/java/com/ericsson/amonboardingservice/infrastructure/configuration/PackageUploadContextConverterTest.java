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
package com.ericsson.amonboardingservice.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.ericsson.am.shared.vnfd.model.HelmChart;
import com.ericsson.am.shared.vnfd.model.HelmChartType;
import com.ericsson.am.shared.vnfd.model.ImageDetails;
import com.ericsson.am.shared.vnfd.model.VnfDescriptorDetails;
import com.ericsson.amonboardingservice.presentation.exceptions.ConvertingPackageUploadContextException;
import com.ericsson.amonboardingservice.presentation.exceptions.ErrorMessage;
import com.ericsson.amonboardingservice.presentation.models.converter.PackageUploadContextConverter;
import com.ericsson.amonboardingservice.presentation.services.onboarding.request.PackageUploadRequestContext;

public class PackageUploadContextConverterTest {

    private static final String DUMMY_JSON = prepareTestJson();

    private final PackageUploadContextConverter packageUploadContextConverter = new PackageUploadContextConverter();

    @Test
    public void testConvertToDatabaseColumn() {
        PackageUploadRequestContext packageUploadRequestContext = createPackageUploadRequestContext();

        String convertedEntity = packageUploadContextConverter.convertToDatabaseColumn(packageUploadRequestContext);
        assertThat(convertedEntity).isEqualTo(DUMMY_JSON);
    }

    @Test
    public void testConvertToDatabaseColumnWithEmptyObject() {
        PackageUploadRequestContext packageUploadRequestContext = new PackageUploadRequestContext();
        assertThatNoException().isThrownBy(() -> packageUploadContextConverter.convertToDatabaseColumn(packageUploadRequestContext));
    }

    @Test
    public void testConvertToDatabaseColumnWithNull() {
        String result = packageUploadContextConverter.convertToDatabaseColumn(null);
        assertThat(result).isEqualTo("null");
    }

    @Test
    public void testConvertToEntityAttribute() {
        PackageUploadRequestContext result = packageUploadContextConverter.convertToEntityAttribute(DUMMY_JSON);
        assertThat(result.getOriginalFileName()).contains("someFileName");
        assertThat(result.getPackageContents()).isEqualTo(Paths.get("some/path").toAbsolutePath());
        assertThat(result.getTimeoutDate()).isEqualTo("2022-03-15T11:23:00");
        assertThat(result.getPackageId()).isEqualTo("somePackageId");
        assertThat(result.getVnfd()).satisfies(vnfDescriptorDetails -> {
            assertThat(vnfDescriptorDetails.getHelmCharts()).extracting(HelmChart::getPath, HelmChart::getChartType, HelmChart::getChartKey)
                    .containsOnly(tuple("/path/to/chart", HelmChartType.CNF, "chartKey"));
            assertThat(vnfDescriptorDetails.getDescriptorModel()).isEqualTo("descriptor-model");
            assertThat(vnfDescriptorDetails.getImagesDetails()).extracting(ImageDetails::getPath, ImageDetails::getResourceId)
                    .containsOnly(tuple("image/path", "imageResourceId"));
        });
    }

    @Test
    public void testConvertToEntityAttributeWithCorruptJson() {
        String corruptJson = "{\"helmCharts\":{\"dummy/path\"}";

        assertThatThrownBy(() -> packageUploadContextConverter.convertToEntityAttribute(corruptJson))
                .isInstanceOf(ConvertingPackageUploadContextException.class)
                .hasMessageContaining("Unable to convert json to PackageUploadRequestContext due to:");
    }

    @Test
    public void testConvertToEntityAttributeWithEmptyJson() {
        assertThatThrownBy(() -> packageUploadContextConverter.convertToEntityAttribute(""))
                .isInstanceOf(ConvertingPackageUploadContextException.class)
                .hasMessageContaining("Unable to convert json to PackageUploadRequestContext due to:");
    }

    @Test
    public void testConvertToEntityAttributeWithNull() {
        assertThatThrownBy(() -> packageUploadContextConverter.convertToEntityAttribute(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("argument \"content\" is null");
    }

    private static String prepareTestJson() {
        String stringToConvert = "{\"helmChartPaths\":[\"%s\"],"
                + "\"helmfile\":null,\"originalFileName\":\"someFileName\","
                + "\"packageContents\":\"%s\","
                + "\"checksum\":null,\"timeoutDate\":\"2022-03-15T11:23:00\",\"artifactPaths\":null,\"packageId\":\"somePackageId\","
                + "\"vnfd\":{\"vnfDescriptorId\":null,\"vnfDescriptorVersion\":null,\"vnfProvider\":null,\"vnfProductName\":null,"
                + "\"vnfSoftwareVersion\":null,\"vnfmInfo\":null,\"descriptorModel\":\"descriptor-model\","
                + "\"helmCharts\":[{\"path\":\"/path/to/chart\",\"chartType\":\"CNF\",\"chartKey\":\"chartKey\"}],"
                + "\"imagesDetails\":[{\"path\":\"image/path\",\"resourceId\":\"imageResourceId\"}],"
                + "\"allDataTypes\":null,\"allInterfaceTypes\":null,\"flavours\":null,\"defaultFlavour\":null},"
                + "\"helmChartStatus\":null,\"etsiPackage\":false,\"renamedImageList\":[],\"serviceModel\":null,"
                + "\"toscaVersion\":null,\"errors\":[{\"message\":\"someErrorMessage\"}],\"packageSigned\":false,"
                + "\"packageSecurityOption\":null}";
        return String.format(stringToConvert, Paths.get("dummy/path").toUri(), Paths.get("some/path").toUri());
    }

    private static PackageUploadRequestContext createPackageUploadRequestContext() {
        PackageUploadRequestContext packageUploadRequestContext =
                new PackageUploadRequestContext("someFileName",
                                                Paths.get("some/path"),
                                                LocalDateTime.of(2022, 3, 15, 11, 23),
                                                "somePackageId");
        packageUploadRequestContext.setHelmChartPaths(Set.of(Paths.get("dummy/path")));
        packageUploadRequestContext.addErrors(new ErrorMessage("someErrorMessage"));
        packageUploadRequestContext.setVnfd(createVnfd());

        return packageUploadRequestContext;
    }

    private static VnfDescriptorDetails createVnfd() {
        final VnfDescriptorDetails vnfDescriptorDetails = new VnfDescriptorDetails();
        vnfDescriptorDetails.setDescriptorModel("descriptor-model");
        vnfDescriptorDetails.setHelmCharts(List.of(new HelmChart("/path/to/chart", HelmChartType.CNF, "chartKey")));
        vnfDescriptorDetails.setImagesDetails(List.of(new ImageDetails("image/path", "imageResourceId")));

        return vnfDescriptorDetails;
    }
}
