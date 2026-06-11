package com.margins.message.mapper;

import com.margins.message.model.MessageRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MessageMapper {

    @Insert("""
        INSERT INTO messages (
          session_id,
          window_id,
          user_id,
          parent_message_id,
          role,
          content,
          message_order,
          ai_model,
          persona_id,
          question_id,
          streaming_status,
          is_test_data
        )
        VALUES (
          #{sessionId},
          #{windowId},
          #{userId},
          #{parentMessageId},
          #{role},
          #{content},
          #{messageOrder},
          #{aiModel},
          #{personaId},
          #{questionId},
          #{streamingStatus},
          #{testData}
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MessageRecord record);

    @Select("""
        SELECT COALESCE(MAX(message_order), 0) + 1
        FROM messages
        WHERE session_id = #{sessionId}
          AND deleted_at IS NULL
        """)
    int selectNextOrder(Long sessionId);
}
