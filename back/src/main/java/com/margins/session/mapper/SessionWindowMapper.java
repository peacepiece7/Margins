package com.margins.session.mapper;

import com.margins.session.model.SessionWindowContext;
import com.margins.session.model.SessionWindowRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

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
          user_id
        FROM session_windows
        WHERE id = #{id}
          AND deleted_at IS NULL
        """)
    SessionWindowContext findContextById(Long id);

    @Select("""
        SELECT COALESCE(MAX(position), 0) + 1
        FROM session_windows
        WHERE session_id = #{sessionId}
          AND deleted_at IS NULL
        """)
    int selectNextPosition(Long sessionId);
}
