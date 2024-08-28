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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V10_2__MigrateChartRegistryUrlData extends BaseJavaMigration {

    private static final String CHARTS_REGISTRY_URL_COLUMN = "charts_registry_url";
    private static final String PACKAGE_ID_COLUMN = "package_id";
    private static final String CHART_URLS_TABLE = "chart_urls";
    private static final String ID_COLUMN = "id";
    private static final String PRIORITY_COLUMN = "priority";

    private static String generateUUIDUniqueToTheTable(final List<String> ids) {
        String uuid = UUID.randomUUID().toString();
        if (ids.contains(uuid)) {
            generateUUIDUniqueToTheTable(ids);
        }
        return uuid;
    }

    @Override
    public void migrate(final Context context) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        getAllPackages(jdbcTemplate).stream()
                .filter(row -> StringUtils.isNotEmpty(row.get(CHARTS_REGISTRY_URL_COLUMN).toString()))
                .forEach(row -> persistChartUrls(row, jdbcTemplate));
    }

    private List<Map<String, Object>> getAllPackages(final JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.query("SELECT * FROM app_packages", new AppPackageRowMapper());
    }

    private void persistChartUrls(final Map<String, Object> row, final JdbcTemplate jdbcTemplate) {
        SimpleJdbcInsert insertIntoChartsUrl = new SimpleJdbcInsert(jdbcTemplate).withTableName(CHART_URLS_TABLE);
        final List<String> listOfIds = jdbcTemplate
                .query("SELECT * FROM " + CHART_URLS_TABLE, new ChartUrlsRowMapper());
        Map<String, Object> chartUrlParams = new HashMap<>();
        chartUrlParams.put(ID_COLUMN, generateUUIDUniqueToTheTable(listOfIds));
        chartUrlParams.put(PACKAGE_ID_COLUMN, row.get(PACKAGE_ID_COLUMN));
        chartUrlParams.put(CHARTS_REGISTRY_URL_COLUMN, row.get(CHARTS_REGISTRY_URL_COLUMN));
        chartUrlParams.put(PRIORITY_COLUMN, 1);
        insertIntoChartsUrl.execute(chartUrlParams);
    }

    private class AppPackageRowMapper implements RowMapper<Map<String, Object>> {

        @Override
        public Map<String, Object> mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            Map<String, Object> row = new HashMap<>();
            row.put(PACKAGE_ID_COLUMN, rs.getString(PACKAGE_ID_COLUMN));
            row.put(CHARTS_REGISTRY_URL_COLUMN, rs.getString(CHARTS_REGISTRY_URL_COLUMN));
            return row;
        }
    }

    private class ChartUrlsRowMapper implements RowMapper<String> {

        @Override
        public String mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return rs.getString(ID_COLUMN);
        }
    }
}
