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
import com.margins.persona.dto.GeneratePersonasRequest;
import com.margins.persona.dto.PersonaDraftDto;
import com.margins.persona.dto.PersonaDraftListResponse;
import com.margins.persona.model.PersonaRecord;
import com.margins.persona.model.PersonaRoleCatalog;
import com.margins.question.dto.GenerateQuestionsRequest;
import com.margins.question.dto.QuestionDto;
import com.margins.question.dto.QuestionListResponse;
import com.margins.question.mapper.QuestionMapper;
import com.margins.question.model.QuestionRecord;
import com.margins.session.dto.AiMessageResponse;
import com.margins.session.dto.DebateMessageRequest;
import com.margins.session.dto.SendMessageRequest;
import com.margins.session.mapper.SessionWindowMapper;
import com.margins.session.mapper.SessionHighlightMapper;
import com.margins.session.mapper.SessionInsightMapper;
import com.margins.session.model.SessionHighlightRecord;
import com.margins.session.model.SessionInsightRecord;
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
    private final SessionHighlightMapper sessionHighlightMapper;
    private final SessionInsightMapper sessionInsightMapper;
    private final AiSafetyPolicy aiSafetyPolicy;
    private final AiAnswerQualityPolicy aiAnswerQualityPolicy;
    private final PlaceholderAiProvider fallback = new PlaceholderAiProvider();

    private record TextResult(String text, String tokenUsage) {
    }

    @Override
    public BookCandidateSearchResponse suggestBooks(String query) {
        if (!configured()) {
            return fallback.suggestBooks(query);
        }

        try {
            String output = createText(
                withSafety("Return exactly three likely book candidates as JSON array. Each item must have title, author, and reason."),
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
    public PersonaDraftListResponse suggestPersonas(GeneratePersonasRequest request) {
        if (!configured()) {
            return fallback.suggestPersonas(request);
        }

        try {
            String output = createText(
                withSafety("Generate concise debate persona drafts for a reading-record app. Return a JSON array of objects with displayName, description, tone, roleKey, systemPrompt, and reason. Use distinct roleKey values from this fixed set when possible: "
                    + String.join(", ", PersonaRoleCatalog.defaultOrder()) + "."),
                "Book title: " + safe(request.getBookTitle())
                    + "\nReading goal: " + safe(request.getReadingGoal())
                    + "\nContext: " + safe(request.getContext())
                    + "\nCount: " + (request.getCount() == null ? 3 : request.getCount())
            );
            List<PersonaDraftDto> personas = parsePersonaDrafts(output);
            if (personas.isEmpty()) {
                return fallback.suggestPersonas(request);
            }

            return PersonaDraftListResponse.builder()
                .aiModel(properties.getModel())
                .personas(personas)
                .build();
        } catch (RuntimeException exception) {
            return fallback.suggestPersonas(request);
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
                withSafety("Generate concise reading reflection questions. Return a JSON array of objects with questionText and questionType."),
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
            TextResult result = createTextResult(
                withGrounding("Respond as a precise reading companion. Use the selected question and prior session context. Keep the answer under 140 words."),
                contextForWindow(windowId) + "\nSelected question id: " + request.getQuestionId() + "\nReader answer: " + request.getContent()
            );

            return AiMessageResponse.builder()
                .windowId(windowId)
                .role("assistant")
                .content(aiAnswerQualityPolicy.ensureSections(result.text()))
                .streamingReady(true)
                .aiModel(properties.getModel())
                .tokenUsage(result.tokenUsage())
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
            TextResult result = createTextStream(
                withGrounding("Respond as a precise reading companion. Use the selected question and prior session context. Keep the answer under 140 words."),
                contextForWindow(windowId) + "\nSelected question id: " + request.getQuestionId() + "\nReader answer: " + request.getContent(),
                (delta) -> {
                    emittedProviderDelta.set(true);
                    deltaConsumer.accept(delta);
                }
            );

            return AiMessageResponse.builder()
                .windowId(windowId)
                .role("assistant")
                .content(aiAnswerQualityPolicy.ensureSections(result.text()))
                .streamingReady(true)
                .aiModel(properties.getModel())
                .tokenUsage(result.tokenUsage())
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
            TextResult result = createTextResult(
                withGrounding(personaPrompt + "\nChallenge or extend the reader's interpretation. Keep the answer under 140 words."),
                contextForWindow(windowId) + "\nReader debate message: " + request.getContent()
            );

            return AiMessageResponse.builder()
                .windowId(windowId)
                .role("assistant")
                .personaId(request.getPersonaId())
                .content(aiAnswerQualityPolicy.ensureSections(result.text()))
                .streamingReady(true)
                .aiModel(properties.getModel())
                .tokenUsage(result.tokenUsage())
                .build();
        } catch (RuntimeException exception) {
            return fallback.answerDebateMessage(windowId, request);
        }
    }

    private boolean configured() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    private String withSafety(String instructions) {
        return instructions + "\n\n" + aiSafetyPolicy.instructions();
    }

    private String withGrounding(String instructions) {
        return withSafety(instructions + "\n\n" + """
            Response grounding contract:
            - Ground the reply in the provided session context, reader quotes, notes, messages, or selected question.
            - Name the supporting quote, note, message, or question when the context makes that possible.
            - If the provided context is insufficient, say what is uncertain instead of inventing book details.
            - Respect the reading boundary and avoid claims beyond the recorded or reader-provided context.
            """ + "\n" + aiAnswerQualityPolicy.instructions());
    }

    private String createText(String instructions, String input) {
        return createTextResult(instructions, input).text();
    }

    private TextResult createTextResult(String instructions, String input) {
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

            JsonNode responseRoot = objectMapper.readTree(response.body());
            String text = extractOutputText(responseRoot);
            if (text.isBlank()) {
                throw new IllegalStateException("OpenAI response did not include text output");
            }
            return new TextResult(text.trim(), extractUsageJson(responseRoot));
        } catch (IOException exception) {
            throw new IllegalStateException("OpenAI response could not be parsed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenAI request interrupted", exception);
        }
    }

    private TextResult createTextStream(String instructions, String input, Consumer<String> deltaConsumer) {
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

            TextResult result = readStreamedOutput(response.body(), deltaConsumer);
            if (result.text().isBlank()) {
                throw new IllegalStateException("OpenAI stream did not include text output");
            }
            return new TextResult(result.text().trim(), result.tokenUsage());
        } catch (IOException exception) {
            throw new IllegalStateException("OpenAI stream could not be parsed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenAI stream request interrupted", exception);
        }
    }

    private TextResult readStreamedOutput(InputStream inputStream, Consumer<String> deltaConsumer) throws IOException {
        StringBuilder output = new StringBuilder();
        StringBuilder eventData = new StringBuilder();
        StringBuilder tokenUsage = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    appendStreamEvent(eventData.toString(), output, tokenUsage, deltaConsumer);
                    eventData.setLength(0);
                } else if (line.startsWith("data:")) {
                    if (!eventData.isEmpty()) {
                        eventData.append('\n');
                    }
                    eventData.append(line.substring(5).trim());
                }
            }
        }
        appendStreamEvent(eventData.toString(), output, tokenUsage, deltaConsumer);
        return new TextResult(output.toString(), tokenUsage.isEmpty() ? null : tokenUsage.toString());
    }

    private void appendStreamEvent(String data, StringBuilder output, StringBuilder tokenUsage, Consumer<String> deltaConsumer) throws IOException {
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
        String usage = extractUsageJson(event);
        if (usage != null && tokenUsage.isEmpty()) {
            tokenUsage.append(usage);
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
        appendIfPresent(builder, "Book title", context.getBookTitle());
        appendIfPresent(builder, "Book author", context.getBookAuthor());
        appendIfPresent(builder, "Session title", context.getSessionTitle());
        appendIfPresent(builder, "Reading goal", context.getReadingGoal());
        appendProgressBoundary(builder, context);
        appendIfPresent(builder, "Progress note", context.getProgressNote());
        appendIfPresent(builder, "Session summary", context.getSummary());

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

        List<SessionHighlightRecord> highlights = sessionHighlightMapper.findBySessionId(context.getSessionId());
        if (!highlights.isEmpty()) {
            builder.append("Reader quotes and notes:\n");
            highlights.stream().limit(5).forEach((highlight) -> {
                builder.append("- ");
                if (highlight.getPageNumber() != null) {
                    builder.append("p. ").append(highlight.getPageNumber()).append(": ");
                }
                if (highlight.getLocationLabel() != null && !highlight.getLocationLabel().isBlank()) {
                    builder.append(highlight.getLocationLabel()).append(": ");
                }
                builder.append(highlight.getQuoteText());
                if (highlight.getNote() != null && !highlight.getNote().isBlank()) {
                    builder.append(" Note: ").append(highlight.getNote());
                }
                builder.append('\n');
            });
        }

        List<SessionInsightRecord> insights = sessionInsightMapper.findBySessionId(context.getSessionId(), context.getUserId());
        if (!insights.isEmpty()) {
            builder.append("Reader summaries and insights:\n");
            insights.stream().limit(5).forEach((insight) -> builder
                .append("- ")
                .append(insight.getInsightType())
                .append(": ")
                .append(insight.getTitle() == null || insight.getTitle().isBlank() ? "" : insight.getTitle() + " - ")
                .append(insight.getContent())
                .append(insight.getEvidence() == null || insight.getEvidence().isBlank() ? "" : " Evidence: " + insight.getEvidence())
                .append('\n'));
        }
        return builder.toString();
    }

    private void appendIfPresent(StringBuilder builder, String label, String value) {
        if (value != null && !value.isBlank()) {
            builder.append(label).append(": ").append(value).append('\n');
        }
    }

    private void appendProgressBoundary(StringBuilder builder, SessionWindowContext context) {
        if (context.getCurrentPage() == null && context.getTargetPage() == null && context.getStartPage() == null) {
            builder.append("Reading boundary: no current reading position is recorded. Avoid plot events or claims beyond reader-provided context.\n");
            return;
        }

        builder.append("Reading boundary: ");
        if (context.getStartPage() != null) {
            builder.append("start page ").append(context.getStartPage()).append("; ");
        }
        if (context.getCurrentPage() != null) {
            builder.append("current page ").append(context.getCurrentPage()).append("; ");
        }
        if (context.getTargetPage() != null) {
            builder.append("target page ").append(context.getTargetPage()).append("; ");
        }
        builder.append("do not reveal or assume content beyond the current page unless the reader already provided it in notes or messages.\n");
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

    private List<PersonaDraftDto> parsePersonaDrafts(String output) {
        List<PersonaDraftDto> personas = new ArrayList<>();
        JsonNode root = parseJsonArray(output);
        for (int index = 0; index < root.size(); index++) {
            JsonNode item = root.get(index);
            String displayName = item.path("displayName").asText("");
            String systemPrompt = item.path("systemPrompt").asText("");
            if (!displayName.isBlank() && !systemPrompt.isBlank()) {
                personas.add(PersonaDraftDto.builder()
                    .displayName(displayName)
                    .description(item.path("description").asText(""))
                    .tone(item.path("tone").asText(""))
                    .roleKey(item.path("roleKey").asText(""))
                    .systemPrompt(systemPrompt)
                    .reason(item.path("reason").asText(""))
                    .build());
            }
        }
        return personas;
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

    private String extractUsageJson(JsonNode root) {
        JsonNode usage = findUsageNode(root);
        if (usage == null || usage.isMissingNode() || usage.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(usage);
        } catch (IOException exception) {
            return null;
        }
    }

    private JsonNode findUsageNode(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.hasNonNull("usage")) {
            return node.path("usage");
        }
        if (node.hasNonNull("response") && node.path("response").hasNonNull("usage")) {
            return node.path("response").path("usage");
        }
        return null;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
