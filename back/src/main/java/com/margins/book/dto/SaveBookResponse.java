package com.margins.book.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SaveBookResponse {
    Long bookId;
    String title;
    String author;
}
