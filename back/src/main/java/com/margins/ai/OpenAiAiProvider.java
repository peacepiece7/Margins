package com.margins.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.margins.book.dto.BookCandidateDto;
import com.margins.book.dto.BookCandidateSearchResponse;
import com.margins.message.mapper.MessageMapper;
import com.margins.message.model.MessageRecord;
import com.margins.persona.mapper.PersonaMapper;
import com.margins.persona.model.PersonaRecord;
import com.margins.question.dto.GenerateQuestionsRequest;
import com.margins.question.dto.QuestionDto;
import com.margins.question.dto.QuestionListResponse;
import com.margins.question.mapper.QuestionMapper;
import com.margins.question.model.QuestionRecord;
import com.margins.session.dto.AiMessageResponse;
import com.margins.session.dto.DebateMessageRequest;
import com.margins.session.dto.SendMessageRequest;
import com.margins.session.mapper.SessionWindowMapper;
import com.margins.session.model.SessionWindowContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "margins.ai.provider", havingValue = "openai")
public class OpenAiAiProvider implements AiProvider {

    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;
    private final SessionWindowMapper sessionWindowMapper;
    private final MessageMapper messageMapper;
    private final QuestionMapper questionMapper;
    private final PersonaMapper personaMapper;
    private final PlaceholderAiProvider fallback = new PlaceholderAiProvider();

    @Override
    public BookCandidateSearchResponse suggestBooks(String query) {
        if (!configured()) {
            return fallback.suggestBooks(query);
        }

        try {
            String output = createText(
                "Return exactly three likely book candidates as JSON array. Each item must have title, author, and reason.",
                "Book search query: " + query
            );
            List<BookCandidateDto> candidates = parseBookCandidates(output);
            if (candidates.isEmpty()) {
                return fallback.suggestBooks(query);
            }

            return BookCandidateSearchResponse.builder()
                .aiModel(properties.getModel())
                .candidates(candidates)
                .build();
        } catch (RuntimeException exception) {
            return fallback.suggestBooks(query);
        }
    }

    @Override
    public QuestionListResponse suggestQuestions(Long windowId, GenerateQuestionsRequest request) {
        if (!configured()) {
            return fallback.suggestQuestions(windowId, request);
        }

        try {
            String context = contextForWindow(windowId);
            String output = createText(
                "Generate concise reading reflection questions. Return a JSON array of objects with questionText and questionType.",
                context + "\nFocus: " + safe(request.getFocus()) + "\nCount: " + (request.getCount() == null ? 3 : request.getCount())
            );
            List<QuestionDto> questions = parseQuestions(windowId, output);
            if (questions.isEmpty()) {
                return fallback.suggestQuestions(windowId, request);
            }

            return QuestionListResponse.builder().questions(questions).build();
        } catch (RuntimeException exception) {
            return fallback.suggestQuestions(windowId, request);
        }
    }

    @Override
    public AiMessageResponse answerWindowMessage(Long windowId, SendMessageRequest request) {
        if (!configured()) {
            return fallback.answerWindowMessage(windowId, request);
        }

        try {
            String output = createText(
                "Respond as a precise reading companion. Use the selected question and prior session context. Keep the answer under 140 words.",
                contextForWindow(windowId) + "\nSelected question id: " + request.getQuestionId() + "\nReader answer: " + request.getContent()
            );

            return AiMessageResponse.builder()
                .windowId(windowId)
                .role("assistant")
                .content(output)
                .streamingReady(true)
                .aiModel(properties.getModel())
                .build();
        } catch (RuntimeException exception) {
            return fallback.answerWindowMessage(windowId, request);
        }
    }

    @Override
    public AiMessageResponse streamWindowMessage(Long windowId, SendMessageRequest request, Consumer<String> deltaConsumer) {
        if (!configured()) {
            return AiProvider.super.streamWindowMessage(windowId, request, deltaConsumer);
        }

        AtomicBoolean emittedProviderDelta = new AtomicBoolean(false);
        try {
            String output = createTextStream(
                "Respond as a precise reading companion. Use the selected question and prior session context. Keep the answer under 140 words.",
                contextForWindow(windowId) + "\nSelected question id: " + request.getQuestionId() + "\nReader answer: " + request.getContent(),
                (delta) -> {
                    emittedProviderDelta.set(true);
                    deltaConsumer.accept(delta);
                }
            );

            return AiMessageResponse.builder()
                .windowId(windowId)
                .role("assistant")
                .content(output)
                .streamingReady(true)
                .aiModel(properties.getModel())
                .build();
        } catch (RuntimeException exception) {
            if (emittedProviderDelta.get()) {
                throw exception;
            }
            return AiProvider.super.streamWindowMessage(windowId, request, deltaConsumer);
        }
    }

    @Override
    public AiMessageResponse answerDebateMessage(Long windowId, DebateMessageRequest request) {
        if (!configured()) {
            return fallback.answerDebateMessage(windowId, request);
        }

        try {
            PersonaRecord persona = personaMapper.findActiveById(request.getPersonaId());
            String personaPrompt = persona == null ? "Respond as a literary discussion participant." : persona.getSystemPrompt();
            String output = createText(
                personaPrompt + "\nChallenge or extend the reader's interpretation. Keep the answer under 140 words.",
                contextForWindow(windowId) + "\nReader debate message: " + request.getContent()
            );

            return AiMessageResponse.builder()
                .windowId(windowId)
                .role("assistant")
                .personaId(request.getPersonaId())
                .content(output)
                .streamingReady(true)
                .aiModel(properties.getModel())
                .build();
        } catch (RuntimeException exception) {
            return fallback.answerDebateMessage(windowId, request);
        }
    }

    private boolean configured() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    private String createText(String instructions, String input) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", properties.getModel());
            ArrayNode messages = root.putArray("input");
            messages.add(message("developer", instructions));
            messages.add(message("user", input));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getBaseUrl() + "/responses"))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(root)))
                .build();
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("OpenAI request failed: " + response.statusCode());
            }

            String text = extractOutputText(objectMapper.readTree(response.body()));
            if (text.isBlank()) {
                throw new IllegalStateException("OpenAI response did not include text output");
            }
            return text.trim();
        } catch (IOException exception) {
            throw new IllegalStateException("OpenAI response could not be parsed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenAI request interrupted", exception);
        }
    }

    private String createTextStream(String instructions, String input, Consumer<String> deltaConsumer) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", properties.getModel());
            root.put("stream", true);
            ArrayNode messages = root.putArray("input");
            messages.add(message("developer", instructions));
            messages.add(message("user", input));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getBaseUrl() + "/responses"))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(root)))
                .build();
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("OpenAI stream request failed: " + response.statusCode());
            }

            String output = readStreamedOutput(response.body(), deltaConsumer);
            if (output.isBlank()) {
                throw new IllegalStateException("OpenAI stream did not include text output");
            }
            return output.trim();
        } catch (IOException exception) {
            throw new IllegalStateException("OpenAI stream could not be parsed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenAI stream request interrupted", exception);
        }
    }

    private String readStreamedOutput(InputStream inputStream, Consumer<String> deltaConsumer) throws IOException {
        StringBuilder output = new StringBuilder();
        StringBuilder eventData = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    appendStreamEvent(eventData.toString(), output, deltaConsumer);
                    eventData.setLength(0);
                } else if (line.startsWith("data:")) {
                    if (!eventData.isEmpty()) {
                        eventData.append('\n');
                    }
                    eventData.append(line.substring(5).trim());
                }
            }
        }
        appendStreamEvent(eventData.toString(), output, deltaConsumer);
        return output.toString();
    }

    private void appendStreamEvent(String data, StringBuilder output, Consumer<String> deltaConsumer) throws IOException {
        if (data.isBlank() || "[DONE]".equals(data)) {
            return;
        }

        JsonNode event = objectMapper.readTree(data);
        if ("error".equals(event.path("type").asText())) {
            String message = streamErrorMessage(event);
            throw new IllegalStateException(message);
        }

        String delta = extractStreamDelta(event);
        if (!delta.isEmpty()) {
            output.append(delta);
            deltaConsumer.accept(delta);
        }
    }

    private String extractStreamDelta(JsonNode event) {
        String type = event.path("type").asText();
        if ("response.output_text.delta".equals(type) && event.hasNonNull("delta")) {
            return event.path("delta").asText();
        }
        if (type.endsWith(".delta") && event.hasNonNull("text")) {
            return event.path("text").asText();
        }
        return "";
    }

    private String streamErrorMessage(JsonNode event) {
        String message = event.path("message").asText("");
        if (message.isBlank()) {
            message = event.path("error").path("message").asText("");
        }
        if (message.isBlank()) {
            message = event.path("error").path("detail").asText("");
        }
        return message.isBlank() ? "OpenAI stream failed" : message;
    }

    private ObjectNode message(String role, String content) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("role", role);
        node.put("content", content);
        return node;
    }

    private String contextForWindow(Long windowId) {
        SessionWindowContext context = sessionWindowMapper.findContextById(windowId);
        if (context == null) {
            return "No persisted session context is available.";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Session id: ").append(context.getSessionId()).append('\n');
        List<QuestionRecord> questions = questionMapper.findByWindowId(windowId);
        if (!questions.isEmpty()) {
            builder.append("Questions:\n");
            questions.stream().limit(5).forEach((question) -> builder
                .append("- #")
                .append(question.getId())
                .append(": ")
                .append(question.getQuestionText())
                .append('\n'));
        }

        List<MessageRecord> messages = messageMapper.findBySessionId(context.getSessionId());
        if (!messages.isEmpty()) {
            builder.append("Recent messages:\n");
            messages.stream().skip(Math.max(0, messages.size() - 8)).forEach((message) -> builder
                .append("- ")
                .append(message.getRole())
                .append(": ")
                .append(message.getContent())
                .append('\n'));
        }
        return builder.toString();
    }

    private List<BookCandidateDto> parseBookCandidates(String output) {
        List<BookCandidateDto> candidates = new ArrayList<>();
        JsonNode root = parseJsonArray(output);
        for (int index = 0; index < root.size(); index++) {
            JsonNode item = root.get(index);
            String title = item.path("title").asText("");
            if (!title.isBlank()) {
                candidates.add(BookCandidateDto.builder()
                    .candidateId("openai-" + (index + 1))
                    .title(title)
                    .author(item.path("author").asText("Unknown"))
                    .reason(item.path("reason").asText(""))
                    .build());
            }
        }
        return candidates;
    }

    private List<QuestionDto> parseQuestions(Long windowId, String output) {
        List<QuestionDto> questions = new ArrayList<>();
        JsonNode root = parseJsonArray(output);
        for (int index = 0; index < root.size(); index++) {
            JsonNode item = root.get(index);
            String questionText = item.path("questionText").asText("");
            if (!questionText.isBlank()) {
                questions.add(QuestionDto.builder()
                    .windowId(windowId)
                    .questionText(questionText)
                    .questionType(item.path("questionType").asText("reflection"))
                    .status("active")
                    .aiModel(properties.getModel())
                    .build());
            }
        }
        return questions;
    }

    private JsonNode parseJsonArray(String output) {
        try {
            String trimmed = output.trim();
            int start = trimmed.indexOf('[');
            int end = trimmed.lastIndexOf(']');
            if (start >= 0 && end > start) {
                trimmed = trimmed.substring(start, end + 1);
            }
            JsonNode node = objectMapper.readTree(trimmed);
            return node.isArray() ? node : objectMapper.createArrayNode();
        } catch (IOException exception) {
            return objectMapper.createArrayNode();
        }
    }

    private String extractOutputText(JsonNode root) {
        if (root.hasNonNull("output_text")) {
            return root.path("output_text").asText();
        }

        StringBuilder builder = new StringBuilder();
        appendOutputText(root, builder);
        return builder.toString();
    }

    private void appendOutputText(JsonNode node, StringBuilder builder) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isObject() && "output_text".equals(node.path("type").asText()) && node.hasNonNull("text")) {
            builder.append(node.path("text").asText());
        }
        if (node.isContainerNode()) {
            node.forEach((child) -> appendOutputText(child, builder));
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
