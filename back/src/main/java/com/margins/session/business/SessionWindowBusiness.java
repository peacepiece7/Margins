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
import com.margins.session.mapper.SessionWindowMapper;
import com.margins.session.model.SessionWindowContext;
import com.margins.session.model.SessionWindowRecord;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class SessionWindowBusiness {

    private static final long DEFAULT_USER_ID = 1L;
    private static final String OPEN_STATUS = "open";
    private static final String ACTIVE_STATUS = "active";
    private static final String READER_QUESTION_TYPE = "reader";
    private static final String COMPLETE_STREAMING_STATUS = "complete";

    private final AiProvider aiProvider;
    private final SessionWindowMapper sessionWindowMapper;
    private final MessageMapper messageMapper;
    private final QuestionMapper questionMapper;
    private final PersonaMapper personaMapper;

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
        MessageRecord aiMessage = insertMessage(MessageRecord.builder()
            .sessionId(context.getSessionId())
            .windowId(windowId)
            .userId(userId)
            .parentMessageId(userMessage.getId())
            .role(aiResponse.getRole())
            .content(aiResponse.getContent())
            .aiModel(aiResponse.getAiModel())
            .questionId(request.getQuestionId())
            .streamingStatus(COMPLETE_STREAMING_STATUS)
            .testData(true)
            .build());

        return copyWithPersistedMessageId(aiResponse, aiMessage.getId());
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
        MessageRecord aiMessage = insertMessage(MessageRecord.builder()
            .sessionId(context.getSessionId())
            .windowId(windowId)
            .userId(userId)
            .parentMessageId(userMessage.getId())
            .role(aiResponse.getRole())
            .content(aiResponse.getContent())
            .aiModel(aiResponse.getAiModel())
            .questionId(request.getQuestionId())
            .streamingStatus(COMPLETE_STREAMING_STATUS)
            .testData(true)
            .build());

        return copyWithPersistedMessageId(aiResponse, aiMessage.getId());
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

    public QuestionDto deleteQuestion(Long questionId) {
        QuestionRecord record = requireDeletableQuestion(questionId);
        requireUpdated(questionMapper.softDelete(questionId, DEFAULT_USER_ID), "Question not found");
        return toQuestionDto(record);
    }

    public AiMessageResponse debate(Long windowId, DebateMessageRequest request) {
        SessionWindowContext context = requireWindowContext(windowId);
        requireActivePersona(request.getPersonaId());
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
        MessageRecord aiMessage = insertMessage(MessageRecord.builder()
            .sessionId(context.getSessionId())
            .windowId(windowId)
            .userId(userId)
            .parentMessageId(userMessage.getId())
            .role(aiResponse.getRole())
            .content(aiResponse.getContent())
            .aiModel(aiResponse.getAiModel())
            .personaId(request.getPersonaId())
            .streamingStatus(COMPLETE_STREAMING_STATUS)
            .testData(true)
            .build());

        return copyWithPersistedMessageId(aiResponse, aiMessage.getId());
    }

    public AiMessageListResponse debateAll(Long windowId, DebateAllMessageRequest request) {
        SessionWindowContext context = requireWindowContext(windowId);
        List<PersonaRecord> personas = selectedDebatePersonas(request.getPersonaIds());
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

        List<DebateMessageRequest> personaRequests = personas.stream()
            .map((persona) -> DebateMessageRequest.builder()
                .personaId(persona.getId())
                .content(userMessage.getContent())
                .clientCorrelationId(request.getClientCorrelationId())
                .build())
            .toList();
        List<AiMessageResponse> aiResponses = aiProvider.answerDebateMessages(windowId, personaRequests);

        return AiMessageListResponse.builder()
            .messages(aiResponses.stream()
                .map((aiResponse) -> insertPersonaDebateResponse(windowId, context, userId, userMessage, aiResponse))
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

    private List<PersonaRecord> selectedDebatePersonas(List<Long> personaIds) {
        if (personaIds == null || personaIds.isEmpty()) {
            return personaMapper.findActive();
        }

        return personaIds.stream()
            .distinct()
            .map(this::requireActivePersona)
            .toList();
    }

    private MessageRecord insertMessage(MessageRecord record) {
        record.setMessageOrder(messageMapper.selectNextOrder(record.getSessionId(), record.getWindowId()));
        requireInserted(messageMapper.insert(record), "Message could not be saved");
        return record;
    }

    private AiMessageResponse copyWithPersistedMessageId(AiMessageResponse response, Long messageId) {
        return AiMessageResponse.builder()
            .messageId(messageId)
            .windowId(response.getWindowId())
            .personaId(response.getPersonaId())
            .role(response.getRole())
            .content(response.getContent())
            .streamingReady(response.isStreamingReady())
            .aiModel(response.getAiModel())
            .build();
    }

    private AiMessageResponse insertPersonaDebateResponse(
        Long windowId,
        SessionWindowContext context,
        Long userId,
        MessageRecord userMessage,
        AiMessageResponse aiResponse
    ) {
        MessageRecord aiMessage = insertMessage(MessageRecord.builder()
            .sessionId(context.getSessionId())
            .windowId(windowId)
            .userId(userId)
            .parentMessageId(userMessage.getId())
            .role(aiResponse.getRole())
            .content(aiResponse.getContent())
            .aiModel(aiResponse.getAiModel())
            .personaId(aiResponse.getPersonaId())
            .streamingStatus(COMPLETE_STREAMING_STATUS)
            .testData(true)
            .build());

        return copyWithPersistedMessageId(aiResponse, aiMessage.getId());
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
