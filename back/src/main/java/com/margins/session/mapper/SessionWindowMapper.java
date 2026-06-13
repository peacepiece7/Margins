package com.margins.session.mapper;

import com.margins.session.model.SessionWindowContext;
import com.margins.session.model.SessionWindowRecord;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SessionWindowMapper {

    @Insert("""
        INSERT INTO session_windows (
          session_id,
          user_id,
          window_type,
          title,
          position,
          status,
          is_test_data
        )
        VALUES (
          #{sessionId},
          #{userId},
          #{windowType},
          #{title},
          #{position},
          #{status},
          #{testData}
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SessionWindowRecord record);

    @Select("""
        SELECT
          id,
          session_id,
          user_id,
          window_type,
          title,
          position,
          status,
          is_test_data
        FROM session_windows
        WHERE id = #{id}
          AND deleted_at IS NULL
        """)
    SessionWindowRecord findById(Long id);

    @Select("""
        SELECT
          id,
          session_id,
          user_id
        FROM session_windows
        WHERE id = #{id}
          AND deleted_at IS NULL
        """)
    SessionWindowContext findContextById(Long id);

    @Update("""
        UPDATE session_windows
        SET title = #{title}
        WHERE id = #{windowId}
          AND deleted_at IS NULL
        """)
    int updateTitle(
        @Param("windowId") Long windowId,
        @Param("title") String title
    );

    @Update("""
        UPDATE session_windows
        SET deleted_at = CURRENT_TIMESTAMP
        WHERE id = #{windowId}
          AND deleted_at IS NULL
        """)
    int softDelete(Long windowId);

    @Select("""
        SELECT COUNT(*)
        FROM session_windows
        WHERE session_id = #{sessionId}
          AND deleted_at IS NULL
        """)
    int countActiveBySessionId(Long sessionId);

    @Select("""
        SELECT COUNT(*)
        FROM reading_sessions
        WHERE id = #{sessionId}
          AND user_id = #{userId}
          AND deleted_at IS NULL
        """)
    int countActiveSessionById(
        @Param("sessionId") Long sessionId,
        @Param("userId") Long userId
    );

    @Select("""
        SELECT COALESCE(MAX(position), 0) + 1
        FROM session_windows
        WHERE session_id = #{sessionId}
          AND deleted_at IS NULL
        """)
    int selectNextPosition(Long sessionId);

    @Select("""
        SELECT
          id,
          session_id,
          user_id,
          window_type,
          title,
          position,
          status,
          is_test_data
        FROM session_windows
        WHERE session_id = #{sessionId}
          AND deleted_at IS NULL
        ORDER BY position ASC, id ASC
        """)
    List<SessionWindowRecord> findBySessionId(Long sessionId);
}
