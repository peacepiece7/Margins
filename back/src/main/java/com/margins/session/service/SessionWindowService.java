package com.margins.session.service;

import com.margins.session.business.SessionWindowBusiness;
import com.margins.session.dto.AiMessageResponse;
import com.margins.session.dto.CreateSessionWindowRequest;
import com.margins.session.dto.CreateSessionWindowResponse;
import com.margins.session.dto.DebateMessageRequest;
import com.margins.session.dto.SendMessageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionWindowService {

    private final SessionWindowBusiness sessionWindowBusiness;

    public CreateSessionWindowResponse create(CreateSessionWindowRequest request) {
        return sessionWindowBusiness.create(request);
    }

    public AiMessageResponse sendMessage(Long windowId, SendMessageRequest request) {
        return sessionWindowBusiness.sendMessage(windowId, request);
    }

    public AiMessageResponse debate(Long windowId, DebateMessageRequest request) {
        return sessionWindowBusiness.debate(windowId, request);
    }
}
