package com.margins.session.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class CreateReadingSessionRequest {
    Long bookId;
    String title;
}
