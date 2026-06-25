package com.margins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.margins.session.mapper.SessionWindowMapper;
import com.margins.session.model.SessionWindowContext;
import com.margins.session.model.SessionWindowRecord;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class OpenAiAiProviderFallbackTest {

    @Test
    void usesReaderSafePlaceholderBehaviorWhenApiKeyIsMissing() {
        OpenAiAiProvider provider = new OpenAiAiProvider(
            new OpenAiProperties(),
            new ObjectMapper(),
            new FakeSessionWindowMapper(),
            new FakeMessageMapper(),
            new FakeQuestionMapper(),
            new FakePersonaMapper(),
            HttpClient.newHttpClient()
        );

        BookCandidateSearchResponse candidates = provider.suggestBooks("Dune");
        QuestionListResponse questions = provider.suggestQuestions(10L, GenerateQuestionsRequest.builder().count(2).focus("Dune").build());
        AiMessageResponse answer = provider.answerWindowMessage(10L, SendMessageRequest.builder().content("Answer").build());
        List<String> streamedDeltas = new ArrayList<>();
        AiMessageResponse streamed = provider.streamWindowMessage(10L, SendMessageRequest.builder().content("Answer").build(), streamedDeltas::add);
        AiMessageResponse debate = provider.answerDebateMessage(10L, DebateMessageRequest.builder().personaId(2L).content("Debate").build());

        assertThat(candidates.getAiModel()).isEqualTo("placeholder");
        assertThat(candidates.getCandidates()).singleElement()
            .satisfies((candidate) -> assertThat(candidate.getReason())
                .contains("임시 후보")
                .doesNotContain("OpenAI")
                .doesNotContain("integration"));
        assertThat(questions.getQuestions()).hasSize(2);
        assertThat(questions.getQuestions())
            .extracting((question) -> question.getQuestionText())
            .allSatisfy((questionText) -> assertThat(questionText)
                .containsAnyOf("어떤 장면", "어떤 구절")
                .doesNotContain("OpenAI")
                .doesNotContain("integration"));
        assertThat(answer.getAiModel()).isEqualTo("placeholder");
        assertThat(answer.getContent())
            .contains("임시 독서 응답")
            .doesNotContain("OpenAI")
            .doesNotContain("integration");
        assertThat(streamed.getAiModel()).isEqualTo("placeholder");
        assertThat(String.join("", streamedDeltas)).isEqualTo(streamed.getContent());
        assertThat(debate.getPersonaId()).isEqualTo(2L);
        assertThat(debate.getContent()).contains("임시 토론 응답");
    }

    @Test
    void requestsKoreanQuestionGenerationWhenConfigured() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/responses", (exchange) -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = ("{\"output_text\":\""
                + "[{\\\"questionText\\\":\\\"이 장면에서 가장 크게 흔들린 해석은 무엇인가요?\\\","
                + "\\\"questionType\\\":\\\"reflection\\\"}]"
                + "\"}").getBytes(StandardCharsets.UTF_8);
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
                HttpClient.newHttpClient()
            );

            QuestionListResponse response = provider.suggestQuestions(10L, GenerateQuestionsRequest.builder()
                .count(1)
                .focus("Dune")
                .build());

            assertThat(response.getQuestions()).singleElement()
                .extracting((question) -> question.getQuestionText())
                .isEqualTo("이 장면에서 가장 크게 흔들린 해석은 무엇인가요?");
            assertThat(requestBody.get()).contains("Generate concise reading reflection questions in Korean.");
            assertThat(requestBody.get()).contains("Every questionText must be natural Korean");
        } finally {
            server.stop(0);
        }
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
                "data: {\"type\":\"response.completed\"}\n\n"
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
                HttpClient.newHttpClient()
            );

            List<String> deltas = new ArrayList<>();
            AiMessageResponse response = provider.streamWindowMessage(10L, SendMessageRequest.builder().content("Answer").build(), deltas::add);

            assertThat(deltas).containsExactly("Hel", "lo");
            assertThat(response.getContent()).isEqualTo("Hello");
            assertThat(response.getAiModel()).isEqualTo(properties.getModel());
            assertThat(requestBody.get()).contains("\"stream\":true");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void readsEventStreamTextFromNonStreamingResponseWhenProviderReturnsSseBody() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        AtomicReference<String> acceptHeader = new AtomicReference<>();
        server.createContext("/responses", (exchange) -> {
            acceptHeader.set(exchange.getRequestHeaders().getFirst("Accept"));
            byte[] response = String.join("",
                "data: {\"type\":\"response.output_text.delta\",\"delta\":\"Debate \"}\n\n",
                "data: {\"type\":\"response.output_text.delta\",\"delta\":\"answer\"}\n\n",
                "data: {\"type\":\"response.completed\"}\n\n"
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
                HttpClient.newHttpClient()
            );

            AiMessageResponse response = provider.answerDebateMessage(10L, DebateMessageRequest.builder()
                .personaId(2L)
                .content("Debate")
                .build());

            assertThat(response.getContent()).isEqualTo("Debate answer");
            assertThat(response.getAiModel()).isEqualTo(properties.getModel());
            assertThat(acceptHeader.get()).isEqualTo("application/json");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void includesContextPackAndDebateStateInConfiguredDebatePrompt() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/responses", (exchange) -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"output_text\":\"Context aware answer\"}".getBytes(StandardCharsets.UTF_8);
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
                new FakeSessionWindowMapper() {
                    @Override
                    public SessionWindowContext findContextById(Long id) {
                        return new SessionWindowContext(
                            id,
                            1L,
                            1L,
                            11L,
                            "The Left Hand of Darkness",
                            "Ursula K. Le Guin",
                            "9780441478125",
                            "{\"aiProfile\":{\"themes\":[\"estrangement\"],\"source\":{\"confidence\":\"low\"}}}"
                        );
                    }

                    @Override
                    public SessionWindowRecord findById(Long id) {
                        return SessionWindowRecord.builder()
                            .id(id)
                            .sessionId(1L)
                            .windowType("debate")
                            .title("토론: 침묵은 회피인가")
                            .status("open")
                            .build();
                    }
                },
                new FakeMessageMapper() {
                    @Override
                    public List<MessageRecord> findBySessionId(Long sessionId) {
                        return List.of(
                            MessageRecord.builder()
                                .id(1L)
                                .sessionId(sessionId)
                                .windowId(10L)
                                .role("user")
                                .content("주인공의 침묵은 책임 회피처럼 보입니다.")
                                .build(),
                            MessageRecord.builder()
                                .id(2L)
                                .sessionId(sessionId)
                                .windowId(10L)
                                .role("assistant")
                                .personaId(2L)
                                .content("심리학자 관점에서는 방어기제로 볼 수 있습니다.")
                                .build()
                        );
                    }
                },
                new FakeQuestionMapper() {
                    @Override
                    public List<QuestionRecord> findByWindowId(Long windowId) {
                        return List.of(QuestionRecord.builder()
                            .id(7L)
                            .windowId(windowId)
                            .questionText("침묵은 선택인가, 강요된 반응인가?")
                            .build());
                    }
                },
                new FakePersonaMapper() {
                    @Override
                    public PersonaRecord findActiveById(Long id) {
                        return PersonaRecord.builder()
                            .id(id)
                            .name("psychologist")
                            .displayName("심리학자")
                            .systemPrompt("Respond through a careful psychology lens.")
                            .tone("분석적")
                            .active(true)
                            .build();
                    }
                },
                HttpClient.newHttpClient()
            );

            AiMessageResponse response = provider.answerDebateMessage(10L, DebateMessageRequest.builder()
                .personaId(2L)
                .content("그렇다면 이 침묵을 어떻게 이어서 봐야 할까요?")
                .build());

            assertThat(response.getContent()).isEqualTo("Context aware answer");
            assertThat(requestBody.get()).contains("AI Context Pack");
            assertThat(requestBody.get()).contains("Book profile");
            assertThat(requestBody.get()).contains("The Left Hand of Darkness");
            assertThat(requestBody.get()).contains("estrangement");
            assertThat(requestBody.get()).contains("Window type: debate");
            assertThat(requestBody.get()).contains("Debate state summary");
            assertThat(requestBody.get()).contains("userPosition");
            assertThat(requestBody.get()).contains("personaPositions");
            assertThat(requestBody.get()).contains("심리학자");
            assertThat(requestBody.get()).contains("Claim, Support, Question");
            assertThat(requestBody.get()).contains("침묵은 선택인가");
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
                HttpClient.newHttpClient()
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
    void fillsMissingPersonaRepliesWhenBatchResponseIsPartial() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        AtomicReference<Integer> requestCount = new AtomicReference<>(0);
        server.createContext("/responses", (exchange) -> {
            int currentRequest = requestCount.updateAndGet((count) -> count + 1);
            String output = currentRequest == 1
                ? "[{\"personaId\":1,\"content\":\"Batch answer\"}]"
                : "Missing persona answer";
            byte[] response = ("{\"output_text\":\"" + output.replace("\"", "\\\"") + "\"}").getBytes(StandardCharsets.UTF_8);
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
                HttpClient.newHttpClient()
            );

            List<AiMessageResponse> responses = provider.answerDebateMessages(10L, List.of(
                DebateMessageRequest.builder().personaId(1L).content("Debate").build(),
                DebateMessageRequest.builder().personaId(2L).content("Debate").build()
            ));

            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(AiMessageResponse::getPersonaId).containsExactly(1L, 2L);
            assertThat(responses).extracting(AiMessageResponse::getContent).containsExactly("Batch answer", "Missing persona answer");
            assertThat(requestCount.get()).isEqualTo(2);
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
            return new SessionWindowContext(id, 1L, 1L);
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
    }
}
