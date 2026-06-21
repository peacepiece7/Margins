package com.margins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.margins.persona.business.PersonaBusiness;
import com.margins.persona.dto.CreatePersonaRequest;
import com.margins.persona.dto.PersonaListResponse;
import com.margins.persona.mapper.PersonaMapper;
import com.margins.persona.model.PersonaRecord;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PersonaBusinessTest {

    @Test
    void findActiveReturnsPersonaDtosInMapperOrder() {
        PersonaBusiness business = new PersonaBusiness(new FakePersonaMapper());

        PersonaListResponse response = business.findActive();

        assertThat(response.getPersonas()).hasSize(4);
        assertThat(response.getPersonas().get(0).getPersonaId()).isEqualTo(1L);
        assertThat(response.getPersonas().get(0).getDisplayName()).isEqualTo("전사 아르단");
        assertThat(response.getPersonas().get(1).getTone()).isEqualTo("상징적인 마법사형");
    }

    @Test
    void createPersistsReaderPersonaAndReturnsActiveList() {
        FakePersonaMapper mapper = new FakePersonaMapper();
        PersonaBusiness business = new PersonaBusiness(mapper);

        PersonaListResponse response = business.create(CreatePersonaRequest.builder()
            .displayName("Skeptical Historian")
            .description("Checks claims against historical context.")
            .systemPrompt("Respond as a skeptical historian.")
            .tone("skeptical")
            .build());

        assertThat(mapper.inserted.getName()).startsWith("reader-skeptical-historian-");
        assertThat(mapper.inserted.getSystemPrompt()).isEqualTo("Respond as a skeptical historian.");
        assertThat(mapper.inserted.isActive()).isTrue();
        assertThat(response.getPersonas()).extracting("displayName").contains("Skeptical Historian");
    }

    @Test
    void createRejectsZeroRowInsert() {
        FakePersonaMapper mapper = new FakePersonaMapper();
        mapper.insertRows = 0;
        PersonaBusiness business = new PersonaBusiness(mapper);

        assertThatThrownBy(() -> business.create(CreatePersonaRequest.builder()
            .displayName("Silent Reviewer")
            .systemPrompt("Review silently.")
            .build()))
            .isInstanceOfSatisfying(ResponseStatusException.class, (exception) -> {
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                assertThat(exception.getReason()).isEqualTo("Persona could not be saved");
            });
    }

    private static class FakePersonaMapper implements PersonaMapper {
        private PersonaRecord inserted;
        private final List<PersonaRecord> customPersonas = new ArrayList<>();
        private int insertRows = 1;

        @Override
        public int insert(PersonaRecord record) {
            record.setId(100L + customPersonas.size());
            inserted = record;
            customPersonas.add(record);
            return insertRows;
        }

        @Override
        public List<PersonaRecord> findActive() {
            List<PersonaRecord> seedPersonas = List.of(
                PersonaRecord.builder()
                    .id(1L)
                    .name("warrior-ardan")
                    .displayName("전사 아르단")
                    .description("이름: 아르단. 나이: 42세. 직업: 전사.")
                    .tone("단호한 전사형")
                    .active(true)
                    .build(),
                PersonaRecord.builder()
                    .id(2L)
                    .name("wizard-lyra")
                    .displayName("마법사 리라")
                    .description("이름: 리라. 나이: 137세. 직업: 마법사.")
                    .tone("상징적인 마법사형")
                    .active(true)
                    .build(),
                PersonaRecord.builder()
                    .id(3L)
                    .name("cleric-seren")
                    .displayName("성직자 세렌")
                    .description("이름: 세렌. 나이: 35세. 직업: 성직자.")
                    .tone("차분한 성직자형")
                    .active(true)
                    .build(),
                PersonaRecord.builder()
                    .id(4L)
                    .name("rogue-nox")
                    .displayName("도적 녹스")
                    .description("이름: 녹스. 나이: 29세. 직업: 도적.")
                    .tone("날카로운 도적형")
                    .active(true)
                    .build()
            );
            List<PersonaRecord> personas = new ArrayList<>(seedPersonas);
            personas.addAll(customPersonas);
            return personas;
        }

        @Override
        public PersonaRecord findActiveById(Long id) {
            return findActive().stream()
                .filter((persona) -> persona.getId().equals(id))
                .findFirst()
                .orElse(null);
        }
    }
}
