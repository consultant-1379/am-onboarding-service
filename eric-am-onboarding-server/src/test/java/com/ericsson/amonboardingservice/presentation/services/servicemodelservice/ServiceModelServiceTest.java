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
package com.ericsson.amonboardingservice.presentation.services.servicemodelservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import com.ericsson.am.shared.vnfd.model.VnfDescriptorDetails;
import com.ericsson.am.shared.vnfd.model.servicemodel.ServiceModel;
import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.model.ServiceModelRecordResponse;
import com.ericsson.amonboardingservice.presentation.models.ServiceModelRecordEntity;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppPackage;
import com.ericsson.amonboardingservice.presentation.repositories.ServiceModelRecordRepository;
import com.ericsson.amonboardingservice.presentation.services.onboarding.request.PackageUploadRequestContext;

@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource( properties = {
        "spring.flyway.enabled = false",
        "spring.datasource.initialize=false"
})
public class ServiceModelServiceTest extends AbstractDbSetupTest {

    private static final String SERVICE_MODEL_ID = "ab0d2626-f21b-4d2a-b4dc-13e4447bb1ee";
    private static final String NON_EXISTING_SERVICE_MODEL_ID = "non-existing-id";
    private static final String SERVICE_MODEL_NAME = "service-model-name";
    private static final String DESCRIPTOR_ID = "4141d230-55d8-49f5-8529-a9ab76902153";


    @Autowired
    private ServiceModelService serviceModelService;
    @MockBean
    private ServiceModelRecordRepository serviceModelRepository;
    private ServiceModelRecordEntity serviceModelEntity;
    private AppPackage appPackage;
    private PackageUploadRequestContext packageUploadRequestContext;

    @BeforeEach
    public void setUp(){
        setUpAppPackageEntity();
        setUpServiceModelEntity();
        setUpPackageUploadRequestContext();

        when(serviceModelRepository.findByAppPackagePackageId(SERVICE_MODEL_ID))
                .thenReturn(Optional.of(serviceModelEntity));
        when(serviceModelRepository.findByAppPackagePackageId(NON_EXISTING_SERVICE_MODEL_ID))
                .thenReturn(Optional.empty());
    }

    @Test
    public void testGetServiceModelByValidPackageId(){
        ServiceModelRecordEntity actual;
        actual = serviceModelService.getServiceModelByPackageId(SERVICE_MODEL_ID).orElse(null);

        assertThat(actual).isNotNull();
        assertThat(actual.getServiceModelId()).isEqualTo(SERVICE_MODEL_ID);
        assertThat(actual.getServiceModelName()).isEqualTo(SERVICE_MODEL_NAME);
    }

    @Test
    public void testGetServiceModelByNotValidPackageId(){
        ServiceModelRecordEntity actual;
        actual = serviceModelService.getServiceModelByPackageId(NON_EXISTING_SERVICE_MODEL_ID).orElse(null);

        assertThat(actual).isNull();
    }

    @Test
    public void testGetServiceModelResponseByValidPackageId(){
        ServiceModelRecordResponse actual;
        actual = serviceModelService.getServiceModelResponseByPackageId(SERVICE_MODEL_ID).orElse(null);

        assertThat(actual).isNotNull();
        assertThat(actual.getServiceModelId()).isEqualTo(SERVICE_MODEL_ID);
        assertThat(actual.getServiceModelName()).isEqualTo(SERVICE_MODEL_NAME);
    }

    @Test
    public void testGetServiceModelRecordByNotValidPackageId(){
        ServiceModelRecordResponse actual;
        actual = serviceModelService.getServiceModelResponseByPackageId(NON_EXISTING_SERVICE_MODEL_ID).orElse(null);

        assertThat(actual).isNull();
    }

    @Test
    public void testSaveServiceModelFromRequestContextIfServiceModelAndVnfdPresent() {
        ServiceModelRecordEntity actual;
        when(serviceModelRepository.save(any(ServiceModelRecordEntity.class))).thenReturn(serviceModelEntity);

        actual = serviceModelService.saveServiceModelFromRequestContext(packageUploadRequestContext, appPackage).orElse(null);

        assertThat(actual).isNotNull();
        assertThat(actual.getDescriptorId()).isEqualTo(DESCRIPTOR_ID);
        assertThat(actual.getServiceModelId()).isEqualTo(SERVICE_MODEL_ID);
    }

    @Test
    public void testSaveServiceModelFromRequestContextWithoutServiceModel(){
        ServiceModelRecordEntity actual;
        packageUploadRequestContext.setServiceModel(null);
        actual = serviceModelService.saveServiceModelFromRequestContext(packageUploadRequestContext, appPackage).orElse(null);

        verify(serviceModelRepository, times(0)).save(any(ServiceModelRecordEntity.class));
        assertThat(actual).isNull();
    }

    private void setUpServiceModelEntity() {
        String id = String.valueOf(System.currentTimeMillis());
        serviceModelEntity = new ServiceModelRecordEntity();
        serviceModelEntity.setServiceModelId(SERVICE_MODEL_ID);
        serviceModelEntity.setServiceModelName(SERVICE_MODEL_NAME);
        serviceModelEntity.setAppPackage(appPackage);
        serviceModelEntity.setDescriptorId(DESCRIPTOR_ID);
        ReflectionTestUtils.setField(serviceModelEntity, "id", id);
    }

    private void setUpAppPackageEntity() {
        appPackage = new AppPackage();
        appPackage.setPackageId("package" + System.currentTimeMillis());
    }

    private void setUpPackageUploadRequestContext() {
        packageUploadRequestContext = new PackageUploadRequestContext("testCsar.csar", null, null, null);
        VnfDescriptorDetails vnfd = new VnfDescriptorDetails();
        vnfd.setVnfDescriptorId(DESCRIPTOR_ID);
        packageUploadRequestContext.setVnfd(vnfd);
        ServiceModel serviceModel = new ServiceModel();
        serviceModel.setId(SERVICE_MODEL_ID);
        serviceModel.setName(SERVICE_MODEL_NAME);
        packageUploadRequestContext.setServiceModel(serviceModel);
    }
}
