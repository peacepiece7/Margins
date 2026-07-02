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

        assertThat(response.getPersonas()).hasSize(8);
        assertThat(response.getPersonas().get(0).getPersonaId()).isEqualTo(5L);
        assertThat(response.getPersonas().get(0).getDisplayName()).isEqualTo("문학평론가");
        assertThat(response.getPersonas().get(1).getTone()).isEqualTo("윤리와 의미");
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
                    .id(5L)
                    .name("literary-critic")
                    .displayName("문학평론가")
                    .description("personaType: professional; primaryLens: structure, symbol, narrator, style.")
                    .tone("구조와 상징")
                    .active(true)
                    .build(),
                PersonaRecord.builder()
                    .id(6L)
                    .name("philosopher")
                    .displayName("철학자")
                    .description("personaType: professional; primaryLens: ethics, freedom, responsibility, meaning.")
                    .tone("윤리와 의미")
                    .active(true)
                    .build(),
                PersonaRecord.builder()
                    .id(7L)
                    .name("psychologist")
                    .displayName("심리학자")
                    .description("personaType: professional; primaryLens: motivation, defense, relationship.")
                    .tone("동기와 관계")
                    .active(true)
                    .build(),
                PersonaRecord.builder()
                    .id(8L)
                    .name("historian")
                    .displayName("역사학자")
                    .description("personaType: professional; primaryLens: period, institution, power.")
                    .tone("시대와 권력")
                    .active(true)
                    .build(),
                PersonaRecord.builder()
                    .id(9L)
                    .name("sociologist")
                    .displayName("사회학자")
                    .description("personaType: professional; primaryLens: group, norm, class, gender, culture.")
                    .tone("규범과 집단")
                    .active(true)
                    .build(),
                PersonaRecord.builder()
                    .id(10L)
                    .name("editor")
                    .displayName("편집자")
                    .description("personaType: professional; primaryLens: scene, plot, pacing, reader experience.")
                    .tone("장면과 독자 경험")
                    .active(true)
                    .build(),
                PersonaRecord.builder()
                    .id(11L)
                    .name("skeptical-reader")
                    .displayName("회의적인 독자")
                    .description("personaType: professional; primaryLens: evidence check, counterargument.")
                    .tone("근거 점검")
                    .active(true)
                    .build(),
                PersonaRecord.builder()
                    .id(12L)
                    .name("book-club-facilitator")
                    .displayName("독서 모임 진행자")
                    .description("personaType: professional; primaryLens: dialogue balance, synthesis, next question.")
                    .tone("대화 진행")
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
