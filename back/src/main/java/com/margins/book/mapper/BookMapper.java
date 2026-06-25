package com.margins.book.mapper;

import com.margins.book.model.BookRecord;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BookMapper {

    @Insert("""
        INSERT INTO books (
          user_id,
          title,
          author,
          isbn,
          published_year,
          source,
          source_ref,
          raw_metadata,
          is_test_data
        )
        VALUES (
          #{userId},
          #{title},
          #{author},
          #{isbn},
          #{publishedYear},
          #{source},
          #{sourceRef},
          #{rawMetadata},
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
          isbn,
          published_year,
          source,
          source_ref,
          raw_metadata AS rawMetadata,
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
          isbn,
          published_year,
          source,
          source_ref,
          raw_metadata AS rawMetadata,
          is_test_data
        FROM books
        WHERE user_id = #{userId}
          AND deleted_at IS NULL
        ORDER BY updated_at DESC, id DESC
        """)
    List<BookRecord> findByUserId(Long userId);

    @Select("""
        SELECT
          id,
          user_id,
          title,
          author,
          isbn,
          published_year,
          source,
          source_ref,
          raw_metadata AS rawMetadata,
          is_test_data
        FROM books
        WHERE id = #{bookId}
          AND user_id = #{userId}
          AND deleted_at IS NULL
        LIMIT 1
        """)
    BookRecord findByIdForUser(@Param("bookId") Long bookId, @Param("userId") Long userId);

    @Update("""
        UPDATE books
        SET
          title = #{title},
          author = #{author},
          published_year = #{publishedYear},
          raw_metadata = #{rawMetadata},
          updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id}
          AND user_id = #{userId}
          AND deleted_at IS NULL
        """)
    int update(BookRecord record);

    @Update("""
        UPDATE books
        SET
          deleted_at = CURRENT_TIMESTAMP,
          updated_at = CURRENT_TIMESTAMP
        WHERE id = #{bookId}
          AND user_id = #{userId}
          AND deleted_at IS NULL
        """)
    int softDelete(@Param("bookId") Long bookId, @Param("userId") Long userId);
}
