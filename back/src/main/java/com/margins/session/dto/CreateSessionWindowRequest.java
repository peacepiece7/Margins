package com.margins.session.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class CreateSessionWindowRequest {
    @NotNull
    Long sessionId;
    @NotBlank
    String windowType;
    @NotBlank
    String title;
}
