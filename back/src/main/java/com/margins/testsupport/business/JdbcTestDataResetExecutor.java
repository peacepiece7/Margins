package com.margins.testsupport.business;

import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JdbcTestDataResetExecutor implements TestDataResetExecutor {

    private final DataSource dataSource;

    @Value("${margins.test-support.seed-script:../db/seed/001_seed_mvp_data.sql}")
    private String seedScriptPath;

    @Override
    public void resetTestData() {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            deleteTestData(connection);
            ScriptUtils.executeSqlScript(
                connection,
                new EncodedResource(new FileSystemResource(seedScriptPath), "UTF-8")
            );
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private void deleteTestData(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS = 0");
            try {
                statement.executeUpdate("DELETE FROM metrics WHERE is_test_data = TRUE");
                statement.executeUpdate("DELETE FROM messages WHERE is_test_data = TRUE");
                statement.executeUpdate("DELETE FROM reading_session_reviews WHERE is_test_data = TRUE");
                statement.executeUpdate("DELETE FROM session_insights WHERE is_test_data = TRUE");
                statement.executeUpdate("DELETE FROM session_tags WHERE is_test_data = TRUE");
                statement.executeUpdate("DELETE FROM session_highlights WHERE is_test_data = TRUE");
                statement.executeUpdate("DELETE FROM questions WHERE is_test_data = TRUE");
                statement.executeUpdate("DELETE FROM personas WHERE is_test_data = TRUE");
                statement.executeUpdate("DELETE FROM session_windows WHERE is_test_data = TRUE");
                statement.executeUpdate("DELETE FROM reading_sessions WHERE is_test_data = TRUE");
                statement.executeUpdate("DELETE FROM book_candidates WHERE is_test_data = TRUE");
                statement.executeUpdate("DELETE FROM books WHERE is_test_data = TRUE");
                statement.executeUpdate("DELETE FROM users WHERE is_test_data = TRUE");
            } finally {
                statement.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to delete test data", exception);
        }
    }
}
