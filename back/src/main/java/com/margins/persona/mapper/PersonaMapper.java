package com.margins.persona.mapper;

import com.margins.persona.model.PersonaRecord;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PersonaMapper {

    @Insert("""
        INSERT INTO personas (
          name,
          display_name,
          description,
          system_prompt,
          tone,
          role_key,
          source_session_id,
          is_active,
          is_test_data
        )
        VALUES (
          #{name},
          #{displayName},
          #{description},
          #{systemPrompt},
          #{tone},
          #{roleKey},
          #{sourceSessionId},
          #{active},
          TRUE
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PersonaRecord record);

    @Select("""
        SELECT
          id,
          name,
          display_name,
          description,
          system_prompt,
          tone,
          role_key,
          source_session_id,
          is_active
        FROM personas
        WHERE is_active = TRUE
          AND deleted_at IS NULL
        ORDER BY id ASC
        """)
    List<PersonaRecord> findActive();

    @Select("""
        SELECT
          id,
          name,
          display_name,
          description,
          system_prompt,
          tone,
          role_key,
          source_session_id,
          is_active
        FROM personas
        WHERE is_active = TRUE
          AND deleted_at IS NULL
          AND (
            source_session_id = #{sessionId}
            OR (
              source_session_id IS NULL
              AND NOT EXISTS (
                SELECT 1
                FROM personas session_personas
                WHERE session_personas.source_session_id = #{sessionId}
                  AND session_personas.is_active = TRUE
                  AND session_personas.deleted_at IS NULL
              )
            )
          )
        ORDER BY id ASC
        """)
    List<PersonaRecord> findActiveForSession(Long sessionId);

    @Select("""
        SELECT
          id,
          name,
          display_name,
          description,
          system_prompt,
          tone,
          role_key,
          source_session_id,
          is_active
        FROM personas
        WHERE id = #{id}
          AND is_active = TRUE
          AND deleted_at IS NULL
        """)
    PersonaRecord findActiveById(Long id);

    @Select("""
        SELECT COUNT(*)
        FROM personas
        WHERE source_session_id = #{sessionId}
          AND role_key = #{roleKey}
          AND is_active = TRUE
          AND deleted_at IS NULL
        """)
    int countActiveBySessionAndRoleKey(@Param("sessionId") Long sessionId, @Param("roleKey") String roleKey);
}
