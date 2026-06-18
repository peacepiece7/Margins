package com.margins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.margins.ai.AiProvider;
import com.margins.book.dto.BookCandidateSearchResponse;
import com.margins.message.mapper.MessageMapper;
import com.margins.message.model.MessageRecord;
import com.margins.persona.dto.GeneratePersonasRequest;
import com.margins.persona.dto.PersonaDraftListResponse;
import com.margins.persona.mapper.PersonaMapper;
import com.margins.persona.model.PersonaRecord;
import com.margins.question.dto.CreateQuestionRequest;
import com.margins.question.dto.GenerateQuestionsRequest;
import com.margins.question.dto.QuestionDto;
import com.margins.question.dto.QuestionListResponse;
import com.margins.question.mapper.QuestionMapper;
import com.margins.question.model.QuestionRecord;
import com.margins.session.business.SessionWindowBusiness;
import com.margins.session.dto.AiMessageResponse;
import com.margins.session.dto.CreateSessionWindowRequest;
import com.margins.session.dto.CreateSessionWindowResponse;
import com.margins.session.dto.DebateAllMessageRequest;
import com.margins.session.dto.DebateMessageRequest;
import com.margins.session.dto.SendMessageRequest;
import com.margins.session.dto.UpdateSessionWindowTitleRequest;
import com.margins.session.mapper.SessionHighlightMapper;
import com.margins.session.mapper.SessionWindowMapper;
import com.margins.session.model.SessionHighlightRecord;
import com.margins.session.model.SessionWindowContext;
import com.margins.session.model.SessionWindowRecord;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class SessionWindowBusinessPersistenceTest {

    @Test
    void createPersistsAndReturnsGeneratedId() {
        FakeSessionWindowMapper windowMapper = new FakeSessionWindowMapper();
        SessionWindowBusiness business = new SessionWindowBusiness(
            new StubAiProvider(),
            windowMapper,
            new FakeMessageMapper(),
            new FakeQuestionMapper(),
            new FakePersonaMapper()
        );

        CreateSessionWindowResponse response = business.create(CreateSessionWindowRequest.builder()
            .sessionId(3L)
            .windowType("question")
            .title("Question")
            .build());

        assertThat(response.getWindowId()).isEqualTo(300L);
        assertThat(response.getSessionId()).isEqualTo(3L);
        assertThat(response.getStatus()).isEqualTo("open");
        assertThat(windowMapper.inserted.getPosition()).isEqualTo(5);
    }

    @Test
    void createRejectsMissingReadingSessionBeforeInsert() {
        FakeSessionWindowMapper windowMapper = new FakeSessionWindowMapper();
        windowMapper.activeSessionCount = 0;
        SessionWindowBusiness business = new SessionWindowBusiness(
            new StubAiProvider(),
            windowMapper,
            new FakeMessageMapper(),
            new FakeQuestionMapper(),
            new FakePersonaMapper()
        );

        assertNotFound("Reading session not found", () -> business.create(CreateSessionWindowRequest.builder()
            .sessionId(404L)
            .windowType("question")
            .title("Missing parent")
            .build()));
        assertThat(windowMapper.inserted).isNull();
    }

    @Test
    void createRejectsZeroRowInsert() {
        FakeSessionWindowMapper windowMapper = new FakeSessionWindowMapper();
        windowMapper.insertRows = 0;
        SessionWindowBusiness business = new SessionWindowBusiness(
            new StubAiProvider(),
            windowMapper,
            new FakeMessageMapper(),
            new FakeQuestionMapper(),
            new FakePersonaMapper()
        );

        assertServerError("Session window could not be saved", () -> business.create(CreateSessionWindowRequest.builder()
            .sessionId(3L)
            .windowType("question")
            .title("Question")
            .build()));
    }

    @Test
    void updateTitleStoresWindowTitleAndReturnsWindow() {
        FakeSessionWindowMapper windowMapper = new FakeSessionWindowMapper();
        SessionWindowBusiness business = new SessionWindowBusiness(
            new StubAiProvider(),
            windowMapper,
            new FakeMessageMapper(),
            new FakeQuestionMapper(),
            new FakePersonaMapper()
        );

        CreateSessionWindowResponse response = business.updateTitle(10L, UpdateSessionWindowTitleRequest.builder()
            .title("Ecology thread")
            .build());

        assertThat(windowMapper.updatedWindowId).isEqualTo(10L);
        assertThat(windowMapper.updatedTitle).isEqualTo("Ecology thread");
        assertThat(response.getWindowId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Ecology thread");
    }

    @Test
    void archiveSoftDeletesWindowAndReturnsArchivedWindow() {
        FakeSessionWindowMapper windowMapper = new FakeSessionWindowMapper();
        windowMapper.activeWindowCount = 2;
        SessionWindowBusiness business = new SessionWindowBusiness(
            new StubAiProvider(),
            windowMapper,
            new FakeMessageMapper(),
            new FakeQuestionMapper(),
            new FakePersonaMapper()
        );

        CreateSessionWindowResponse response = business.archive(10L);

        assertThat(windowMapper.deletedWindowId).isEqualTo(10L);
        assertThat(response.getWindowId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo("archived");
    }

    @Test
    void archiveRejectsLastActiveWindow() {
        FakeSessionWindowMapper windowMapper = new FakeSessionWindowMapper();
        windowMapper.activeWindowCount = 1;
        SessionWindowBusiness business = new SessionWindowBusiness(
            new StubAiProvider(),
            windowMapper,
            new FakeMessageMapper(),
            new FakeQuestionMapper(),
            new FakePersonaMapper()
        );

        assertThatThrownBy(() -> business.archive(10L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies((exception) -> {
                ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                assertThat(responseStatusException.getReason()).isEqualTo("At least one session window must remain");
            });

        assertThat(windowMapper.deletedWindowId).isNull();
    }

    @Test
    void windowMutationsReturnNotFoundWhenRowsAreMissing() {
        FakeSessionWindowMapper windowMapper = new FakeSessionWindowMapper();
        windowMapper.updatedRows = 0;
        windowMapper.deletedRows = 0;
        SessionWindowBusiness business = new SessionWindowBusiness(
            new StubAiProvider(),
            windowMapper,
            new FakeMessageMapper(),
            new FakeQuestionMapper(),
            new FakePersonaMapper()
        );

        assertNotFound("Session window not found", () -> business.updateTitle(10L, UpdateSessionWindowTitleRequest.builder()
            .title("Missing")
            .build()));
        assertNotFound("Session window not found", () -> business.archive(10L));
    }


    @Test
    void sendMessagePersistsUserAndAssistantMessages() {
        FakeMessageMapper messageMapper = new FakeMessageMapper();
        FakeQuestionMapper questionMapper = new FakeQuestionMapper();
        questionMapper.inserted.add(QuestionRecord.builder()
            .id(9L)
            .sessionId(30L)
            .windowId(10L)
            .userId(1L)
            .questionText("What matters?")
            .questionType("reflection")
            .status("active")
            .build());
        SessionWindowBusiness business = new SessionWindowBusiness(
            new StubAiProvider(),
            new FakeSessionWindowMapper(),
            messageMapper,
            questionMapper,
            new FakePersonaMapper()
        );

        AiMessageResponse response = business.sendMessage(10L, SendMessageRequest.builder()
            .content("What matters?")
            .questionId(9L)
            .build());

        assertThat(response.getMessageId()).isEqualTo(101L);
        assertThat(messageMapper.inserted).hasSize(2);
        assertThat(messageMapper.inserted.get(0).getRole()).isEqualTo("user");
        assertThat(messageMapper.inserted.get(0).getMessageOrder()).isEqualTo(1);
        assertThat(messageMapper.inserted.get(1).getRole()).isEqualTo("assistant");
        assertThat(messageMapper.inserted.get(1).getParentMessageId()).isEqualTo(100L);
        assertThat(messageMapper.inserted.get(1).getQuestionId()).isEqualTo(9L);
        assertThat(messageMapper.inserted.get(1).getContextSnapshot()).contains("\"question\"", "What matters?");
        assertThat(messageMapper.inserted.get(1).getPromptSnapshot())
            .contains("\"schemaVersion\":1")
            .contains("\"promptContractVersion\":\"mvp-answer-v2\"")
            .contains("\"responseType\":\"book_answer\"")
            .contains("\"provider\":\"placeholder\"")
            .contains("\"groundingPolicyVersion\":\"mvp-grounding-v1\"");
        assertThat(response.getPromptSnapshot()).isEqualTo(messageMapper.inserted.get(1).getPromptSnapshot());
        assertThat(response.getContextSnapshot()).isEqualTo(messageMapper.inserted.get(1).getContextSnapshot());
        assertThat(messageMapper.inserted.get(1).getTokenUsage()).contains("\"total_tokens\":7");
        assertThat(response.getTokenUsage()).isEqualTo(messageMapper.inserted.get(1).getTokenUsage());
        assertThat(messageMapper.orderWindowIds).containsExactly(10L, 10L);
    }

    @Test
    void sendMessageIncludesSavedHighlightsInContextSnapshot() {
        FakeMessageMapper messageMapper = new FakeMessageMapper();
        SessionWindowBusiness business = new SessionWindowBusiness(
            new StubAiProvider(),
            new FakeSessionWindowMapper(),
            messageMapper,
            new FakeQuestionMapper(),
            new FakePersonaMapper(),
            new FakeSessionHighlightMapper()
        );

        AiMessageResponse response = business.sendMessage(10L, SendMessageRequest.builder()
            .content("Use my quote")
            .build());

        assertThat(messageMapper.inserted.get(1).getContextSnapshot()).contains("\"highlights\"", "Quote p. 42", "fear is the mind-killer");
        assertThat(response.getContextSnapshot()).contains("fear is the mind-killer");
    }

    @Test
    void sendMessageIgnoresClientSuppliedUserId() {
        FakeMessageMapper messageMapper = new FakeMessageMapper();
        SessionWindowBusiness business = new SessionWindowBusiness(
            new StubAiProvider(),
            new FakeSessionWindowMapper(),
            messageMapper,
            new FakeQuestionMapper(),
            new FakePersonaMapper()
        );

        business.sendMessage(10L, SendMessageRequest.builder()
            .userId(999L)
            .content("Do not trust client user id")
            .build());

        assertThat(messageMapper.inserted).extracting(MessageRecord::getUserId).containsOnly(1L);
    }

    @Test
    void sendMessageRejectsZeroRowUserMessageInsertBeforeAiCall() {
        FakeMessageMapper messageMapper = new FakeMessageMapper();
        messageMapper.insertRows = 0;
        StubAiProvider aiProvider = new StubAiProvider();
        SessionWindowBusiness business = new SessionWindowBusiness(
            aiProvider,
            new FakeSessionWindowMapper(),
            messageMapper,
            new FakeQuestionMapper(),
            new FakePersonaMapper()
        );

        assertServerError("Message could not be saved", () -> business.sendMessage(10L, SendMessageRequest.builder()
            .content("What matters?")
            .build()));
        assertThat(aiProvider.windowAnswerCalls).isZero();
    }

    @Test
    void sendMessageRejectsQuestionFromAnotherWindow() {
        FakeMessageMapper messageMapper = new FakeMessageMapper();
        FakeQuestionMapper questionMapper = new FakeQuestionMapper();
        questionMapper.inserted.add(QuestionRecord.builder()
            .id(9L)
            .sessionId(30L)
            .windowId(99L)
            .userId(1L)
            .questionText("Wrong window")
            .questionType("reflection")
            .status("active")
            .build());
        SessionWindowBusiness business = new SessionWindowBusiness(
            new StubAiProvider(),
            new FakeSessionWindowMapper(),
            messageMapper,
            questionMapper,
            new FakePersonaMapper()
        );

        assertThatThrownBy(() -> business.sendMessage(10L, SendMessageRequest.builder()
                .content("Answer")
                .questionId(9L)
                .build()))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies((exception) -> {
                ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(responseStatusException.getReason()).isEqualTo("Question not found for session window");
            });

        assertThat(messageMapper.inserted).isEmpty();
    }

    @Test
    void debatePersistsPersonaResponse() {
        FakeMessageMapper messageMapper = new FakeMessageMapper();
        SessionWindowBusiness business = new SessionWindowBusiness(
            new StubAiProvider(),
            new FakeSessionWindowMapper(),
            messageMapper,
            new FakeQuestionMapper(),
            new FakePersonaMapper()
        );

        AiMessageResponse response = business.debate(10L, DebateMessageRequest.builder()
            .personaId(4L)
            .content("Challenge me")
            .build());

        assertThat(response.getMessageId()).isEqualTo(101L);
        assertThat(response.getPersonaId()).isEqualTo(4L);
        assertThat(messageMapper.inserted.get(1).getPersonaId()).isEqualTo(4L);
        assertThat(messageMapper.inserted.get(1).getContextSnapshot()).contains("\"persona\"", "Historian");
        assertThat(messageMapper.inserted.get(1).getPromptSnapshot())
            .contains("\"responseType\":\"persona_debate\"")
            .contains("\"safetyPolicyVersion\":\"mvp-safety-v1\"")
            .contains("\"readingBoundaryPolicyVersion\":\"mvp-reading-boundary-v1\"");
        assertThat(response.getPromptSnapshot()).isEqualTo(messageMapper.inserted.get(1).getPromptSnapshot());
        assertThat(messageMapper.inserted.get(1).getTokenUsage()).contains("\"total_tokens\":9");
        assertThat(response.getTokenUsage()).isEqualTo(messageMapper.inserted.get(1).getTokenUsage());
    }

    @Test
    void debateIgnoresClientSuppliedUserId() {
        FakeMessageMapper messageMapper = new FakeMessageMapper();
        SessionWindowBusiness business = new SessionWindowBusiness(
            new StubAiProvider(),
            new FakeSessionWindowMapper(),
            messageMapper,
            new FakeQuestionMapper(),
            new FakePersonaMapper()
        );

        business.debate(10L, DebateMessageRequest.builder()
            .userId(999L)
            .personaId(4L)
            .content("Challenge me")
            .build());

        assertThat(messageMapper.inserted).extracting(MessageRecord::getUserId).containsOnly(1L);
    }

    @Test
    void debateRejectsInactiveOrMissingPersonaBeforePersistingMessages() {
        FakeMessageMapper messageMapper = new FakeMessageMapper();
        SessionWindowBusiness business = new SessionWindowBusiness(
            new StubAiProvider(),
            new FakeSessionWindowMapper(),
            messageMapper,
            new FakeQuestionMapper(),
            new FakePersonaMapper()
        );

        assertThatThrownBy(() -> business.debate(10L, DebateMessageRequest.builder()
                .personaId(99L)
                .content("Challenge me")
                .build()))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies((exception) -> {
                ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(responseStatusException.getReason()).isEqualTo("Persona not found");
            });

        assertThat(messageMapper.inserted).isEmpty();
    }

    @Test
    void debateAllPersistsOnePromptAndEveryPersonaResponse() {
        FakeMessageMapper messageMapper = new FakeMessageMapper();
        SessionWindowBusiness business = new SessionWindowBusiness(
            new StubAiProvider(),
            new FakeSessionWindowMapper(),
            messageMapper,
            new FakeQuestionMapper(),
            new FakePersonaMapper()
        );

        assertThat(business.debateAll(10L, DebateAllMessageRequest.builder()
            .content("Compare this interpretation")
            .build()).getMessages()).hasSize(2);

        assertThat(messageMapper.inserted).hasSize(3);
        assertThat(messageMapper.inserted.get(0).getRole()).isEqualTo("user");
        assertThat(messageMapper.inserted.get(1).getParentMessageId()).isEqualTo(100L);
        assertThat(messageMapper.inserted.get(1).getPersonaId()).isEqualTo(4L);
        assertThat(messageMapper.inserted.get(1).getContextSnapshot()).contains("Historian");
        assertThat(messageMapper.inserted.get(1).getPromptSnapshot()).contains("\"responseType\":\"persona_debate\"");
        assertThat(messageMapper.inserted.get(1).getTokenUsage()).contains("\"total_tokens\":9");
        assertThat(messageMapper.inserted.get(2).getParentMessageId()).isEqualTo(100L);
        assertThat(messageMapper.inserted.get(2).getPersonaId()).isEqualTo(5L);
        assertThat(messageMapper.inserted.get(2).getContextSnapshot()).contains("Formalist");
        assertThat(messageMapper.inserted.get(2).getPromptSnapshot()).contains("\"responseType\":\"persona_debate\"");
        assertThat(messageMapper.inserted.get(2).getTokenUsage()).contains("\"total_tokens\":9");
    }

    @Test
    void debateAllIgnoresClientSuppliedUserId() {
        FakeMessageMapper messageMapper = new FakeMessageMapper();
        SessionWindowBusiness business = new SessionWindowBusiness(
            new StubAiProvider(),
            new FakeSessionWindowMapper(),
            messageMapper,
            new FakeQuestionMapper(),
            new FakePersonaMapper()
        );

        business.debateAll(10L, DebateAllMessageRequest.builder()
            .userId(999L)
            .content("Compare this interpretation")
            .build());

        assertThat(messageMapper.inserted).extracting(MessageRecord::getUserId).containsOnly(1L);
    }

    @Test
    void generateQuestionsPersistsAiSuggestions() {
        FakeQuestionMapper questionMapper = new FakeQuestionMapper();
        SessionWindowBusiness business = new SessionWindowBusiness(
            new StubAiProvider(),
            new FakeSessionWindowMapper(),
            new FakeMessageMapper(),
            questionMapper,
            new FakePersonaMapper()
        );

        QuestionListResponse response = business.generateQuestions(10L, GenerateQuestionsRequest.builder()
            .count(2)
            .focus("chapter one")
            .build());

        assertThat(response.getQuestions()).hasSize(1);
        assertThat(response.getQuestions().get(0).getQuestionId()).isEqualTo(900L);
        assertThat(questionMapper.inserted.get(0).getSessionId()).isEqualTo(30L);
        assertThat(questionMapper.inserted.get(0).getWindowId()).isEqualTo(10L);
        assertThat(questionMapper.inserted.get(0).getQuestionText()).contains("chapter one");
    }

    @Test
    void suggestQuestionsReturnsAiSuggestionsWithoutPersisting() {
        FakeQuestionMapper questionMapper = new FakeQuestionMapper();
        SessionWindowBusiness business = new SessionWindowBusiness(
            new StubAiProvider(),
            new FakeSessionWindowMapper(),
            new FakeMessageMapper(),
            questionMapper,
            new FakePersonaMapper()
        );

        QuestionListResponse response = business.suggestQuestions(10L, GenerateQuestionsRequest.builder()
            .count(2)
            .focus("chapter one")
            .build());

        assertThat(response.getQuestions()).singleElement()
            .extracting(QuestionDto::getQuestionText)
            .isEqualTo("What matters in chapter one?");
        assertThat(questionMapper.inserted).isEmpty();
    }

    @Test
    void generateQuestionsRejectsZeroRowQuestionInsert() {
        FakeQuestionMapper questionMapper = new FakeQuestionMapper();
        questionMapper.insertRows = 0;
        SessionWindowBusiness business = new SessionWindowBusiness(
            new StubAiProvider(),
            new FakeSessionWindowMapper(),
            new FakeMessageMapper(),
            questionMapper,
            new FakePersonaMapper()
        );

        assertServerError("Question could not be saved", () -> business.generateQuestions(10L, GenerateQuestionsRequest.builder()
            .count(1)
            .focus("chapter one")
            .build()));
    }

    @Test
    void createQuestionPersistsReaderPrompt() {
        FakeQuestionMapper questionMapper = new FakeQuestionMapper();
        SessionWindowBusiness business = new SessionWindowBusiness(
            new StubAiProvider(),
            new FakeSessionWindowMapper(),
            new FakeMessageMapper(),
            questionMapper,
            new FakePersonaMapper()
        );

        QuestionListResponse response = business.createQuestion(10L, CreateQuestionRequest.builder()
            .questionText("How does the reader notice power?")
            .build());

        assertThat(response.getQuestions()).singleElement()
            .satisfies((question) -> {
                assertThat(question.getQuestionText()).isEqualTo("How does the reader notice power?");
                assertThat(question.getQuestionType()).isEqualTo("reader");
            });
        assertThat(questionMapper.inserted.get(0).getSessionId()).isEqualTo(30L);
        assertThat(questionMapper.inserted.get(0).getWindowId()).isEqualTo(10L);
    }

    @Test
    void deleteQuestionSoftDeletesUnansweredPrompt() {
        FakeQuestionMapper questionMapper = new FakeQuestionMapper();
        questionMapper.inserted.add(QuestionRecord.builder()
            .id(900L)
            .sessionId(30L)
            .windowId(10L)
            .userId(1L)
            .questionText("Scratch prompt")
            .questionType("reader")
            .status("active")
            .build());
        SessionWindowBusiness business = new SessionWindowBusiness(
            new StubAiProvider(),
            new FakeSessionWindowMapper(),
            new FakeMessageMapper(),
            questionMapper,
            new FakePersonaMapper()
        );

        QuestionDto response = business.deleteQuestion(900L);

        assertThat(response.getQuestionId()).isEqualTo(900L);
        assertThat(questionMapper.deletedQuestionId).isEqualTo(900L);
        assertThat(questionMapper.deletedUserId).isEqualTo(1L);
    }

    @Test
    void deleteQuestionReturnsNotFoundWhenDeleteRowIsMissing() {
        FakeQuestionMapper questionMapper = new FakeQuestionMapper();
        questionMapper.deletedRows = 0;
        questionMapper.inserted.add(QuestionRecord.builder()
            .id(900L)
            .sessionId(30L)
            .windowId(10L)
            .userId(1L)
            .questionText("Scratch prompt")
            .questionType("reader")
            .status("active")
            .build());
        SessionWindowBusiness business = new SessionWindowBusiness(
            new StubAiProvider(),
            new FakeSessionWindowMapper(),
            new FakeMessageMapper(),
            questionMapper,
            new FakePersonaMapper()
        );

        assertNotFound("Question not found", () -> business.deleteQuestion(900L));
    }

    private void assertNotFound(String reason, Runnable action) {
        assertThatThrownBy(action::run)
            .isInstanceOf(ResponseStatusException.class)
            .satisfies((exception) -> {
                ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(responseStatusException.getReason()).isEqualTo(reason);
            });
    }

    private void assertServerError(String reason, Runnable action) {
        assertThatThrownBy(action::run)
            .isInstanceOf(ResponseStatusException.class)
            .satisfies((exception) -> {
                ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                assertThat(responseStatusException.getReason()).isEqualTo(reason);
            });
    }

    private static class FakeSessionWindowMapper implements SessionWindowMapper {
        private SessionWindowRecord inserted;
        private Long updatedWindowId;
        private String updatedTitle;
        private Long deletedWindowId;
        private int activeSessionCount = 1;
        private int activeWindowCount = 2;
        private int insertRows = 1;
        private int updatedRows = 1;
        private int deletedRows = 1;

        @Override
        public int insert(SessionWindowRecord record) {
            this.inserted = record;
            record.setId(300L);
            return insertRows;
        }

        @Override
        public SessionWindowRecord findById(Long id) {
            return SessionWindowRecord.builder()
                .id(id)
                .sessionId(30L)
                .userId(1L)
                .windowType("question")
                .title(updatedTitle == null ? "Question" : updatedTitle)
                .position(1)
                .status("open")
                .build();
        }

        @Override
        public SessionWindowContext findContextById(Long id) {
            return new SessionWindowContext(id, 30L, 1L);
        }

        @Override
        public int updateTitle(Long windowId, String title) {
            this.updatedWindowId = windowId;
            this.updatedTitle = title;
            return updatedRows;
        }

        @Override
        public int softDelete(Long windowId) {
            this.deletedWindowId = windowId;
            return deletedRows;
        }

        @Override
        public int countActiveBySessionId(Long sessionId) {
            return activeWindowCount;
        }

        @Override
        public int countActiveSessionById(Long sessionId, Long userId) {
            return activeSessionCount;
        }

        @Override
        public int selectNextPosition(Long sessionId) {
            return 5;
        }

        @Override
        public List<SessionWindowRecord> findBySessionId(Long sessionId) {
            return List.of();
        }
    }

    private static class FakeMessageMapper implements MessageMapper {
        private final List<MessageRecord> inserted = new ArrayList<>();
        private final List<Long> orderWindowIds = new ArrayList<>();
        private int nextId = 100;
        private int nextOrder = 1;
        private int insertRows = 1;

        @Override
        public int insert(MessageRecord record) {
            if (insertRows <= 0) {
                return insertRows;
            }
            record.setId((long) nextId++);
            inserted.add(record);
            return insertRows;
        }

        @Override
        public int selectNextOrder(Long sessionId, Long windowId) {
            orderWindowIds.add(windowId);
            return nextOrder++;
        }

        @Override
        public List<MessageRecord> findBySessionId(Long sessionId) {
            return List.of();
        }

        @Override
        public MessageRecord findEditableById(Long messageId, Long userId) {
            return null;
        }

        @Override
        public int updateContent(Long messageId, Long userId, String content) {
            return 1;
        }

        @Override
        public int softDelete(Long messageId, Long userId) {
            return 1;
        }
    }

    private static class FakeQuestionMapper implements QuestionMapper {
        private final List<QuestionRecord> inserted = new ArrayList<>();
        private long nextId = 900L;
        private Long deletedQuestionId;
        private Long deletedUserId;
        private int insertRows = 1;
        private int deletedRows = 1;

        @Override
        public int insert(QuestionRecord record) {
            if (insertRows <= 0) {
                return insertRows;
            }
            record.setId(nextId++);
            inserted.add(record);
            return insertRows;
        }

        @Override
        public List<QuestionRecord> findBySessionId(Long sessionId) {
            return List.of();
        }

        @Override
        public List<QuestionRecord> findByWindowId(Long windowId) {
            return inserted;
        }

        @Override
        public QuestionRecord findActiveById(Long questionId, Long userId) {
            return inserted.stream()
                .filter((question) -> question.getId().equals(questionId))
                .findFirst()
                .orElse(null);
        }

        @Override
        public int countActiveUserAnswers(Long questionId) {
            return 0;
        }

        @Override
        public int softDelete(Long questionId, Long userId) {
            this.deletedQuestionId = questionId;
            this.deletedUserId = userId;
            return deletedRows;
        }
    }

    private static class FakePersonaMapper implements PersonaMapper {
        @Override
        public int insert(PersonaRecord record) {
            return 1;
        }

        @Override
        public List<PersonaRecord> findActive() {
            return List.of(
                PersonaRecord.builder().id(4L).displayName("Historian").systemPrompt("Respond as historian").active(true).build(),
                PersonaRecord.builder().id(5L).displayName("Formalist").systemPrompt("Respond as formalist").active(true).build()
            );
        }

        @Override
        public List<PersonaRecord> findActiveForSession(Long sessionId) {
            return findActive();
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
            return 0;
        }
    }

    private static class FakeSessionHighlightMapper implements SessionHighlightMapper {
        @Override
        public int insert(SessionHighlightRecord record) {
            return 1;
        }

        @Override
        public int selectNextOrder(Long sessionId) {
            return 1;
        }

        @Override
        public List<SessionHighlightRecord> findBySessionId(Long sessionId) {
            return List.of(SessionHighlightRecord.builder()
                .id(12L)
                .sessionId(sessionId)
                .bookId(3L)
                .userId(1L)
                .pageNumber(42)
                .quoteText("fear is the mind-killer")
                .note("central quote")
                .highlightOrder(1)
                .build());
        }

        @Override
        public int update(Long sessionId, Long highlightId, Long userId, Integer pageNumber, String locationLabel, String quoteText, String note) {
            return 1;
        }

        @Override
        public int softDelete(Long sessionId, Long highlightId, Long userId) {
            return 1;
        }
    }

    private static class StubAiProvider implements AiProvider {
        private int windowAnswerCalls;

        @Override
        public BookCandidateSearchResponse suggestBooks(String query) {
            return BookCandidateSearchResponse.builder().build();
        }

        @Override
        public PersonaDraftListResponse suggestPersonas(GeneratePersonasRequest request) {
            return PersonaDraftListResponse.builder().build();
        }

        @Override
        public QuestionListResponse suggestQuestions(Long windowId, GenerateQuestionsRequest request) {
            return QuestionListResponse.builder()
                .questions(List.of(QuestionDto.builder()
                    .windowId(windowId)
                    .questionText("What matters in " + request.getFocus() + "?")
                    .questionType("reflection")
                    .status("active")
                    .aiModel("placeholder")
                    .build()))
                .build();
        }

        @Override
        public AiMessageResponse answerWindowMessage(Long windowId, SendMessageRequest request) {
            windowAnswerCalls++;
            return AiMessageResponse.builder()
                .messageId(null)
                .windowId(windowId)
                .role("assistant")
                .content("Answer")
                .streamingReady(true)
                .aiModel("placeholder")
                .tokenUsage("{\"input_tokens\":3,\"output_tokens\":4,\"total_tokens\":7}")
                .build();
        }

        @Override
        public AiMessageResponse answerDebateMessage(Long windowId, DebateMessageRequest request) {
            return AiMessageResponse.builder()
                .messageId(null)
                .windowId(windowId)
                .personaId(request.getPersonaId())
                .role("assistant")
                .content("Debate")
                .streamingReady(true)
                .aiModel("placeholder")
                .tokenUsage("{\"input_tokens\":4,\"output_tokens\":5,\"total_tokens\":9}")
                .build();
        }
    }
}
