package com.margins;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SqlSoftDeleteContractTest {

    @Test
    void sessionSummaryAggregatesIgnoreArchivedWindowRecords() throws IOException {
        String mapper = Files.readString(Path.of("src/main/java/com/margins/session/mapper/ReadingSessionMapper.java"));

        assertThat(mapper)
            .contains("FROM session_windows qsw")
            .contains("WHERE qsw.id = q.window_id")
            .contains("AND qsw.deleted_at IS NULL")
            .contains("FROM session_windows qmw")
            .contains("WHERE qmw.id = qm.window_id")
            .contains("AND qmw.deleted_at IS NULL")
            .contains("FROM session_windows msw")
            .contains("WHERE msw.id = m.window_id")
            .contains("AND msw.deleted_at IS NULL");
    }

    @Test
    void metricSourceAggregatesIgnoreArchivedWindowRecords() throws IOException {
        String mapper = Files.readString(Path.of("src/main/java/com/margins/metric/mapper/MetricMapper.java"));
        String lookupQuery = Files.readString(Path.of("../db/queries/004_metric_sources.sql"));

        assertMetricSourceContract(mapper);
        assertMetricSourceContract(lookupQuery);
    }

    private static void assertMetricSourceContract(String sqlText) {
        assertThat(sqlText)
            .contains("FROM session_windows qsw")
            .contains("WHERE qsw.id = q.window_id")
            .contains("AND qsw.deleted_at IS NULL")
            .contains("FROM session_windows msw")
            .contains("WHERE msw.id = m.window_id")
            .contains("AND msw.deleted_at IS NULL");
    }
}
