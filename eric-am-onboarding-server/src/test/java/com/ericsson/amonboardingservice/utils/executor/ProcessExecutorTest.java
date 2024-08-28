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
package com.ericsson.amonboardingservice.utils.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static com.ericsson.amonboardingservice.utils.executor.ProcessExecutor.hideSensitiveData;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = { ProcessExecutor.class })
@TestPropertySource(properties = { "docker.registry.user.name=vnfm", "docker.registry.user.password=)WE3'h?!*%$&-_=#Z+&*[-]()$^`OdfoaK9QMH\"vjAX" })
public class ProcessExecutorTest {


    @Value("${docker.registry.user.name}")
    String userName;
    @Value("${docker.registry.user.password}")
    String userPass;
    @Test
    public void hideSensitiveDataTest() {
        String command = "Executing bash -c /dockertarpusher.py https://registry.eostaging.ccd2.athtem.eei.ericsson"
                + ".se /tmp/1726c7d9-9b91-48bd-b817-2f1f72b45030/Files/images/docker.tar "
                + "/tmp/1726c7d9-9b91-48bd-b817-2f1f72b45030/Files/images"
                + "/ab7dc7f42a9c6edbbcf5f52008232bce2906ac164778f5bdf7e83b0271fb00ac/layer.tar https://registry"
                + ".eostaging.ccd2.athtem.eei.ericsson.se/v2/proj-common-assets-cd/monitoring/pm/eric-pm-server/blobs"
                + "/uploads/0c1497dd-90e7-4654-ba9c-e1c846f30f58?_state=hNomPb2pt3PAtKvrM2UiLcpERmIp83eqY4tp"
                +
                "-wiiQVJ7Ik5hbWUiOiJwcm9qLWNvbW1vbi1hc3NldHMtY2QvbW9uaXRvcmluZy9wbS9lcmljLXBtLXNlcnZlciIsIlVVSUQiOiIwYzE0OTdkZC05MGU3LTQ2NTQtYmE5Yy1lMWM4NDZmMzBmNTgiLCJPZmZzZXQiOjAsIlN0YXJ0ZWRBdCI6IjIwMjAtMDgtMTFUMDk6MjI6MTUuNTc1Mjg1M1oifQ%3D%3D vnfm )WE3'h?!*%$&-_=#Z+&*[-]()$^`OdfoaK9QMH\"vjAX";
        String expectedOutput =
                "Executing bash -c /dockertarpusher.py https://registry.eostaging.ccd2.athtem.eei.ericsson"
                        + ".se /tmp/1726c7d9-9b91-48bd-b817-2f1f72b45030/Files/images/docker.tar "
                        + "/tmp/1726c7d9-9b91-48bd-b817-2f1f72b45030/Files/images"
                        + "/ab7dc7f42a9c6edbbcf5f52008232bce2906ac164778f5bdf7e83b0271fb00ac/layer.tar https://registry"
                        + ".eostaging.ccd2.athtem.eei.ericsson"
                        + ".se/v2/proj-common-assets-cd/monitoring/pm/eric-pm-server/blobs"
                        + "/uploads/0c1497dd-90e7-4654-ba9c-e1c846f30f58?_state=hNomPb2pt3PAtKvrM2UiLcpERmIp83eqY4tp"
                        +
                        "-wiiQVJ7Ik5hbWUiOiJwcm9qLWNvbW1vbi1hc3NldHMtY2QvbW9uaXRvcmluZy9wbS9lcmljLXBtLXNlcnZlciIsIlVVSUQiOiIwYzE0OTdkZC05MGU3LTQ2NTQtYmE5Yy1lMWM4NDZmMzBmNTgiLCJPZmZzZXQiOjAsIlN0YXJ0ZWRBdCI6IjIwMjAtMDgtMTFUMDk6MjI6MTUuNTc1Mjg1M1oifQ%3D%3D ****** ******";
        String processedCommand = hideSensitiveData(command);
        assertEquals(processedCommand, expectedOutput);
    }

    @Test
    public void DoesNotHideSensitiveDataTest() {
        String command = "Executing bash -c /dockertarpusher.py https://registry.eostaging.ccd2.athtem.eei.ericsson"
                + ".se /tmp/1726c7d9-9b91-48bd-b817-2f1f72b45030/Files/images/docker.tar";
        String processedCommand = hideSensitiveData(command);
        assertEquals(processedCommand, command);
    }


    @Test
    public void hideSkopeoSpecificCmdSensitiveDataTest() {
        String inputCommandLog = String.format(
                "bash -c skopeo delete --creds=%s:%s --tls-verify=false docker://testRegistry.testRegistrySpace.testWebsite.com/budybox:1.0.0",
                userName,
                userPass
        );

        String sequreCommandLog = "bash -c skopeo delete --creds=******:****** --tls-verify=false docker://testRegistry.testRegistrySpace.testWebsite.com/budybox:1.0.0";

        String processedCommand = hideSensitiveData(inputCommandLog);
        assertEquals(sequreCommandLog, processedCommand);
    }
}
