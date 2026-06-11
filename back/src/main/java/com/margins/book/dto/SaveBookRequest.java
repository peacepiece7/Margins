package com.margins.book.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class SaveBookRequest {
    String candidateId;
    String title;
    String author;
    Integer publishedYear;
}
