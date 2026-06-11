package com.margins.session.business;

import com.margins.ai.AiProvider;
import com.margins.session.dto.AiMessageResponse;
import com.margins.session.dto.CreateSessionWindowRequest;
import com.margins.session.dto.CreateSessionWindowResponse;
import com.margins.session.dto.DebateMessageRequest;
import com.margins.session.dto.SendMessageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionWindowBusiness {

    private final AiProvider aiProvider;

    public CreateSessionWindowResponse create(CreateSessionWindowRequest request) {
        return CreateSessionWindowResponse.builder()
            .windowId(1L)
            .sessionId(request.getSessionId())
            .windowType(request.getWindowType())
            .title(request.getTitle())
            .status("open")
            .build();
    }

    public AiMessageResponse sendMessage(Long windowId, SendMessageRequest request) {
        return aiProvider.answerWindowMessage(windowId, request);
    }

    public AiMessageResponse debate(Long windowId, DebateMessageRequest request) {
        return aiProvider.answerDebateMessage(windowId, request);
    }
}
