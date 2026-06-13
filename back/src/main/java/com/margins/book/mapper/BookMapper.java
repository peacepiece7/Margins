package com.margins.book.mapper;

import com.margins.book.model.BookRecord;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

    @Select("""
        SELECT
          id,
          user_id,
          title,
          author,
          published_year,
          source,
          source_ref,
          is_test_data
        FROM books
        WHERE user_id = #{userId}
          AND deleted_at IS NULL
          AND LOWER(TRIM(title)) = LOWER(TRIM(#{title}))
          AND (
            (
              (#{author} IS NULL OR TRIM(#{author}) = '')
              AND (author IS NULL OR TRIM(author) = '')
            )
            OR LOWER(TRIM(author)) = LOWER(TRIM(#{author}))
          )
        ORDER BY updated_at DESC, id DESC
        LIMIT 1
        """)
    BookRecord findDuplicate(
        @Param("userId") Long userId,
        @Param("title") String title,
        @Param("author") String author
    );

    @Select("""
        SELECT
          id,
          user_id,
          title,
          author,
          published_year,
          source,
          source_ref,
          is_test_data
        FROM books
        WHERE user_id = #{userId}
          AND deleted_at IS NULL
        ORDER BY updated_at DESC, id DESC
        """)
    List<BookRecord> findByUserId(Long userId);
}
