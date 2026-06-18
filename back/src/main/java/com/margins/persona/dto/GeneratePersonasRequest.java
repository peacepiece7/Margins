package com.margins.persona.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class GeneratePersonasRequest {
    @Min(1)
    @Max(5)
    Integer count;
    String bookTitle;
    String readingGoal;
    String context;
}
