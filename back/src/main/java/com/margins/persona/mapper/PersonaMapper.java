package com.margins.persona.mapper;

import com.margins.persona.model.PersonaRecord;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
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
          is_active,
          is_test_data
        )
        VALUES (
          #{name},
          #{displayName},
          #{description},
          #{systemPrompt},
          #{tone},
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
          is_active
        FROM personas
        WHERE id = #{id}
          AND is_active = TRUE
          AND deleted_at IS NULL
        """)
    PersonaRecord findActiveById(Long id);
}
