package com.margins.session.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class SaveReadingSessionReviewRequest {
    @NotBlank
    @Size(max = 255)
    String title;

    @NotBlank
    String contentHtml;

    @Size(max = 40)
    String status;
}
