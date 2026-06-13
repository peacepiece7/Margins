package com.margins.session.mapper;

import com.margins.session.model.SessionInsightRecord;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SessionInsightMapper {

    @Insert("""
        INSERT INTO session_insights (
          session_id,
          user_id,
          insight_type,
          title,
          content,
          evidence,
          insight_order,
          is_test_data
        )
        VALUES (
          #{sessionId},
          #{userId},
          #{insightType},
          #{title},
          #{content},
          #{evidence},
          #{insightOrder},
          #{testData}
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SessionInsightRecord record);

    @Select("""
        SELECT COALESCE(MAX(insight_order), 0) + 1
        FROM session_insights
        WHERE session_id = #{sessionId}
          AND deleted_at IS NULL
        """)
    int selectNextOrder(Long sessionId);

    @Select("""
        SELECT id, session_id, user_id, insight_type, title, content, evidence, insight_order, is_test_data
        FROM session_insights
        WHERE session_id = #{sessionId}
          AND user_id = #{userId}
          AND deleted_at IS NULL
        ORDER BY insight_order ASC, id ASC
        """)
    List<SessionInsightRecord> findBySessionId(
        @Param("sessionId") Long sessionId,
        @Param("userId") Long userId
    );

    @Update("""
        UPDATE session_insights
        SET deleted_at = CURRENT_TIMESTAMP
        WHERE id = #{insightId}
          AND session_id = #{sessionId}
          AND user_id = #{userId}
          AND deleted_at IS NULL
        """)
    int softDelete(
        @Param("sessionId") Long sessionId,
        @Param("insightId") Long insightId,
        @Param("userId") Long userId
    );
}
