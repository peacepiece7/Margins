package com.margins.session.mapper;

import com.margins.session.model.ReadingSessionReviewRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ReadingSessionReviewMapper {

    @Select("""
        SELECT
          id,
          session_id,
          user_id,
          title,
          content_html,
          editor_type,
          status,
          is_test_data,
          created_at,
          updated_at
        FROM reading_session_reviews
        WHERE session_id = #{sessionId}
          AND user_id = #{userId}
          AND deleted_at IS NULL
        """)
    ReadingSessionReviewRecord findBySessionId(
        @Param("sessionId") Long sessionId,
        @Param("userId") Long userId
    );

    @Insert("""
        INSERT INTO reading_session_reviews (
          session_id,
          user_id,
          title,
          content_html,
          editor_type,
          status,
          is_test_data
        )
        VALUES (
          #{sessionId},
          #{userId},
          #{title},
          #{contentHtml},
          #{editorType},
          #{status},
          #{testData}
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ReadingSessionReviewRecord record);

    @Update("""
        UPDATE reading_session_reviews
        SET title = #{title},
            content_html = #{contentHtml},
            editor_type = #{editorType},
            status = #{status}
        WHERE id = #{id}
          AND session_id = #{sessionId}
          AND user_id = #{userId}
          AND deleted_at IS NULL
        """)
    int update(ReadingSessionReviewRecord record);
}
