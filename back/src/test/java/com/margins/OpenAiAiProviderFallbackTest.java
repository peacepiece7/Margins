package com.margins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.margins.ai.AiAnswerQualityPolicy;
import com.margins.ai.AiSafetyPolicy;
import com.margins.ai.OpenAiAiProvider;
import com.margins.ai.OpenAiProperties;
import com.margins.book.dto.BookCandidateSearchResponse;
import com.margins.message.mapper.MessageMapper;
import com.margins.message.model.MessageRecord;
import com.margins.persona.mapper.PersonaMapper;
import com.margins.persona.model.PersonaRecord;
import com.margins.question.dto.GenerateQuestionsRequest;
import com.margins.question.dto.QuestionListResponse;
import com.margins.question.mapper.QuestionMapper;
import com.margins.question.model.QuestionRecord;
import com.margins.session.dto.AiMessageResponse;
import com.margins.session.dto.DebateMessageRequest;
import com.margins.session.dto.SendMessageRequest;
import com.margins.session.mapper.SessionHighlightMapper;
import com.margins.session.mapper.SessionInsightMapper;
import com.margins.session.model.SessionHighlightRecord;
import com.margins.session.model.SessionInsightRecord;
import com.margins.session.mapper.SessionWindowMapper;
import com.margins.session.model.SessionWindowContext;
import com.margins.session.model.SessionWindowRecord;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class OpenAiAiProviderFallbackTest {

    @Test
    void usesPlaceholderBehaviorWhenApiKeyIsMissing() {
        OpenAiAiProvider provider = new OpenAiAiProvider(
            new OpenAiProperties(),
            new ObjectMapper(),
            new FakeSessionWindowMapper(),
            new FakeMessageMapper(),
            new FakeQuestionMapper(),
            new FakePersonaMapper(),
            new FakeSessionHighlightMapper(),
            new FakeSessionInsightMapper(),
            new AiSafetyPolicy(),
            new AiAnswerQualityPolicy()
        );

        BookCandidateSearchResponse candidates = provider.suggestBooks("Dune");
        QuestionListResponse questions = provider.suggestQuestions(10L, GenerateQuestionsRequest.builder().count(2).focus("Dune").build());
        AiMessageResponse answer = provider.answerWindowMessage(10L, SendMessageRequest.builder().content("Answer").build());
        List<String> streamedDeltas = new ArrayList<>();
        AiMessageResponse streamed = provider.streamWindowMessage(10L, SendMessageRequest.builder().content("Answer").build(), streamedDeltas::add);
        AiMessageResponse debate = provider.answerDebateMessage(10L, DebateMessageRequest.builder().personaId(2L).content("Debate").build());

        assertThat(candidates.getAiModel()).isEqualTo("placeholder");
        assertThat(questions.getQuestions()).hasSize(2);
        assertThat(answer.getAiModel()).isEqualTo("placeholder");
        assertThat(streamed.getAiModel()).isEqualTo("placeholder");
        assertThat(String.join("", streamedDeltas)).isEqualTo(streamed.getContent());
        assertThat(debate.getPersonaId()).isEqualTo(2L);
    }

    @Test
    void streamsOpenAiResponseDeltasWhenConfigured() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/responses", (exchange) -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = String.join("",
                "data: {\"type\":\"response.output_text.delta\",\"delta\":\"Hel\"}\n\n",
                "data: {\"type\":\"response.output_text.delta\",\"delta\":\"lo\"}\n\n",
                "data: {\"type\":\"response.completed\",\"response\":{\"usage\":{\"input_tokens\":3,\"output_tokens\":2,\"total_tokens\":5}}}\n\n"
            ).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            OpenAiProperties properties = new OpenAiProperties();
            properties.setApiKey("test-key");
            properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            OpenAiAiProvider provider = new OpenAiAiProvider(
                properties,
                new ObjectMapper(),
                new FakeSessionWindowMapper(),
                new FakeMessageMapper(),
                new FakeQuestionMapper(),
                new FakePersonaMapper(),
                new FakeSessionHighlightMapper(),
                new FakeSessionInsightMapper(),
                new AiSafetyPolicy(),
                new AiAnswerQualityPolicy()
            );

            List<String> deltas = new ArrayList<>();
            AiMessageResponse response = provider.streamWindowMessage(10L, SendMessageRequest.builder().content("Answer").build(), deltas::add);

            assertThat(deltas).containsExactly("Hel", "lo");
            assertThat(response.getContent())
                .startsWith("Hello")
                .contains("Evidence:")
                .contains("Uncertainty:");
            assertThat(response.getAiModel()).isEqualTo(properties.getModel());
            assertThat(response.getTokenUsage()).contains("\"total_tokens\":5");
            assertThat(requestBody.get())
                .contains("\"stream\":true")
                .contains("Response grounding contract")
                .contains("Response structure")
                .contains("Ground the reply in the provided session context")
                .contains("If the provided context is insufficient")
                .contains("Respect the reading boundary");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void includesGroundingContractInAnswerAndDebatePrompts() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        List<String> requestBodies = new ArrayList<>();
        server.createContext("/responses", (exchange) -> {
            requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"output_text\":\"Grounded response\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            OpenAiProperties properties = new OpenAiProperties();
            properties.setApiKey("test-key");
            properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            OpenAiAiProvider provider = new OpenAiAiProvider(
                properties,
                new ObjectMapper(),
                new FakeSessionWindowMapper(),
                new FakeMessageMapper(),
                new FakeQuestionMapper(),
                new FakePersonaMapper(),
                new FakeSessionHighlightMapper(),
                new FakeSessionInsightMapper(),
                new AiSafetyPolicy(),
                new AiAnswerQualityPolicy()
            );

            provider.answerWindowMessage(10L, SendMessageRequest.builder().questionId(7L).content("What is the current claim?").build());
            provider.answerDebateMessage(10L, DebateMessageRequest.builder().personaId(2L).content("Challenge this reading.").build());

            assertThat(requestBodies).hasSize(2);
            assertThat(requestBodies.get(0))
                .contains("Response grounding contract")
                .contains("Response structure")
                .contains("Ground the reply in the provided session context")
                .contains("Name the supporting quote, note, message, or question")
                .contains("If the provided context is insufficient")
                .contains("Respect the reading boundary");
            assertThat(requestBodies.get(1))
                .contains("Response grounding contract")
                .contains("Response structure")
                .contains("Ground the reply in the provided session context")
                .contains("Name the supporting quote, note, message, or question")
                .contains("If the provided context is insufficient")
                .contains("Respect the reading boundary")
                .contains("Respond as a test persona");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void preservesNestedOpenAiStreamErrorMessageAfterProviderDelta() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/responses", (exchange) -> {
            byte[] response = String.join("",
                "data: {\"type\":\"response.output_text.delta\",\"delta\":\"Partial\"}\n\n",
                "data: {\"type\":\"error\",\"error\":{\"message\":\"rate limit from provider\"}}\n\n"
            ).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            OpenAiProperties properties = new OpenAiProperties();
            properties.setApiKey("test-key");
            properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            OpenAiAiProvider provider = new OpenAiAiProvider(
                properties,
                new ObjectMapper(),
                new FakeSessionWindowMapper(),
                new FakeMessageMapper(),
                new FakeQuestionMapper(),
                new FakePersonaMapper(),
                new FakeSessionHighlightMapper(),
                new FakeSessionInsightMapper(),
                new AiSafetyPolicy(),
                new AiAnswerQualityPolicy()
            );

            List<String> deltas = new ArrayList<>();
            assertThatThrownBy(() -> provider.streamWindowMessage(10L, SendMessageRequest.builder().content("Answer").build(), deltas::add))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("rate limit from provider");
            assertThat(deltas).containsExactly("Partial");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void includesBookAndReaderRecordsInOpenAiContext() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/responses", (exchange) -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"output_text\":\"Context response\",\"usage\":{\"input_tokens\":11,\"output_tokens\":7,\"total_tokens\":18}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            OpenAiProperties properties = new OpenAiProperties();
            properties.setApiKey("test-key");
            properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            OpenAiAiProvider provider = new OpenAiAiProvider(
                properties,
                new ObjectMapper(),
                new FakeSessionWindowMapper(),
                new FakeMessageMapper(),
                new FakeQuestionMapper(),
                new FakePersonaMapper(),
                new FakeSessionHighlightMapper(),
                new FakeSessionInsightMapper(),
                new AiSafetyPolicy(),
                new AiAnswerQualityPolicy()
            );

            AiMessageResponse response = provider.answerDebateMessage(10L, DebateMessageRequest.builder().personaId(2L).content("Debate").build());

            assertThat(requestBody.get())
                .contains("Book title")
                .contains("Dune")
                .contains("Reading boundary")
                .contains("current page 128")
                .contains("do not reveal or assume content beyond the current page")
                .contains("Reader quotes and notes")
                .contains("fear is the mind-killer")
                .contains("Reader summaries and insights")
                .contains("power and ecology");
            assertThat(response.getTokenUsage()).contains("\"input_tokens\":11", "\"total_tokens\":18");
            assertThat(response.getContent()).contains("Evidence:", "Uncertainty:");
        } finally {
            server.stop(0);
        }
    }

    private static class FakeSessionWindowMapper implements SessionWindowMapper {
        @Override
        public int insert(SessionWindowRecord record) {
            return 1;
        }

        @Override
        public SessionWindowRecord findById(Long id) {
            return SessionWindowRecord.builder().id(id).sessionId(1L).windowType("question").title("Question").status("open").build();
        }

        @Override
        public SessionWindowContext findContextById(Long id) {
            SessionWindowContext context = new SessionWindowContext(id, 1L, 1L);
            context.setBookTitle("Dune");
            context.setBookAuthor("Frank Herbert");
            context.setSessionTitle("Dune reflection");
            context.setReadingGoal("power and ecology");
            context.setStartPage(1);
            context.setCurrentPage(128);
            context.setTargetPage(412);
            context.setProgressNote("Arrakis politics");
            context.setSummary("A session about power and ecology.");
            return context;
        }

        @Override
        public int updateTitle(Long windowId, String title) {
            return 1;
        }

        @Override
        public int softDelete(Long windowId) {
            return 1;
        }

        @Override
        public int countActiveBySessionId(Long sessionId) {
            return findBySessionId(sessionId).size();
        }

        @Override
        public int countActiveSessionById(Long sessionId, Long userId) {
            return 1;
        }

        @Override
        public int selectNextPosition(Long sessionId) {
            return 1;
        }

        @Override
        public List<SessionWindowRecord> findBySessionId(Long sessionId) {
            return List.of();
        }
    }

    private static class FakeMessageMapper implements MessageMapper {
        @Override
        public int insert(MessageRecord record) {
            return 1;
        }

        @Override
        public int selectNextOrder(Long sessionId, Long windowId) {
            return 1;
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
        @Override
        public int insert(QuestionRecord record) {
            return 1;
        }

        @Override
        public List<QuestionRecord> findBySessionId(Long sessionId) {
            return List.of();
        }

        @Override
        public List<QuestionRecord> findByWindowId(Long windowId) {
            return List.of();
        }

        @Override
        public QuestionRecord findActiveById(Long questionId, Long userId) {
            return null;
        }

        @Override
        public int countActiveUserAnswers(Long questionId) {
            return 0;
        }

        @Override
        public int softDelete(Long questionId, Long userId) {
            return 1;
        }
    }

    private static class FakePersonaMapper implements PersonaMapper {
        @Override
        public int insert(PersonaRecord record) {
            return 1;
        }

        @Override
        public List<PersonaRecord> findActive() {
            return List.of();
        }

        @Override
        public List<PersonaRecord> findActiveForSession(Long sessionId) {
            return findActive();
        }

        @Override
        public PersonaRecord findActiveById(Long id) {
            return PersonaRecord.builder()
                .id(id)
                .name("test")
                .displayName("Test Persona")
                .systemPrompt("Respond as a test persona.")
                .tone("test")
                .active(true)
                .build();
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
                .sessionId(sessionId)
                .pageNumber(12)
                .locationLabel("chapter 1")
                .quoteText("fear is the mind-killer")
                .note("central discipline")
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

    private static class FakeSessionInsightMapper implements SessionInsightMapper {
        @Override
        public int insert(SessionInsightRecord record) {
            return 1;
        }

        @Override
        public int selectNextOrder(Long sessionId) {
            return 1;
        }

        @Override
        public List<SessionInsightRecord> findBySessionId(Long sessionId, Long userId) {
            return List.of(SessionInsightRecord.builder()
                .sessionId(sessionId)
                .userId(userId)
                .insightType("takeaway")
                .title("power and ecology")
                .content("Political control depends on ecological scarcity.")
                .evidence("Arrakis")
                .build());
        }

        @Override
        public int softDelete(Long sessionId, Long insightId, Long userId) {
            return 1;
        }
    }
}
