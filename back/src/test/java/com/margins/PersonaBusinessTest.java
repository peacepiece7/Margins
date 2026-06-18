package com.margins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.margins.ai.AiProvider;
import com.margins.ai.AiSafetyPolicy;
import com.margins.book.dto.BookCandidateSearchResponse;
import com.margins.persona.business.PersonaBusiness;
import com.margins.persona.dto.CreatePersonaRequest;
import com.margins.persona.dto.GeneratePersonasRequest;
import com.margins.persona.dto.PersonaDraftDto;
import com.margins.persona.dto.PersonaDraftListResponse;
import com.margins.persona.dto.PersonaListResponse;
import com.margins.persona.mapper.PersonaMapper;
import com.margins.persona.model.PersonaRecord;
import com.margins.question.dto.GenerateQuestionsRequest;
import com.margins.question.dto.QuestionListResponse;
import com.margins.session.dto.AiMessageResponse;
import com.margins.session.dto.DebateMessageRequest;
import com.margins.session.dto.SendMessageRequest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PersonaBusinessTest {
    private final AiSafetyPolicy aiSafetyPolicy = new AiSafetyPolicy();

    @Test
    void findActiveReturnsPersonaDtosInMapperOrder() {
        PersonaBusiness business = new PersonaBusiness(new FakeAiProvider(), new FakePersonaMapper(), aiSafetyPolicy);

        PersonaListResponse response = business.findActive();

        assertThat(response.getPersonas()).hasSize(2);
        assertThat(response.getPersonas().get(0).getPersonaId()).isEqualTo(1L);
        assertThat(response.getPersonas().get(0).getDisplayName()).isEqualTo("Careful Critic");
        assertThat(response.getPersonas().get(1).getTone()).isEqualTo("warm");
    }

    @Test
    void createPersistsReaderPersonaAndReturnsActiveList() {
        FakePersonaMapper mapper = new FakePersonaMapper();
        PersonaBusiness business = new PersonaBusiness(new FakeAiProvider(), mapper, aiSafetyPolicy);

        PersonaListResponse response = business.create(CreatePersonaRequest.builder()
            .displayName("Skeptical Historian")
            .description("Checks claims against historical context.")
            .systemPrompt("Respond as a skeptical historian.")
            .tone("skeptical")
            .roleKey("skeptic")
            .sessionId(7L)
            .build());

        assertThat(mapper.inserted.getName()).startsWith("reader-skeptical-historian-");
        assertThat(mapper.inserted.getSystemPrompt()).isEqualTo("Respond as a skeptical historian.");
        assertThat(mapper.inserted.getRoleKey()).isEqualTo("skeptic");
        assertThat(mapper.inserted.getSourceSessionId()).isEqualTo(7L);
        assertThat(mapper.inserted.isActive()).isTrue();
        assertThat(response.getPersonas()).extracting("displayName").contains("Skeptical Historian");
    }

    @Test
    void createRejectsDuplicateSessionRole() {
        FakePersonaMapper mapper = new FakePersonaMapper();
        mapper.customPersonas.add(PersonaRecord.builder()
            .id(20L)
            .displayName("Existing Skeptic")
            .roleKey("skeptic")
            .sourceSessionId(7L)
            .active(true)
            .build());
        PersonaBusiness business = new PersonaBusiness(new FakeAiProvider(), mapper, aiSafetyPolicy);

        assertThatThrownBy(() -> business.create(CreatePersonaRequest.builder()
            .displayName("Another Skeptic")
            .systemPrompt("Challenge claims.")
            .roleKey("skeptic")
            .sessionId(7L)
            .build()))
            .isInstanceOfSatisfying(ResponseStatusException.class, (exception) -> {
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                assertThat(exception.getReason()).isEqualTo("Persona role already exists for this session");
            });
    }

    @Test
    void createRejectsZeroRowInsert() {
        FakePersonaMapper mapper = new FakePersonaMapper();
        mapper.insertRows = 0;
        PersonaBusiness business = new PersonaBusiness(new FakeAiProvider(), mapper, aiSafetyPolicy);

        assertThatThrownBy(() -> business.create(CreatePersonaRequest.builder()
            .displayName("Silent Reviewer")
            .systemPrompt("Review silently.")
            .build()))
            .isInstanceOfSatisfying(ResponseStatusException.class, (exception) -> {
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                assertThat(exception.getReason()).isEqualTo("Persona could not be saved");
            });
    }

    @Test
    void generateReturnsAiPersonaDraftsWithoutPersisting() {
        FakeAiProvider aiProvider = new FakeAiProvider();
        FakePersonaMapper mapper = new FakePersonaMapper();
        PersonaBusiness business = new PersonaBusiness(aiProvider, mapper, aiSafetyPolicy);

        PersonaDraftListResponse response = business.generate(GeneratePersonasRequest.builder()
            .bookTitle("Dune")
            .readingGoal("power and ecology")
            .build());

        assertThat(response.getPersonas()).singleElement()
            .extracting(PersonaDraftDto::getDisplayName)
            .isEqualTo("Systems Critic");
        assertThat(response.getPersonas()).singleElement()
            .extracting(PersonaDraftDto::getRoleKey)
            .isEqualTo("evidence_analyst");
        assertThat(mapper.inserted).isNull();
    }

    @Test
    void generateReplacesUnsafePersonaDraftsWithRoleFallback() {
        FakeAiProvider aiProvider = new FakeAiProvider();
        aiProvider.drafts = List.of(PersonaDraftDto.builder()
            .displayName("Graphic Violence Coach")
            .description("Uses graphic violence to humiliate the reader.")
            .tone("hostile")
            .roleKey("skeptic")
            .systemPrompt("Use graphic violence and harass the reader.")
            .reason("unsafe")
            .build());
        PersonaBusiness business = new PersonaBusiness(aiProvider, new FakePersonaMapper(), aiSafetyPolicy);

        PersonaDraftListResponse response = business.generate(GeneratePersonasRequest.builder()
            .bookTitle("Dune")
            .build());

        assertThat(response.getPersonas()).singleElement().satisfies((draft) -> {
            assertThat(draft.getRoleKey()).isEqualTo("skeptic");
            assertThat(draft.getDisplayName()).isEqualTo("Skeptical Reader");
            assertThat(draft.getSystemPrompt()).contains("skeptical but respectful reader of Dune");
            assertThat(draft.getDescription()).doesNotContain("graphic violence");
        });
    }

    private static class FakeAiProvider implements AiProvider {
        private List<PersonaDraftDto> drafts = List.of(PersonaDraftDto.builder()
            .displayName("Systems Critic")
            .roleKey("unknown external label")
            .systemPrompt("Challenge systems-level assumptions.")
            .build());

        @Override
        public BookCandidateSearchResponse suggestBooks(String query) {
            return BookCandidateSearchResponse.builder().build();
        }

        @Override
        public PersonaDraftListResponse suggestPersonas(GeneratePersonasRequest request) {
            return PersonaDraftListResponse.builder()
                .aiModel("fake")
                .personas(drafts)
                .build();
        }

        @Override
        public QuestionListResponse suggestQuestions(Long windowId, GenerateQuestionsRequest request) {
            return QuestionListResponse.builder().build();
        }

        @Override
        public AiMessageResponse answerWindowMessage(Long windowId, SendMessageRequest request) {
            return null;
        }

        @Override
        public AiMessageResponse answerDebateMessage(Long windowId, DebateMessageRequest request) {
            return null;
        }
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
                    .name("careful-critic")
                    .displayName("Careful Critic")
                    .description("Challenges vague reflections.")
                    .tone("critical")
                    .roleKey("skeptic")
                    .active(true)
                    .build(),
                PersonaRecord.builder()
                    .id(2L)
                    .name("empathetic-reader")
                    .displayName("Empathetic Reader")
                    .description("Explores emotional response.")
                    .tone("warm")
                    .roleKey("empathy_reader")
                    .active(true)
                    .build()
            );
            List<PersonaRecord> personas = new ArrayList<>(seedPersonas);
            personas.addAll(customPersonas);
            return personas;
        }

        @Override
        public List<PersonaRecord> findActiveForSession(Long sessionId) {
            return findActive().stream()
                .filter((persona) -> persona.getSourceSessionId() == null || persona.getSourceSessionId().equals(sessionId))
                .toList();
        }

        @Override
        public PersonaRecord findActiveById(Long id) {
            return findActive().stream()
                .filter((persona) -> persona.getId().equals(id))
                .findFirst()
                .orElse(null);
        }

        @Override
        public int countActiveBySessionAndRoleKey(Long sessionId, String roleKey) {
            return (int) findActive().stream()
                .filter((persona) -> persona.getSourceSessionId() != null && persona.getSourceSessionId().equals(sessionId))
                .filter((persona) -> roleKey.equals(persona.getRoleKey()))
                .count();
        }
    }
}
