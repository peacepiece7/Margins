package com.margins.session.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class CreateSessionWindowRequest {
    Long sessionId;
    String windowType;
    String title;
}
