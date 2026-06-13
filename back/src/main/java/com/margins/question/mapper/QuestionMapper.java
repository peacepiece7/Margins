package com.margins.question.mapper;

import com.margins.question.model.QuestionRecord;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface QuestionMapper {

    @Insert("""
        INSERT INTO questions (
          session_id,
          window_id,
          user_id,
          question_text,
          question_type,
          status,
          ai_model,
          is_test_data
        )
        VALUES (
          #{sessionId},
          #{windowId},
          #{userId},
          #{questionText},
          #{questionType},
          #{status},
          #{aiModel},
          #{testData}
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuestionRecord record);

    @Select("""
        SELECT
          q.id,
          q.session_id,
          q.window_id,
          q.user_id,
          q.question_text,
          q.question_type,
          q.status,
          q.ai_model,
          q.is_test_data
        FROM questions q
        LEFT JOIN session_windows sw ON sw.id = q.window_id
        WHERE q.session_id = #{sessionId}
          AND q.deleted_at IS NULL
          AND (q.window_id IS NULL OR sw.deleted_at IS NULL)
        ORDER BY q.id ASC
        """)
    List<QuestionRecord> findBySessionId(Long sessionId);

    @Select("""
        SELECT
          q.id,
          q.session_id,
          q.window_id,
          q.user_id,
          q.question_text,
          q.question_type,
          q.status,
          q.ai_model,
          q.is_test_data
        FROM questions q
        INNER JOIN session_windows sw ON sw.id = q.window_id
        WHERE q.window_id = #{windowId}
          AND q.deleted_at IS NULL
          AND sw.deleted_at IS NULL
        ORDER BY q.id ASC
        """)
    List<QuestionRecord> findByWindowId(Long windowId);

    @Select("""
        SELECT
          q.id,
          q.session_id,
          q.window_id,
          q.user_id,
          q.question_text,
          q.question_type,
          q.status,
          q.ai_model,
          q.is_test_data
        FROM questions q
        INNER JOIN session_windows sw ON sw.id = q.window_id
        WHERE q.id = #{questionId}
          AND q.user_id = #{userId}
          AND q.deleted_at IS NULL
          AND sw.deleted_at IS NULL
        """)
    QuestionRecord findActiveById(
        @Param("questionId") Long questionId,
        @Param("userId") Long userId
    );

    @Select("""
        SELECT COUNT(*)
        FROM messages
        WHERE question_id = #{questionId}
          AND role = 'user'
          AND deleted_at IS NULL
        """)
    int countActiveUserAnswers(Long questionId);

    @Update("""
        UPDATE questions
        SET deleted_at = CURRENT_TIMESTAMP
        WHERE id = #{questionId}
          AND user_id = #{userId}
          AND deleted_at IS NULL
        """)
    int softDelete(
        @Param("questionId") Long questionId,
        @Param("userId") Long userId
    );
}
