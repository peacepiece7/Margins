package com.margins.session.business;

import com.margins.ai.AiProvider;
import com.margins.message.mapper.MessageMapper;
import com.margins.message.model.MessageRecord;
import com.margins.persona.mapper.PersonaMapper;
import com.margins.persona.model.PersonaRecord;
import com.margins.question.dto.CreateQuestionRequest;
import com.margins.question.dto.GenerateQuestionsRequest;
import com.margins.question.dto.QuestionDto;
import com.margins.question.dto.QuestionListResponse;
import com.margins.question.mapper.QuestionMapper;
import com.margins.question.model.QuestionRecord;
import com.margins.session.dto.AiMessageResponse;
import com.margins.session.dto.AiMessageListResponse;
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
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SessionWindowBusiness {

    private static final long DEFAULT_USER_ID = 1L;
    private static final String OPEN_STATUS = "open";
    private static final String ACTIVE_STATUS = "active";
    private static final String READER_QUESTION_TYPE = "reader";
    private static final String COMPLETE_STREAMING_STATUS = "complete";
    private static final int PROMPT_SNAPSHOT_SCHEMA_VERSION = 1;
    private static final String PROMPT_CONTRACT_VERSION = "mvp-answer-v2";
    private static final String SAFETY_POLICY_VERSION = "mvp-safety-v1";
    private static final String GROUNDING_POLICY_VERSION = "mvp-grounding-v1";
    private static final String READING_BOUNDARY_POLICY_VERSION = "mvp-reading-boundary-v1";

    private final AiProvider aiProvider;
    private final SessionWindowMapper sessionWindowMapper;
    private final MessageMapper messageMapper;
    private final QuestionMapper questionMapper;
    private final PersonaMapper personaMapper;
    private final SessionHighlightMapper sessionHighlightMapper;

    @Autowired
    public SessionWindowBusiness(
        AiProvider aiProvider,
        SessionWindowMapper sessionWindowMapper,
        MessageMapper messageMapper,
        QuestionMapper questionMapper,
        PersonaMapper personaMapper,
        SessionHighlightMapper sessionHighlightMapper
    ) {
        this.aiProvider = aiProvider;
        this.sessionWindowMapper = sessionWindowMapper;
        this.messageMapper = messageMapper;
        this.questionMapper = questionMapper;
        this.personaMapper = personaMapper;
        this.sessionHighlightMapper = sessionHighlightMapper;
    }

    public SessionWindowBusiness(
        AiProvider aiProvider,
        SessionWindowMapper sessionWindowMapper,
        MessageMapper messageMapper,
        QuestionMapper questionMapper,
        PersonaMapper personaMapper
    ) {
        this(aiProvider, sessionWindowMapper, messageMapper, questionMapper, personaMapper, null);
    }

    public CreateSessionWindowResponse create(CreateSessionWindowRequest request) {
        if (sessionWindowMapper.countActiveSessionById(request.getSessionId(), DEFAULT_USER_ID) <= 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reading session not found");
        }

        SessionWindowRecord record = SessionWindowRecord.builder()
            .sessionId(request.getSessionId())
            .userId(DEFAULT_USER_ID)
            .windowType(request.getWindowType())
            .title(request.getTitle())
            .position(sessionWindowMapper.selectNextPosition(request.getSessionId()))
            .status(OPEN_STATUS)
            .testData(true)
            .build();

        requireInserted(sessionWindowMapper.insert(record), "Session window could not be saved");

        return CreateSessionWindowResponse.builder()
            .windowId(record.getId())
            .sessionId(record.getSessionId())
            .windowType(record.getWindowType())
            .title(record.getTitle())
            .status(record.getStatus())
            .build();
    }

    public CreateSessionWindowResponse updateTitle(Long windowId, UpdateSessionWindowTitleRequest request) {
        requireWindowContext(windowId);
        requireUpdated(sessionWindowMapper.updateTitle(windowId, request.getTitle()), "Session window not found");
        SessionWindowRecord record = sessionWindowMapper.findById(windowId);

        return CreateSessionWindowResponse.builder()
            .windowId(record.getId())
            .sessionId(record.getSessionId())
            .windowType(record.getWindowType())
            .title(record.getTitle())
            .status(record.getStatus())
            .build();
    }

    public CreateSessionWindowResponse archive(Long windowId) {
        SessionWindowContext context = requireWindowContext(windowId);
        SessionWindowRecord record = sessionWindowMapper.findById(windowId);
        if (sessionWindowMapper.countActiveBySessionId(context.getSessionId()) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "At least one session window must remain");
        }
        requireUpdated(sessionWindowMapper.softDelete(windowId), "Session window not found");

        return CreateSessionWindowResponse.builder()
            .windowId(record.getId())
            .sessionId(record.getSessionId())
            .windowType(record.getWindowType())
            .title(record.getTitle())
            .status("archived")
            .build();
    }

    public AiMessageResponse sendMessage(Long windowId, SendMessageRequest request) {
        SessionWindowContext context = requireWindowContext(windowId);
        validateQuestionForWindow(request.getQuestionId(), windowId, context);
        Long userId = resolveUserId(request.getUserId(), context);
        MessageRecord userMessage = insertMessage(MessageRecord.builder()
            .sessionId(context.getSessionId())
            .windowId(windowId)
            .userId(userId)
            .role("user")
            .content(request.getContent())
            .questionId(request.getQuestionId())
            .streamingStatus(COMPLETE_STREAMING_STATUS)
            .testData(true)
            .build());

        AiMessageResponse aiResponse = aiProvider.answerWindowMessage(windowId, request);
        String contextSnapshot = buildContextSnapshot(context, windowId, request.getQuestionId(), null, userMessage);
        String promptSnapshot = buildPromptSnapshot(aiResponse, "book_answer", false);
        MessageRecord aiMessage = insertMessage(MessageRecord.builder()
            .sessionId(context.getSessionId())
            .windowId(windowId)
            .userId(userId)
            .parentMessageId(userMessage.getId())
            .role(aiResponse.getRole())
            .content(aiResponse.getContent())
            .aiModel(aiResponse.getAiModel())
            .questionId(request.getQuestionId())
            .promptSnapshot(promptSnapshot)
            .contextSnapshot(contextSnapshot)
            .tokenUsage(aiResponse.getTokenUsage())
            .streamingStatus(COMPLETE_STREAMING_STATUS)
            .testData(true)
            .build());

        return copyWithPersistedMessageId(aiResponse, aiMessage.getId(), promptSnapshot, contextSnapshot);
    }

    public AiMessageResponse streamMessage(Long windowId, SendMessageRequest request, Consumer<String> deltaConsumer) {
        SessionWindowContext context = requireWindowContext(windowId);
        validateQuestionForWindow(request.getQuestionId(), windowId, context);
        Long userId = resolveUserId(request.getUserId(), context);
        MessageRecord userMessage = insertMessage(MessageRecord.builder()
            .sessionId(context.getSessionId())
            .windowId(windowId)
            .userId(userId)
            .role("user")
            .content(request.getContent())
            .questionId(request.getQuestionId())
            .streamingStatus(COMPLETE_STREAMING_STATUS)
            .testData(true)
            .build());

        AiMessageResponse aiResponse = aiProvider.streamWindowMessage(windowId, request, deltaConsumer);
        String contextSnapshot = buildContextSnapshot(context, windowId, request.getQuestionId(), null, userMessage);
        String promptSnapshot = buildPromptSnapshot(aiResponse, "book_answer", true);
        MessageRecord aiMessage = insertMessage(MessageRecord.builder()
            .sessionId(context.getSessionId())
            .windowId(windowId)
            .userId(userId)
            .parentMessageId(userMessage.getId())
            .role(aiResponse.getRole())
            .content(aiResponse.getContent())
            .aiModel(aiResponse.getAiModel())
            .questionId(request.getQuestionId())
            .promptSnapshot(promptSnapshot)
            .contextSnapshot(contextSnapshot)
            .tokenUsage(aiResponse.getTokenUsage())
            .streamingStatus(COMPLETE_STREAMING_STATUS)
            .testData(true)
            .build());

        return copyWithPersistedMessageId(aiResponse, aiMessage.getId(), promptSnapshot, contextSnapshot);
    }

    public QuestionListResponse questions(Long windowId) {
        requireWindowContext(windowId);

        return QuestionListResponse.builder()
            .questions(questionMapper.findByWindowId(windowId).stream().map(this::toQuestionDto).toList())
            .build();
    }

    public QuestionListResponse createQuestion(Long windowId, CreateQuestionRequest request) {
        SessionWindowContext context = requireWindowContext(windowId);
        QuestionDto question = QuestionDto.builder()
            .questionText(request.getQuestionText())
            .questionType(READER_QUESTION_TYPE)
            .status(ACTIVE_STATUS)
            .build();

        insertQuestion(context, windowId, question);
        return questions(windowId);
    }

    public QuestionListResponse generateQuestions(Long windowId, GenerateQuestionsRequest request) {
        SessionWindowContext context = requireWindowContext(windowId);
        QuestionListResponse suggestions = aiProvider.suggestQuestions(windowId, request);

        return QuestionListResponse.builder()
            .questions(suggestions.getQuestions().stream()
                .map((suggestion) -> insertQuestion(context, windowId, suggestion))
                .toList())
            .build();
    }

    public QuestionListResponse suggestQuestions(Long windowId, GenerateQuestionsRequest request) {
        requireWindowContext(windowId);
        return aiProvider.suggestQuestions(windowId, request);
    }

    public QuestionDto deleteQuestion(Long questionId) {
        QuestionRecord record = requireDeletableQuestion(questionId);
        requireUpdated(questionMapper.softDelete(questionId, DEFAULT_USER_ID), "Question not found");
        return toQuestionDto(record);
    }

    public AiMessageResponse debate(Long windowId, DebateMessageRequest request) {
        SessionWindowContext context = requireWindowContext(windowId);
        PersonaRecord persona = requireActivePersona(request.getPersonaId());
        Long userId = resolveUserId(request.getUserId(), context);
        MessageRecord userMessage = insertMessage(MessageRecord.builder()
            .sessionId(context.getSessionId())
            .windowId(windowId)
            .userId(userId)
            .role("user")
            .content(request.getContent())
            .streamingStatus(COMPLETE_STREAMING_STATUS)
            .testData(true)
            .build());

        AiMessageResponse aiResponse = aiProvider.answerDebateMessage(windowId, request);
        String contextSnapshot = buildContextSnapshot(context, windowId, null, persona, userMessage);
        String promptSnapshot = buildPromptSnapshot(aiResponse, "persona_debate", false);
        MessageRecord aiMessage = insertMessage(MessageRecord.builder()
            .sessionId(context.getSessionId())
            .windowId(windowId)
            .userId(userId)
            .parentMessageId(userMessage.getId())
            .role(aiResponse.getRole())
            .content(aiResponse.getContent())
            .aiModel(aiResponse.getAiModel())
            .personaId(request.getPersonaId())
            .promptSnapshot(promptSnapshot)
            .contextSnapshot(contextSnapshot)
            .tokenUsage(aiResponse.getTokenUsage())
            .streamingStatus(COMPLETE_STREAMING_STATUS)
            .testData(true)
            .build());

        return copyWithPersistedMessageId(aiResponse, aiMessage.getId(), promptSnapshot, contextSnapshot);
    }

    public AiMessageListResponse debateAll(Long windowId, DebateAllMessageRequest request) {
        SessionWindowContext context = requireWindowContext(windowId);
        Long userId = resolveUserId(request.getUserId(), context);
        MessageRecord userMessage = insertMessage(MessageRecord.builder()
            .sessionId(context.getSessionId())
            .windowId(windowId)
            .userId(userId)
            .role("user")
            .content(request.getContent())
            .streamingStatus(COMPLETE_STREAMING_STATUS)
            .testData(true)
            .build());

        return AiMessageListResponse.builder()
            .messages(personaMapper.findActiveForSession(context.getSessionId()).stream()
                .map((persona) -> insertPersonaDebateResponse(windowId, context, userId, userMessage, persona))
                .toList())
            .build();
    }

    private SessionWindowContext requireWindowContext(Long windowId) {
        SessionWindowContext context = sessionWindowMapper.findContextById(windowId);
        if (context == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session window not found");
        }
        return context;
    }

    private Long resolveUserId(Long ignoredRequestUserId, SessionWindowContext context) {
        if (context.getUserId() != null) {
            return context.getUserId();
        }
        return DEFAULT_USER_ID;
    }

    private void validateQuestionForWindow(Long questionId, Long windowId, SessionWindowContext context) {
        if (questionId == null) {
            return;
        }

        QuestionRecord question = questionMapper.findActiveById(questionId, resolveUserId(null, context));
        if (question == null
            || !context.getSessionId().equals(question.getSessionId())
            || !windowId.equals(question.getWindowId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found for session window");
        }
    }

    private PersonaRecord requireActivePersona(Long personaId) {
        PersonaRecord persona = personaMapper.findActiveById(personaId);
        if (persona == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Persona not found");
        }
        return persona;
    }

    private MessageRecord insertMessage(MessageRecord record) {
        record.setMessageOrder(messageMapper.selectNextOrder(record.getSessionId(), record.getWindowId()));
        requireInserted(messageMapper.insert(record), "Message could not be saved");
        return record;
    }

    private AiMessageResponse copyWithPersistedMessageId(AiMessageResponse response, Long messageId, String promptSnapshot, String contextSnapshot) {
        return AiMessageResponse.builder()
            .messageId(messageId)
            .windowId(response.getWindowId())
            .personaId(response.getPersonaId())
            .role(response.getRole())
            .content(response.getContent())
            .streamingReady(response.isStreamingReady())
            .aiModel(response.getAiModel())
            .promptSnapshot(promptSnapshot)
            .contextSnapshot(contextSnapshot)
            .tokenUsage(response.getTokenUsage())
            .build();
    }

    private AiMessageResponse insertPersonaDebateResponse(
        Long windowId,
        SessionWindowContext context,
        Long userId,
        MessageRecord userMessage,
        PersonaRecord persona
    ) {
        DebateMessageRequest personaRequest = DebateMessageRequest.builder()
            .personaId(persona.getId())
            .content(userMessage.getContent())
            .build();
        AiMessageResponse aiResponse = aiProvider.answerDebateMessage(windowId, personaRequest);
        String contextSnapshot = buildContextSnapshot(context, windowId, null, persona, userMessage);
        String promptSnapshot = buildPromptSnapshot(aiResponse, "persona_debate", false);
        MessageRecord aiMessage = insertMessage(MessageRecord.builder()
            .sessionId(context.getSessionId())
            .windowId(windowId)
            .userId(userId)
            .parentMessageId(userMessage.getId())
            .role(aiResponse.getRole())
            .content(aiResponse.getContent())
            .aiModel(aiResponse.getAiModel())
            .personaId(persona.getId())
            .promptSnapshot(promptSnapshot)
            .contextSnapshot(contextSnapshot)
            .tokenUsage(aiResponse.getTokenUsage())
            .streamingStatus(COMPLETE_STREAMING_STATUS)
            .testData(true)
            .build());

        return copyWithPersistedMessageId(aiResponse, aiMessage.getId(), promptSnapshot, contextSnapshot);
    }

    private String buildPromptSnapshot(AiMessageResponse response, String responseType, boolean streaming) {
        String aiModel = response.getAiModel() == null ? "" : response.getAiModel();
        return "{"
            + "\"schemaVersion\":" + PROMPT_SNAPSHOT_SCHEMA_VERSION + ","
            + "\"promptContractVersion\":\"" + PROMPT_CONTRACT_VERSION + "\","
            + "\"responseType\":\"" + jsonEscape(responseType) + "\","
            + "\"provider\":\"" + promptProvider(aiModel) + "\","
            + "\"aiModel\":\"" + jsonEscape(aiModel) + "\","
            + "\"streaming\":" + streaming + ","
            + "\"safetyPolicyVersion\":\"" + SAFETY_POLICY_VERSION + "\","
            + "\"groundingPolicyVersion\":\"" + GROUNDING_POLICY_VERSION + "\","
            + "\"readingBoundaryPolicyVersion\":\"" + READING_BOUNDARY_POLICY_VERSION + "\""
            + "}";
    }

    private String promptProvider(String aiModel) {
        return "placeholder".equalsIgnoreCase(aiModel) ? "placeholder" : "openai";
    }

    private QuestionDto insertQuestion(SessionWindowContext context, Long windowId, QuestionDto suggestion) {
        QuestionRecord record = QuestionRecord.builder()
            .sessionId(context.getSessionId())
            .windowId(windowId)
            .userId(resolveUserId(null, context))
            .questionText(suggestion.getQuestionText())
            .questionType(suggestion.getQuestionType() == null ? "reflection" : suggestion.getQuestionType())
            .status(ACTIVE_STATUS)
            .aiModel(suggestion.getAiModel())
            .testData(true)
            .build();

        requireInserted(questionMapper.insert(record), "Question could not be saved");
        return toQuestionDto(record);
    }

    private String buildContextSnapshot(
        SessionWindowContext context,
        Long windowId,
        Long questionId,
        PersonaRecord persona,
        MessageRecord userMessage
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append('{')
            .append("\"schemaVersion\":1,")
            .append("\"sessionId\":").append(context.getSessionId()).append(',')
            .append("\"windowId\":").append(windowId).append(',')
            .append("\"references\":{");
        appendQuestionReference(builder, questionId, context);
        builder.append(',');
        appendPersonaReference(builder, persona);
        builder.append(',');
        appendMessageReferences(builder, context.getSessionId(), userMessage);
        builder.append(',');
        appendHighlightReferences(builder, context.getSessionId());
        builder.append("}}");
        return builder.toString();
    }

    private void appendQuestionReference(StringBuilder builder, Long questionId, SessionWindowContext context) {
        builder.append("\"question\":");
        if (questionId == null) {
            builder.append("null");
            return;
        }

        QuestionRecord question = questionMapper.findActiveById(questionId, resolveUserId(null, context));
        if (question == null) {
            builder.append("null");
            return;
        }

        builder.append('{')
            .append("\"id\":").append(question.getId()).append(',')
            .append("\"label\":\"Question #").append(question.getId()).append("\",")
            .append("\"text\":\"").append(jsonEscape(excerpt(question.getQuestionText(), 240))).append("\"")
            .append('}');
    }

    private void appendPersonaReference(StringBuilder builder, PersonaRecord persona) {
        builder.append("\"persona\":");
        if (persona == null) {
            builder.append("null");
            return;
        }

        builder.append('{')
            .append("\"id\":").append(persona.getId()).append(',')
            .append("\"label\":\"").append(jsonEscape(persona.getDisplayName())).append("\",")
            .append("\"text\":\"").append(jsonEscape(excerpt(persona.getSystemPrompt(), 240))).append("\"")
            .append('}');
    }

    private void appendMessageReferences(StringBuilder builder, Long sessionId, MessageRecord userMessage) {
        builder.append("\"messages\":[");
        List<MessageRecord> messages = new ArrayList<>(messageMapper.findBySessionId(sessionId));
        if (messages.isEmpty() && userMessage != null) {
            messages.add(userMessage);
        }
        List<MessageRecord> recent = messages.stream()
            .skip(Math.max(0, messages.size() - 6))
            .toList();
        for (int index = 0; index < recent.size(); index++) {
            MessageRecord message = recent.get(index);
            if (index > 0) {
                builder.append(',');
            }
            builder.append('{')
                .append("\"id\":").append(message.getId() == null ? "null" : message.getId()).append(',')
                .append("\"label\":\"").append(jsonEscape(message.getRole())).append("\",")
                .append("\"text\":\"").append(jsonEscape(excerpt(message.getContent(), 240))).append("\"");
            if (message.getQuestionId() != null) {
                builder.append(",\"questionId\":").append(message.getQuestionId());
            }
            if (message.getPersonaId() != null) {
                builder.append(",\"personaId\":").append(message.getPersonaId());
            }
            builder.append('}');
        }
        builder.append(']');
    }

    private void appendHighlightReferences(StringBuilder builder, Long sessionId) {
        builder.append("\"highlights\":[");
        if (sessionHighlightMapper == null) {
            builder.append(']');
            return;
        }

        List<SessionHighlightRecord> highlights = sessionHighlightMapper.findBySessionId(sessionId).stream()
            .limit(3)
            .toList();
        for (int index = 0; index < highlights.size(); index++) {
            SessionHighlightRecord highlight = highlights.get(index);
            if (index > 0) {
                builder.append(',');
            }
            builder.append('{')
                .append("\"id\":").append(highlight.getId() == null ? "null" : highlight.getId()).append(',')
                .append("\"label\":\"").append(jsonEscape(highlightLabel(highlight))).append("\",")
                .append("\"text\":\"").append(jsonEscape(excerpt(highlight.getQuoteText(), 240))).append("\"");
            if (highlight.getPageNumber() != null) {
                builder.append(",\"pageNumber\":").append(highlight.getPageNumber());
            }
            builder.append('}');
        }
        builder.append(']');
    }

    private String highlightLabel(SessionHighlightRecord highlight) {
        if (highlight.getPageNumber() != null) {
            return "Quote p. " + highlight.getPageNumber();
        }
        if (highlight.getLocationLabel() != null && !highlight.getLocationLabel().isBlank()) {
            return "Quote " + highlight.getLocationLabel();
        }
        return "Quote";
    }

    private String excerpt(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength - 3) + "...";
    }

    private String jsonEscape(String value) {
        String safe = value == null ? "" : value;
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < safe.length(); index++) {
            char character = safe.charAt(index);
            if (character == '"' || character == '\\') {
                builder.append('\\').append(character);
            } else if (character == '\n') {
                builder.append("\\n");
            } else if (character == '\r') {
                builder.append("\\r");
            } else if (character == '\t') {
                builder.append("\\t");
            } else if (character < 0x20) {
                builder.append(String.format("\\u%04x", (int) character));
            } else {
                builder.append(character);
            }
        }
        return builder.toString();
    }

    private QuestionRecord requireDeletableQuestion(Long questionId) {
        QuestionRecord record = questionMapper.findActiveById(questionId, DEFAULT_USER_ID);
        if (record == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");
        }
        if (questionMapper.countActiveUserAnswers(questionId) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Answered question cannot be deleted");
        }
        return record;
    }

    private void requireUpdated(int updatedRows, String reason) {
        if (updatedRows <= 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, reason);
        }
    }

    private void requireInserted(int insertedRows, String reason) {
        if (insertedRows <= 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, reason);
        }
    }

    private QuestionDto toQuestionDto(QuestionRecord record) {
        return QuestionDto.builder()
            .questionId(record.getId())
            .sessionId(record.getSessionId())
            .windowId(record.getWindowId())
            .questionText(record.getQuestionText())
            .questionType(record.getQuestionType())
            .status(record.getStatus())
            .aiModel(record.getAiModel())
            .build();
    }
}
