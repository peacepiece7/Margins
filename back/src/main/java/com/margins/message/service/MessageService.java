package com.margins.message.service;

import com.margins.message.business.MessageBusiness;
import com.margins.message.dto.UpdateMessageRequest;
import com.margins.session.dto.SessionMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageBusiness messageBusiness;

    @Transactional
    public SessionMessageDto update(Long messageId, UpdateMessageRequest request) {
        return messageBusiness.update(messageId, request);
    }

    @Transactional
    public SessionMessageDto delete(Long messageId) {
        return messageBusiness.delete(messageId);
    }
}
