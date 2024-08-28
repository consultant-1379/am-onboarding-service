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

import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.presentation.models.vnfd.AppUserDefinedData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("squid:S00101")
@Slf4j
public class V12_2__MigrateUserDefinedData extends BaseJavaMigration {

    private static final String USER_DEFINED_DATA_TALE_NAME = "app_user_defined_data";
    private static final String ID_COLUMN_NAME = "id";
    private static final String PACKAGE_ID_COLUMN_NAME = "package_id";
    private static final String KEY_COLUMN_NAME = "user_key";
    private static final String VALUE_COLUMN_NAME = "user_value";
    private static final String APP_PACKAGE_INFO_COLUMN_NAME_USER_DEFINED_DATA = "user_defined_data";

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public void migrate(final Context context) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        getAllPackageInfo(jdbcTemplate).stream()
                .filter(row -> StringUtils.isNotEmpty(row.get(APP_PACKAGE_INFO_COLUMN_NAME_USER_DEFINED_DATA)))
                .forEach(row -> migrateToUserDefinedDataObject(row, jdbcTemplate));
    }

    private void migrateToUserDefinedDataObject(final Map<String, String> row, final JdbcTemplate jdbcTemplate) {
        try {
            Map<String, String> userDefinedDataMap =
                    mapper.readValue(new JSONObject(row.get(APP_PACKAGE_INFO_COLUMN_NAME_USER_DEFINED_DATA)).toString(),
                            new TypeReference<Map<String, String>>() { });
            List<AppUserDefinedData> userDefinedData = new ArrayList<>();
            for (Map.Entry<String, String> entry : userDefinedDataMap.entrySet()) {
                AppUserDefinedData data = new AppUserDefinedData();
                data.setKey(entry.getKey());
                data.setValue(userDefinedDataMap.get(entry.getKey()));
                userDefinedData.add(data);
            }
            persistUserDefinedData(jdbcTemplate, userDefinedData, row.get(PACKAGE_ID_COLUMN_NAME));
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to parse Json string representation of userDefinedData: {}",
                    row.get(APP_PACKAGE_INFO_COLUMN_NAME_USER_DEFINED_DATA), e);
            throw new InternalRuntimeException(e);
        }
    }

    private static List<Map<String, String>> getAllPackageInfo(final JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.query("SELECT * FROM app_packages", new PackageInfoRowMapper());
    }

    private static void persistUserDefinedData(final JdbcTemplate jdbcTemplate,
                                  final List<AppUserDefinedData> userDefinedDataList,
                                  final String packageId) {
        SimpleJdbcInsert insertIntoUserDefinedData = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName(USER_DEFINED_DATA_TALE_NAME);
        final List<String> listOfIds = jdbcTemplate.query("SELECT * FROM " + USER_DEFINED_DATA_TALE_NAME,
                new UserDefinedDataRowMapper());
        for (AppUserDefinedData userDefinedData : userDefinedDataList) {
            Map<String, Object> userDefinedDataParameters = new HashMap<>();
            userDefinedDataParameters.put(ID_COLUMN_NAME, generateUUIDUniqueToTheTable(listOfIds));
            userDefinedDataParameters.put(PACKAGE_ID_COLUMN_NAME, packageId);
            userDefinedDataParameters.put(KEY_COLUMN_NAME, userDefinedData.getKey());
            userDefinedDataParameters.put(VALUE_COLUMN_NAME, userDefinedData.getValue());
            insertIntoUserDefinedData.execute(userDefinedDataParameters);
        }
    }

    private static String generateUUIDUniqueToTheTable(final List<String> ids) {
        String uuid = UUID.randomUUID().toString();
        if (ids.contains(uuid)) {
            generateUUIDUniqueToTheTable(ids);
        }
        return uuid;
    }

    private static class UserDefinedDataRowMapper implements RowMapper<String> {

        @Override
        public String mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return rs.getString(ID_COLUMN_NAME);
        }
    }

    private static class PackageInfoRowMapper implements RowMapper<Map<String, String>> {

        @Override
        public Map<String, String> mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            Map<String, String> row = new HashMap<>();
            row.put(PACKAGE_ID_COLUMN_NAME, rs.getString(PACKAGE_ID_COLUMN_NAME));
            row.put(APP_PACKAGE_INFO_COLUMN_NAME_USER_DEFINED_DATA,
                    rs.getString(APP_PACKAGE_INFO_COLUMN_NAME_USER_DEFINED_DATA));
            return row;
        }
    }
}
