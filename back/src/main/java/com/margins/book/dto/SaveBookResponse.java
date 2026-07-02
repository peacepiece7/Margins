package com.margins.book.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SaveBookResponse {
    Long bookId;
    String title;
    String subtitle;
    String author;
    String publisher;
    Integer publishedYear;
    String isbn;
    String source;
    String sourceRef;
    String coverImageUrl;
    String language;
}
