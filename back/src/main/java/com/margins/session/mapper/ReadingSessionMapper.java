package com.margins.session.mapper;

import com.margins.session.model.ReadingSessionRecord;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ReadingSessionMapper {

    @Select("""
        SELECT COUNT(*)
        FROM books
        WHERE id = #{bookId}
          AND user_id = #{userId}
          AND deleted_at IS NULL
        """)
    int countActiveBookById(
        @Param("bookId") Long bookId,
        @Param("userId") Long userId
    );

    @Insert("""
        INSERT INTO reading_sessions (
          user_id,
          book_id,
          title,
          status,
          is_test_data
        )
        VALUES (
          #{userId},
          #{bookId},
          #{title},
          #{status},
          #{testData}
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ReadingSessionRecord record);

    @Select("""
        SELECT
          rs.id,
          rs.user_id,
          rs.book_id,
          b.title AS book_title,
          b.author AS book_author,
          rs.title,
          rs.status,
          rs.is_pinned AS pinned,
          rs.reading_goal,
          rs.start_page,
          rs.current_page,
          rs.target_page,
          rs.progress_note,
          rs.summary,
          rs.is_test_data
        FROM reading_sessions rs
        INNER JOIN books b ON b.id = rs.book_id
        WHERE rs.user_id = #{userId}
          AND rs.deleted_at IS NULL
          AND b.deleted_at IS NULL
        ORDER BY rs.updated_at DESC, rs.id DESC
        LIMIT 1
        """)
    ReadingSessionRecord findLatestByUserId(Long userId);

    @Select("""
        SELECT
          rs.id,
          rs.user_id,
          rs.book_id,
          b.title AS book_title,
          b.author AS book_author,
          rs.title,
          rs.status,
          rs.is_pinned AS pinned,
          rs.reading_goal,
          rs.start_page,
          rs.current_page,
          rs.target_page,
          rs.progress_note,
          rs.summary,
          COUNT(DISTINCT sw.id) AS window_count,
          COUNT(DISTINCT q.id) AS question_count,
          COUNT(DISTINCT CASE WHEN qm.role = 'user' AND qm.question_id IS NOT NULL THEN qm.question_id END) AS answered_question_count,
          COUNT(DISTINCT h.id) AS highlight_count,
          COUNT(DISTINCT m.id) AS message_count,
          rs.is_test_data
        FROM reading_sessions rs
        INNER JOIN books b ON b.id = rs.book_id
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
        LEFT JOIN messages qm ON qm.session_id = rs.id
          AND qm.deleted_at IS NULL
          AND EXISTS (
            SELECT 1
            FROM session_windows qmw
            WHERE qmw.id = qm.window_id
              AND qmw.deleted_at IS NULL
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
        WHERE rs.user_id = #{userId}
          AND rs.deleted_at IS NULL
          AND b.deleted_at IS NULL
        GROUP BY rs.id, rs.user_id, rs.book_id, b.title, b.author, rs.title, rs.status, rs.is_pinned, rs.reading_goal, rs.start_page, rs.current_page, rs.target_page, rs.progress_note, rs.summary, rs.is_test_data, rs.updated_at
        ORDER BY rs.is_pinned DESC, rs.updated_at DESC, rs.id DESC
        """)
    List<ReadingSessionRecord> findSummariesByUserId(Long userId);

    @Select("""
        SELECT
          rs.id,
          rs.user_id,
          rs.book_id,
          b.title AS book_title,
          b.author AS book_author,
          rs.title,
          rs.status,
          rs.is_pinned AS pinned,
          rs.reading_goal,
          rs.start_page,
          rs.current_page,
          rs.target_page,
          rs.progress_note,
          rs.summary,
          rs.is_test_data
        FROM reading_sessions rs
        INNER JOIN books b ON b.id = rs.book_id
        WHERE rs.id = #{sessionId}
          AND rs.user_id = #{userId}
          AND rs.deleted_at IS NULL
          AND b.deleted_at IS NULL
        """)
    ReadingSessionRecord findByIdAndUserId(Long sessionId, Long userId);

    @Update("""
        UPDATE reading_sessions
        SET status = 'completed',
            summary = #{summary},
            completed_at = CURRENT_TIMESTAMP
        WHERE id = #{sessionId}
          AND user_id = #{userId}
          AND deleted_at IS NULL
        """)
    int complete(
        @Param("sessionId") Long sessionId,
        @Param("userId") Long userId,
        @Param("summary") String summary
    );

    @Update("""
        UPDATE reading_sessions
        SET deleted_at = CURRENT_TIMESTAMP
        WHERE id = #{sessionId}
          AND user_id = #{userId}
          AND deleted_at IS NULL
        """)
    int softDelete(
        @Param("sessionId") Long sessionId,
        @Param("userId") Long userId
    );

    @Update("""
        UPDATE reading_sessions
        SET title = #{title}
        WHERE id = #{sessionId}
          AND user_id = #{userId}
          AND deleted_at IS NULL
        """)
    int updateTitle(
        @Param("sessionId") Long sessionId,
        @Param("userId") Long userId,
        @Param("title") String title
    );

    @Update("""
        UPDATE reading_sessions
        SET reading_goal = #{readingGoal},
            start_page = #{startPage},
            current_page = #{currentPage},
            target_page = #{targetPage},
            progress_note = #{progressNote}
        WHERE id = #{sessionId}
          AND user_id = #{userId}
          AND deleted_at IS NULL
        """)
    int updateProgress(
        @Param("sessionId") Long sessionId,
        @Param("userId") Long userId,
        @Param("readingGoal") String readingGoal,
        @Param("startPage") Integer startPage,
        @Param("currentPage") Integer currentPage,
        @Param("targetPage") Integer targetPage,
        @Param("progressNote") String progressNote
    );

    @Update("""
        UPDATE reading_sessions
        SET is_pinned = #{pinned}
        WHERE id = #{sessionId}
          AND user_id = #{userId}
          AND deleted_at IS NULL
        """)
    int updatePinned(
        @Param("sessionId") Long sessionId,
        @Param("userId") Long userId,
        @Param("pinned") boolean pinned
    );
}
