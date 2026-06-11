package com.margins.session.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CreateSessionWindowResponse {
    Long windowId;
    Long sessionId;
    String windowType;
    String title;
    String status;
}
