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
          subtitle,
          author,
          publisher,
          isbn,
          published_year,
          language_code,
          description,
          source,
          source_ref,
          cover_image_url,
          raw_metadata,
          is_test_data
        )
        VALUES (
          #{userId},
          #{title},
          #{subtitle},
          #{author},
          #{publisher},
          #{isbn},
          #{publishedYear},
          #{languageCode},
          #{description},
          #{source},
          #{sourceRef},
          #{coverImageUrl},
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
          subtitle,
          author,
          publisher,
          isbn,
          published_year,
          language_code AS languageCode,
          description,
          source,
          source_ref,
          cover_image_url AS coverImageUrl,
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
          subtitle,
          author,
          publisher,
          isbn,
          published_year,
          language_code AS languageCode,
          description,
          source,
          source_ref,
          cover_image_url AS coverImageUrl,
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
          subtitle,
          author,
          publisher,
          isbn,
          published_year,
          language_code AS languageCode,
          description,
          source,
          source_ref,
          cover_image_url AS coverImageUrl,
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
          subtitle = CASE WHEN (subtitle IS NULL OR TRIM(subtitle) = '') THEN #{subtitle} ELSE subtitle END,
          publisher = CASE WHEN (publisher IS NULL OR TRIM(publisher) = '') THEN #{publisher} ELSE publisher END,
          isbn = CASE WHEN (isbn IS NULL OR TRIM(isbn) = '') THEN #{isbn} ELSE isbn END,
          published_year = COALESCE(published_year, #{publishedYear}),
          language_code = CASE WHEN (language_code IS NULL OR TRIM(language_code) = '') THEN #{languageCode} ELSE language_code END,
          description = CASE WHEN (description IS NULL OR TRIM(description) = '') THEN #{description} ELSE description END,
          source = CASE
            WHEN (source IS NULL OR TRIM(source) = '' OR source = 'ai') AND #{source} <> 'ai' THEN #{source}
            ELSE source
          END,
          source_ref = CASE
            WHEN #{source} <> 'ai'
              AND (source IS NULL OR TRIM(source) = '' OR source = 'ai' OR source_ref IS NULL OR TRIM(source_ref) = '' OR source_ref LIKE 'manual-%')
              THEN #{sourceRef}
            WHEN (source_ref IS NULL OR TRIM(source_ref) = '') THEN #{sourceRef}
            ELSE source_ref
          END,
          cover_image_url = CASE WHEN (cover_image_url IS NULL OR TRIM(cover_image_url) = '') THEN #{coverImageUrl} ELSE cover_image_url END,
          raw_metadata = CASE
            WHEN #{source} <> 'ai'
              AND (source IS NULL OR TRIM(source) = '' OR source = 'ai' OR source_ref IS NULL OR TRIM(source_ref) = '' OR source_ref LIKE 'manual-%')
              THEN #{rawMetadata}
            WHEN raw_metadata IS NULL THEN #{rawMetadata}
            ELSE raw_metadata
          END,
          updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id}
          AND user_id = #{userId}
          AND deleted_at IS NULL
        """)
    int fillMissingProviderMetadata(BookRecord record);

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
