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
package com.ericsson.amonboardingservice.presentation.services.fileservice;

import com.ericsson.amonboardingservice.AbstractDbSetupTest;
import com.ericsson.amonboardingservice.TestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class FileServiceTest extends AbstractDbSetupTest {
    @Autowired
    private FileService fileService;

    @Test
    public void testHasRelativePathContent() throws URISyntaxException {
        Path csarFile = TestUtils.getResource("sampledescriptor.csar").toAbsolutePath();
        assertThat(fileService.hasRelativePathContent(csarFile, 15)).isEqualTo(false);
    }

    @Test
    public void testHasRelativePathContentWithRelativePath() throws URISyntaxException {
        Path csarFile = TestUtils.getResource("zip-slip.zip").toAbsolutePath();
        assertThat(fileService.hasRelativePathContent(csarFile, 15)).isEqualTo(true);
    }
}
