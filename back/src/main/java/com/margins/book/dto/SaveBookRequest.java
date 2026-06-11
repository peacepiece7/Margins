package com.margins.book.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class SaveBookRequest {
    @NotBlank
    String candidateId;
    @NotBlank
    String title;
    @NotBlank
    String author;
    Integer publishedYear;
}
