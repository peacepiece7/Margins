package com.margins.metric.mapper;

import com.margins.metric.model.MetricRecord;
import com.margins.metric.model.SessionMetricSourceRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MetricMapper {

    @Select("""
        SELECT
          rs.user_id,
          rs.book_id,
          rs.id AS session_id,
          rs.start_page,
          rs.current_page,
          rs.target_page,
          CASE
            WHEN rs.start_page IS NOT NULL AND rs.current_page IS NOT NULL
            THEN GREATEST(rs.current_page - rs.start_page + 1, 0)
            ELSE NULL
          END AS pages_read_estimate,
          COUNT(DISTINCT sw.id) AS window_count,
          COUNT(DISTINCT q.id) AS question_count,
          COUNT(DISTINCT CASE WHEN m.role = 'user' AND m.question_id IS NOT NULL THEN m.question_id END) AS answered_question_count,
          COUNT(DISTINCT h.id) AS highlight_count,
          COUNT(DISTINCT m.id) AS message_count,
          COUNT(DISTINCT m.persona_id) AS persona_count
        FROM reading_sessions rs
        INNER JOIN books b ON b.id = rs.book_id AND b.deleted_at IS NULL
        LEFT JOIN session_windows sw ON sw.session_id = rs.id AND sw.deleted_at IS NULL
        LEFT JOIN questions q ON q.session_id = rs.id
          AND q.deleted_at IS NULL
          AND (
            q.window_id IS NULL
            OR EXISTS (
              SELECT 1
              FROM session_windows qsw
              WHERE qsw.id = q.window_id
                AND qsw.deleted_at IS NULL
            )
          )
        LEFT JOIN session_highlights h ON h.session_id = rs.id AND h.deleted_at IS NULL
        LEFT JOIN messages m ON m.session_id = rs.id
          AND m.deleted_at IS NULL
          AND EXISTS (
            SELECT 1
            FROM session_windows msw
            WHERE msw.id = m.window_id
              AND msw.deleted_at IS NULL
          )
        WHERE rs.id = #{sessionId}
          AND rs.user_id = #{userId}
          AND rs.deleted_at IS NULL
        GROUP BY rs.user_id, rs.book_id, rs.id, rs.start_page, rs.current_page, rs.target_page
        """)
    SessionMetricSourceRecord findSessionSource(
        @Param("sessionId") Long sessionId,
        @Param("userId") Long userId
    );

    @Insert("""
        INSERT INTO metrics (
          user_id,
          book_id,
          session_id,
          metric_name,
          metric_scope,
          metric_period_start,
          metric_period_end,
          metric_value,
          metric_unit,
          metric_details,
          source_ref,
          generated_by,
          is_test_data
        )
        VALUES (
          #{userId},
          #{bookId},
          #{sessionId},
          #{metricName},
          #{metricScope},
          CURRENT_DATE,
          CURRENT_DATE,
          #{metricValue},
          #{metricUnit},
          #{metricDetails},
          #{sourceRef},
          #{generatedBy},
          #{testData}
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MetricRecord record);
}
