package com.margins.message.business;

import com.margins.message.dto.UpdateMessageRequest;
import com.margins.message.mapper.MessageMapper;
import com.margins.message.model.MessageRecord;
import com.margins.session.dto.SessionMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class MessageBusiness {

    private static final long DEFAULT_USER_ID = 1L;

    private final MessageMapper messageMapper;

    public SessionMessageDto update(Long messageId, UpdateMessageRequest request) {
        requireEditableMessage(messageId);
        messageMapper.updateContent(messageId, DEFAULT_USER_ID, request.getContent());
        return toDto(requireEditableMessage(messageId));
    }

    public SessionMessageDto delete(Long messageId) {
        MessageRecord record = requireEditableMessage(messageId);
        requireUpdated(messageMapper.softDelete(messageId, DEFAULT_USER_ID));
        return toDto(record);
    }

    private MessageRecord requireEditableMessage(Long messageId) {
        MessageRecord record = messageMapper.findEditableById(messageId, DEFAULT_USER_ID);
        if (record == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Editable message not found");
        }
        return record;
    }

    private void requireUpdated(int updatedRows) {
        if (updatedRows <= 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Editable message not found");
        }
    }

    private SessionMessageDto toDto(MessageRecord record) {
        return SessionMessageDto.builder()
            .messageId(record.getId())
            .sessionId(record.getSessionId())
            .windowId(record.getWindowId())
            .parentMessageId(record.getParentMessageId())
            .role(record.getRole())
            .content(record.getContent())
            .messageOrder(record.getMessageOrder())
            .aiModel(record.getAiModel())
            .personaId(record.getPersonaId())
            .questionId(record.getQuestionId())
            .streamingStatus(record.getStreamingStatus())
            .build();
    }
}
