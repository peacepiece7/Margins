package com.margins.session.mapper;

import com.margins.session.model.ReadingSessionRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface ReadingSessionMapper {

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
}
