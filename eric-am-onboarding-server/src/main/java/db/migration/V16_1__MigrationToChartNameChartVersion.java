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

import org.apache.commons.lang3.tuple.Pair;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.ericsson.amonboardingservice.utils.HelmChartUtils;

@SuppressWarnings("squid:S00101")
public class V16_1__MigrationToChartNameChartVersion extends BaseJavaMigration {
    private static final String CHART_URLS_ID_COLUMN = "id";
    private static final String CHART_URLS_CHART_REGISTRY_URL_COLUMN = "charts_registry_url";
    private static final String INSERT_CHART_NAME_CHART_VERSION_SQL = "UPDATE chart_urls SET chart_name = ?, chart_version = ? WHERE id = ?";

    @Override
    public void migrate(final Context context) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        List<Map<String, String>> chartUrls = getAllChartUrls(jdbcTemplate);
        chartUrls.forEach(row -> insertChartNameChartVersion(row, jdbcTemplate));
    }

    private static void insertChartNameChartVersion(final Map<String, String> row, final JdbcTemplate jdbcTemplate) {
        final String chartId = row.get(CHART_URLS_ID_COLUMN);
        final String chartRegistryUrl = row.get(CHART_URLS_CHART_REGISTRY_URL_COLUMN);
        Pair<String, String> chartNameAndChartUrl = HelmChartUtils.parseChartUrl(chartRegistryUrl);
        String chartName = chartNameAndChartUrl.getKey();
        String chartVersion = chartNameAndChartUrl.getValue();
        jdbcTemplate.update(INSERT_CHART_NAME_CHART_VERSION_SQL, chartName, chartVersion, chartId);
    }

    private static List<Map<String, String>> getAllChartUrls(final JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.query("SELECT * FROM chart_urls", new ChartUrlRowMapper());
    }

    private static class ChartUrlRowMapper implements RowMapper<Map<String, String>> {
        @Override
        public Map<String, String> mapRow(final ResultSet resultSet, final int rowNum) throws SQLException {
            Map<String, String> row = new HashMap<>();
            row.put(CHART_URLS_ID_COLUMN, resultSet.getString(CHART_URLS_ID_COLUMN));
            row.put(CHART_URLS_CHART_REGISTRY_URL_COLUMN, resultSet.getString(CHART_URLS_CHART_REGISTRY_URL_COLUMN));
            return row;
        }
    }
}
