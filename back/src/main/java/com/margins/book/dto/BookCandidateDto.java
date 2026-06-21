package com.margins.book.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BookCandidateDto {
    String candidateId;
    String isbn;
    String title;
    String author;
    Integer publishedYear;
    String reason;
}
