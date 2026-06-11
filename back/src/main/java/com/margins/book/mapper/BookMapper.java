package com.margins.book.mapper;

import com.margins.book.model.BookRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface BookMapper {

    @Insert("""
        INSERT INTO books (
          user_id,
          title,
          author,
          published_year,
          source,
          source_ref,
          is_test_data
        )
        VALUES (
          #{userId},
          #{title},
          #{author},
          #{publishedYear},
          #{source},
          #{sourceRef},
          #{testData}
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BookRecord record);
}
