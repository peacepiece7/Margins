package com.margins.session.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class UpdateReadingSessionProgressRequest {
    @Size(max = 500)
    String readingGoal;

    @Min(0)
    Integer startPage;

    @Min(0)
    Integer currentPage;

    @Min(0)
    Integer targetPage;

    @Size(max = 2000)
    String progressNote;
}
