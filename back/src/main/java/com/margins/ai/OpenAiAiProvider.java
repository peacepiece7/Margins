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
import com.margins.session.model.SessionWindowRecord;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "margins.ai.provider", havingValue = "openai")
public class OpenAiAiProvider implements AiProvider {

    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;
    private final SessionWindowMapper sessionWindowMapper;
    private final MessageMapper messageMapper;
    private final QuestionMapper questionMapper;
    private final PersonaMapper personaMapper;
    private final HttpClient httpClient;
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
            logOpenAiFallback("book candidates", exception);
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
                "Generate concise reading reflection questions in Korean. Every questionText must be natural Korean, even when the focus contains English titles or names. Return a JSON array of objects with questionText and questionType.",
                context + "\nFocus: " + safe(request.getFocus()) + "\nCount: " + (request.getCount() == null ? 3 : request.getCount())
            );
            List<QuestionDto> questions = parseQuestions(windowId, output);
            if (questions.isEmpty()) {
                return fallback.suggestQuestions(windowId, request);
            }

            return QuestionListResponse.builder().questions(questions).build();
        } catch (RuntimeException exception) {
            logOpenAiFallback("question generation", exception);
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
            logOpenAiFallback("window answer", exception);
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
            logOpenAiFallback("window stream", exception);
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
            logOpenAiFallback("debate answer", exception);
            return fallback.answerDebateMessage(windowId, request);
        }
    }

    @Override
    public List<AiMessageResponse> answerDebateMessages(Long windowId, List<DebateMessageRequest> requests) {
        if (!configured()) {
            return AiProvider.super.answerDebateMessages(windowId, requests);
        }
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        try {
            String output = createText(
                "Return a JSON array of persona debate replies. Each object must include personaId and content. Match exactly the requested personaId values. Keep each content under 140 words.",
                contextForWindow(windowId)
                    + "\nReader debate message: "
                    + safe(requests.get(0).getContent())
                    + "\nPersonas:\n"
                    + personaBatchPrompt(requests)
            );
            List<AiMessageResponse> responses = parseDebateResponses(windowId, requests, output);
            if (responses.isEmpty()) {
                return AiProvider.super.answerDebateMessages(windowId, requests);
            }
            return fillMissingDebateResponses(windowId, requests, responses);
        } catch (RuntimeException exception) {
            logOpenAiFallback("debate batch answer", exception);
            return AiProvider.super.answerDebateMessages(windowId, requests);
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
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(root)))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("OpenAI request failed: "
                    + response.statusCode()
                    + " body="
                    + summarizeForLog(response.body()));
            }

            String text = extractOutputText(parseTextResponseBody(response));
            if (text.isBlank()) {
                throw new IllegalStateException("OpenAI response did not include text output");
            }
            return text.trim();
        } catch (IOException exception) {
            throw new IllegalStateException("OpenAI request could not be completed", exception);
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
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(root)))
                .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String body = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new IllegalStateException("OpenAI stream request failed: "
                    + response.statusCode()
                    + " body="
                    + summarizeForLog(body));
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

    private JsonNode parseTextResponseBody(HttpResponse<String> response) {
        String body = response.body();
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("OpenAI response body was empty. status="
                + response.statusCode()
                + ", contentType="
                + responseContentType(response)
                + ", bodyLength=0");
        }

        try {
            return objectMapper.readTree(body);
        } catch (IOException jsonException) {
            if (looksLikeEventStream(body)) {
                try {
                    String streamedOutput = readStreamedOutput(
                        new java.io.ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)),
                        (delta) -> {
                        }
                    );
                    ObjectNode root = objectMapper.createObjectNode();
                    root.put("output_text", streamedOutput);
                    return root;
                } catch (IOException streamException) {
                    throw malformedTextResponse(response, streamException);
                }
            }

            throw malformedTextResponse(response, jsonException);
        }
    }

    private boolean looksLikeEventStream(String body) {
        return body.stripLeading().startsWith("data:");
    }

    private IllegalStateException malformedTextResponse(HttpResponse<String> response, Exception cause) {
        String body = response.body() == null ? "" : response.body();
        return new IllegalStateException("OpenAI response could not be parsed. status="
            + response.statusCode()
            + ", contentType="
            + responseContentType(response)
            + ", bodyLength="
            + body.length()
            + ", bodyPreview="
            + summarizeForLog(body), cause);
    }

    private String responseContentType(HttpResponse<?> response) {
        return response.headers().firstValue("content-type").orElse("");
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
        builder.append("AI Context Pack\n");
        builder.append("Session id: ").append(context.getSessionId()).append('\n');
        builder.append("Window id: ").append(windowId).append('\n');
        appendBookProfile(builder, context);

        SessionWindowRecord window = sessionWindowMapper.findById(windowId);
        if (window != null) {
            builder.append("Window type: ").append(safe(window.getWindowType())).append('\n');
            builder.append("Window title/topic: ").append(safe(window.getTitle())).append('\n');
        }

        builder.append("Conversation rules:\n");
        builder.append("- Connect to the reader's latest point before adding a new interpretation.\n");
        builder.append("- Ground claims in persisted questions, highlights, messages, or explicit book metadata.\n");
        builder.append("- Do not invent plot details or author intent when context is missing.\n");
        builder.append("- Prefer Claim, Support, Question when it helps the discussion continue.\n");

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
        appendDebateState(builder, windowId, messages);
        if (!messages.isEmpty()) {
            builder.append("Recent messages:\n");
            messages.stream().skip(Math.max(0, messages.size() - 8)).forEach((message) -> builder
                .append("- ")
                .append(messageLabel(message))
                .append(": ")
                .append(truncate(message.getContent(), 500))
                .append('\n'));
        }
        return builder.toString();
    }

    private void appendBookProfile(StringBuilder builder, SessionWindowContext context) {
        if (context.getBookId() == null) {
            return;
        }
        builder.append("Book profile:\n");
        builder.append("- bookId: ").append(context.getBookId()).append('\n');
        builder.append("- title: ").append(safe(context.getBookTitle())).append('\n');
        builder.append("- author: ").append(safe(context.getBookAuthor())).append('\n');
        if (context.getBookIsbn() != null && !context.getBookIsbn().isBlank()) {
            builder.append("- isbn: ").append(context.getBookIsbn()).append('\n');
        }
        if (context.getBookRawMetadata() != null && !context.getBookRawMetadata().isBlank()) {
            builder.append("- metadata: ")
                .append(truncate(context.getBookRawMetadata(), 1200))
                .append('\n');
        }
    }

    private void appendDebateState(StringBuilder builder, Long windowId, List<MessageRecord> messages) {
        List<MessageRecord> windowMessages = messages.stream()
            .filter((message) -> windowId.equals(message.getWindowId()))
            .toList();
        if (windowMessages.isEmpty()) {
            return;
        }

        MessageRecord latestUser = null;
        List<MessageRecord> personaReplies = new ArrayList<>();
        for (MessageRecord message : windowMessages) {
            if ("user".equals(message.getRole())) {
                latestUser = message;
            } else if (message.getPersonaId() != null) {
                personaReplies.add(message);
            }
        }

        builder.append("Debate state summary:\n");
        if (latestUser != null) {
            builder.append("- userPosition: ")
                .append(truncate(latestUser.getContent(), 300))
                .append('\n');
        }
        if (!personaReplies.isEmpty()) {
            builder.append("- personaPositions:\n");
            personaReplies.stream()
                .skip(Math.max(0, personaReplies.size() - 4))
                .forEach((message) -> builder
                    .append("  - ")
                    .append(messageLabel(message))
                    .append(": ")
                    .append(truncate(message.getContent(), 240))
                    .append('\n'));
        }
        builder.append("- nextBestMove: acknowledge the latest reader position, contrast useful lenses, and leave one follow-up question.\n");
    }

    private String messageLabel(MessageRecord message) {
        if (message.getPersonaId() == null) {
            return safe(message.getRole());
        }
        PersonaRecord persona = personaMapper.findActiveById(message.getPersonaId());
        String personaName = persona == null ? "persona#" + message.getPersonaId() : safe(persona.getDisplayName());
        return safe(message.getRole()) + "(" + personaName + ")";
    }

    private String truncate(String value, int maxLength) {
        String safeValue = safe(value).replaceAll("\\s+", " ").trim();
        if (safeValue.length() <= maxLength) {
            return safeValue;
        }
        return safeValue.substring(0, maxLength - 3) + "...";
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

    private String personaBatchPrompt(List<DebateMessageRequest> requests) {
        StringBuilder builder = new StringBuilder();
        for (DebateMessageRequest request : requests) {
            PersonaRecord persona = personaMapper.findActiveById(request.getPersonaId());
            String personaPrompt = persona == null ? "Respond as a literary discussion participant." : persona.getSystemPrompt();
            builder.append("- personaId: ")
                .append(request.getPersonaId())
                .append("\n  prompt: ")
                .append(personaPrompt)
                .append('\n');
        }
        return builder.toString();
    }

    private List<AiMessageResponse> parseDebateResponses(Long windowId, List<DebateMessageRequest> requests, String output) {
        java.util.Map<Long, DebateMessageRequest> requestByPersonaId = requests.stream()
            .collect(java.util.stream.Collectors.toMap(
                DebateMessageRequest::getPersonaId,
                (request) -> request,
                (left, right) -> left,
                java.util.LinkedHashMap::new
            ));
        java.util.Map<Long, String> contentByPersonaId = new java.util.LinkedHashMap<>();
        JsonNode root = parseJsonArray(output);
        for (JsonNode item : root) {
            Long personaId = item.path("personaId").canConvertToLong() ? item.path("personaId").asLong() : null;
            String content = item.path("content").asText("");
            if (personaId != null && requestByPersonaId.containsKey(personaId) && !content.isBlank()) {
                contentByPersonaId.put(personaId, content.trim());
            }
        }

        return requests.stream()
            .filter((request) -> contentByPersonaId.containsKey(request.getPersonaId()))
            .map((request) -> AiMessageResponse.builder()
                .windowId(windowId)
                .role("assistant")
                .personaId(request.getPersonaId())
                .content(contentByPersonaId.get(request.getPersonaId()))
                .streamingReady(true)
                .aiModel(properties.getModel())
                .build())
            .toList();
    }

    private List<AiMessageResponse> fillMissingDebateResponses(Long windowId, List<DebateMessageRequest> requests, List<AiMessageResponse> responses) {
        java.util.Map<Long, AiMessageResponse> responseByPersonaId = responses.stream()
            .filter((response) -> response.getPersonaId() != null)
            .collect(java.util.stream.Collectors.toMap(
                AiMessageResponse::getPersonaId,
                (response) -> response,
                (left, right) -> left,
                java.util.LinkedHashMap::new
            ));

        List<DebateMessageRequest> missingRequests = requests.stream()
            .filter((request) -> !responseByPersonaId.containsKey(request.getPersonaId()))
            .toList();
        if (!missingRequests.isEmpty()) {
            AiProvider.super.answerDebateMessages(windowId, missingRequests)
                .forEach((response) -> responseByPersonaId.putIfAbsent(response.getPersonaId(), response));
        }

        return requests.stream()
            .map((request) -> responseByPersonaId.get(request.getPersonaId()))
            .filter(java.util.Objects::nonNull)
            .toList();
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

    private void logOpenAiFallback(String operation, RuntimeException exception) {
        log.warn(
            "OpenAI {} failed; using local fallback. model={}, baseUrl={}, reason={}",
            operation,
            properties.getModel(),
            properties.getBaseUrl(),
            exception.getMessage()
        );
    }

    private String summarizeForLog(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.length() <= 500 ? compact : compact.substring(0, 500) + "...";
    }
}
