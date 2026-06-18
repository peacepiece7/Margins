package com.margins.session.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AiMessageResponse {
    Long messageId;
    Long windowId;
    Long personaId;
    String role;
    String content;
    boolean streamingReady;
    String aiModel;
    String promptSnapshot;
    String contextSnapshot;
    String tokenUsage;
}
