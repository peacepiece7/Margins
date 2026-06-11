package com.margins.session.business;

import com.margins.ai.AiProvider;
import com.margins.message.mapper.MessageMapper;
import com.margins.message.model.MessageRecord;
import com.margins.session.dto.AiMessageResponse;
import com.margins.session.dto.CreateSessionWindowRequest;
import com.margins.session.dto.CreateSessionWindowResponse;
import com.margins.session.dto.DebateMessageRequest;
import com.margins.session.dto.SendMessageRequest;
import com.margins.session.mapper.SessionWindowMapper;
import com.margins.session.model.SessionWindowContext;
import com.margins.session.model.SessionWindowRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class SessionWindowBusiness {

    private static final long DEFAULT_USER_ID = 1L;
    private static final String OPEN_STATUS = "open";
    private static final String COMPLETE_STREAMING_STATUS = "complete";

    private final AiProvider aiProvider;
    private final SessionWindowMapper sessionWindowMapper;
    private final MessageMapper messageMapper;

    public CreateSessionWindowResponse create(CreateSessionWindowRequest request) {
        SessionWindowRecord record = SessionWindowRecord.builder()
            .sessionId(request.getSessionId())
            .userId(DEFAULT_USER_ID)
            .windowType(request.getWindowType())
            .title(request.getTitle())
            .position(sessionWindowMapper.selectNextPosition(request.getSessionId()))
            .status(OPEN_STATUS)
            .testData(true)
            .build();

        sessionWindowMapper.insert(record);

        return CreateSessionWindowResponse.builder()
            .windowId(record.getId())
            .sessionId(record.getSessionId())
            .windowType(record.getWindowType())
            .title(record.getTitle())
            .status(record.getStatus())
            .build();
    }

    public AiMessageResponse sendMessage(Long windowId, SendMessageRequest request) {
        SessionWindowContext context = requireWindowContext(windowId);
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

    public AiMessageResponse debate(Long windowId, DebateMessageRequest request) {
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

    private SessionWindowContext requireWindowContext(Long windowId) {
        SessionWindowContext context = sessionWindowMapper.findContextById(windowId);
        if (context == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session window not found");
        }
        return context;
    }

    private Long resolveUserId(Long requestUserId, SessionWindowContext context) {
        if (requestUserId != null) {
            return requestUserId;
        }
        if (context.getUserId() != null) {
            return context.getUserId();
        }
        return DEFAULT_USER_ID;
    }

    private MessageRecord insertMessage(MessageRecord record) {
        record.setMessageOrder(messageMapper.selectNextOrder(record.getSessionId()));
        messageMapper.insert(record);
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
}
