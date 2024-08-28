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
package db.migration;

import com.ericsson.amonboardingservice.presentation.exceptions.UnhandledException;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileSystemService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("squid:S00101")
@Slf4j
public class V14_1__MigrationToMultipleVnfdSchema extends BaseJavaMigration {

    private static final String PACKAGE_ID_COLUMN_NAME = "package_id";
    private static final String DESCRIPTOR_MODEL = "descriptor_model";
    private static final String UPDATE_SQL = "update app_packages set vnfd_zip = ? WHERE package_id = ?";
    public static final String VNFD_FILE_NAME = "cnf_vnfd.yaml";
    public static final String INVALID_VNFD_JSON_ERROR_MESSAGE = "Invalid vnfd json present in package, Failed " +
            "due to %s";

    @Override
    public void migrate(final Context context) {
        FileSystemService fileService = new FileSystemService();
        fileService.setRootDirectory(System.getProperty("java.io.tmpdir"));
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        getAllPackageInfo(jdbcTemplate).stream()
                .filter(row -> StringUtils.isNotEmpty(row.get(DESCRIPTOR_MODEL)))
                .forEach(row -> migrateVnfDModelToVnfdZip(row, jdbcTemplate, fileService));
    }

    private static void migrateVnfDModelToVnfdZip(final Map<String, String> row, final JdbcTemplate jdbcTemplate,
                                                FileSystemService fileService) {
        String descriptorModel = row.get(DESCRIPTOR_MODEL);
        Path yamlDirectory = null;
        Path zipDirectoryFile = null;
        try {
            JsonNode jsonNodeTree = new ObjectMapper().readTree(descriptorModel);
            String jsonAsYaml = new YAMLMapper().writeValueAsString(jsonNodeTree);
            String yamlDirectoryName = UUID.randomUUID().toString();
            yamlDirectory = fileService.createDirectory(yamlDirectoryName);
            Path vnfdFile = fileService.storeFile(new ByteArrayInputStream(jsonAsYaml
                    .getBytes(StandardCharsets.UTF_8)), yamlDirectory, VNFD_FILE_NAME);
            zipDirectoryFile = fileService.zipDirectory(vnfdFile.getParent());
            byte[] vnfdZip = Files.readAllBytes(zipDirectoryFile);
            jdbcTemplate.update(UPDATE_SQL, vnfdZip, row.get(PACKAGE_ID_COLUMN_NAME));
        } catch (JsonProcessingException jme) {
            LOGGER.error(String.format(INVALID_VNFD_JSON_ERROR_MESSAGE, jme.getMessage()));
            throw new UnhandledException(String.format(INVALID_VNFD_JSON_ERROR_MESSAGE, jme.getMessage()), jme);
        } catch (IOException ioe) {
            LOGGER.error("Unable to create zip file", ioe);
            throw new UnhandledException("Unable to create zip file", ioe);
        } finally {
            if (yamlDirectory != null) {
                fileService.deleteDirectory(yamlDirectory.toString());
            }
            if (zipDirectoryFile != null) {
                fileService.deleteDirectory(zipDirectoryFile.getParent().toString());
            }
        }
    }

    private static List<Map<String, String>> getAllPackageInfo(final JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.query("SELECT * FROM app_packages", new PackageInfoRowMapper());
    }

    private static class PackageInfoRowMapper implements RowMapper<Map<String, String>> {

        @Override
        public Map<String, String> mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            Map<String, String> row = new HashMap<>();
            row.put(PACKAGE_ID_COLUMN_NAME, rs.getString(PACKAGE_ID_COLUMN_NAME));
            row.put(DESCRIPTOR_MODEL, rs.getString(DESCRIPTOR_MODEL));
            return row;
        }
    }
}
