package com.margins.session.mapper;

import com.margins.session.model.SessionHighlightRecord;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SessionHighlightMapper {

    @Insert("""
        INSERT INTO session_highlights (
          session_id,
          book_id,
          user_id,
          page_number,
          location_label,
          quote_text,
          note,
          highlight_order,
          is_test_data
        )
        VALUES (
          #{sessionId},
          #{bookId},
          #{userId},
          #{pageNumber},
          #{locationLabel},
          #{quoteText},
          #{note},
          #{highlightOrder},
          #{testData}
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SessionHighlightRecord record);

    @Select("""
        SELECT COALESCE(MAX(highlight_order), 0) + 1
        FROM session_highlights
        WHERE session_id = #{sessionId}
          AND deleted_at IS NULL
        """)
    int selectNextOrder(Long sessionId);

    @Select("""
        SELECT
          id,
          session_id,
          book_id,
          user_id,
          page_number,
          location_label,
          quote_text,
          note,
          highlight_order,
          is_test_data
        FROM session_highlights
        WHERE session_id = #{sessionId}
          AND deleted_at IS NULL
        ORDER BY highlight_order ASC, id ASC
        """)
    List<SessionHighlightRecord> findBySessionId(Long sessionId);

    @Update("""
        UPDATE session_highlights
        SET
          page_number = #{pageNumber},
          location_label = #{locationLabel},
          quote_text = #{quoteText},
          note = #{note}
        WHERE id = #{highlightId}
          AND session_id = #{sessionId}
          AND user_id = #{userId}
          AND deleted_at IS NULL
        """)
    int update(Long sessionId, Long highlightId, Long userId, Integer pageNumber, String locationLabel, String quoteText, String note);

    @Update("""
        UPDATE session_highlights
        SET deleted_at = CURRENT_TIMESTAMP
        WHERE id = #{highlightId}
          AND session_id = #{sessionId}
          AND user_id = #{userId}
          AND deleted_at IS NULL
        """)
    int softDelete(Long sessionId, Long highlightId, Long userId);
}
