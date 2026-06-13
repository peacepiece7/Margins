package com.margins.session.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class CreateSessionInsightRequest {
    @Size(max = 60)
    String insightType;

    @Size(max = 160)
    String title;

    @NotBlank
    String content;

    String evidence;
}
