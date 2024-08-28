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
package com.ericsson.amonboardingservice.utilities;

import java.util.List;

import static com.ericsson.amonboardingservice.utilities.AccTestUtils.getHost;
import static com.ericsson.amonboardingservice.utilities.AccTestUtils.getNamespace;
import static com.ericsson.amonboardingservice.utilities.AccTestUtils.getRunType;
import static com.ericsson.amonboardingservice.utilities.AccTestUtils.getUserSecretName;

public final class TestConstants {

    public static final String HOST = getHost();
    public static final String BASE_URI = "/api/v1";
    public static final String ETSI_A1_PACKAGE_TO_ONBOARD = "onboardA1Etsi.json";
    public static final String ETSI_SIGNED_A1_PACKAGE_TO_ONBOARD_OPTION1_AND_OPTION2 = "onboardA1EtsiSignedOption1AndOption2.json";
    public static final String ETSI_SIGNED_A1_PACKAGE_TO_ONBOARD_OPTION1 = "onboardA1EtsiSignedOption1.json";
    public static final String ETSI_SIGNED_A1_PACKAGE_TO_ONBOARD_OPTION2 = "onboardA1EtsiSignedOption2.json";
    public static final String PACKAGE_WITH_MULTIPLE_CHARTS = "onboardMultipleCharts.json";
    public static final String TOSCA_1_DOT_3_REL_4_PACKAGE_WITH_MULTIPLE_CHARTS = "onboardTosca1Dot3Rel4MultipleCharts.json";
    public static final String PACKAGE_WITH_SINGLE_CHART_WITHOUT_IMAGE_1_DOT_3 = "onboardTosca1Dot3SingleChart.json";
    public static final String PACKAGE_WITH_MULTI_CHART_FOR_ANY_TRIGGER = "onboardTosca1Dot2MultipleBCharts.json";
    public static final String PACKAGE_WITH_MULTIPLE_VNFDS = "onboardMultipleVnfds.json";
    public static final String MONGO_NGINX_PACKAGE = "mongoNginx.json";
    public static final String NGINX_REDIS_PACKAGE_1 = "nginxRedis.json";
    public static final String NGINX_REDIS_PACKAGE_2 = "nginxRedis2.json";
    public static final String EXTERNAL_CSAR_URI = "externalCsarUri";
    public static final String CREATE_VNF_PACKAGE = "createVnfPackage.json";
    public static final String ETSI_PACKAGE_WITHOUT_IMAGE = "etsiPackageWithoutImage.json";
    public static final String ETSI_PACKAGE_WITHOUT_CHARTS = "packages/etsiPackageWithoutCharts.json";
    public static final String ETSI_PACKAGE_WITH_CRD_WITHOUT_IMAGE = "etsiPackageWithCRDWithoutImage.json";
    public static final String ETSI_PACKAGE_WITH_ROLLBACK_WITHOUT_IMAGE = "etsiPackageWithRollbackWithoutImage.json";
    public static final String CREATE_VNFPACKAGE_WITH_IMAGE_UPLOAD_JSON = "createVNFPackageWithImageUpload.json";
    public static final String CREATE_VNFPACKAGE_SKIP_IMAGE_UPLOAD_JSON = "createVNFPackageWithSkipImageUpload.json";
    public static final String RUN_TYPE = getRunType();
    public static final String RUN_TYPE_LOCAL = "local";
    public static final String NAMESPACE = getNamespace();
    public static final String USER_SECRET_NAME = getUserSecretName();
    public static final String LIGHTWEIGHT_SAME_CHART_PACKAGE_1 = "onboardLightweightSameChart1.json";
    public static final String LIGHTWEIGHT_SAME_CHART_PACKAGE_2 = "onboardLightweightSameChart2.json";
    public static final String ETSI_PACKAGE_WITH_LEVELS_AND_MAPPING = "etsiPackageWithLevelsAndMapping.json";
    public static final String ETSI_PACKAGE_WITH_LEVELS_NO_MAPPING = "etsiPackageWithLevelsNoMapping.json";
    public static final List<String> REAL_NODE_PCC_PACKAGES = List.of("cnf-automation/PCC_CXP9041577_1-R57B75.json");
    public static final List<String> REAL_NODE_PCG_PACKAGES = List.of("cnf-automation/PCG_CXP9041656_1-R57B04.json");
    public static final List<String> REAL_NODE_EDA_PACKAGES = List.of("cnf-automation/aric-act-cna-1.53.200.json");
    public static final List<String> REAL_NODE_CLOUD_RAN_PACKAGES = List.of(
            "cnf-automation/Cloud_RAN_RANS_Discovery-1.455.3-CXF1010090_1-R456D.json",
            "cnf-automation/Cloud_RAN_CU-CP-2.821.2-CXF1010102_2-R822C.json",
            "cnf-automation/Cloud_RAN_CU-UP-1.1878.29-CXF1010103_1-R1879AK.json"
    );

    private TestConstants() {
    }

}
