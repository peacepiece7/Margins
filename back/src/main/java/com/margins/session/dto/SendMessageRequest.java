package com.margins.session.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class SendMessageRequest {
    Long userId;
    String content;
    Long questionId;
    String clientCorrelationId;
}
