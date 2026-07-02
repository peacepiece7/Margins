package com.margins.book.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookRecord {
    private Long id;
    private Long userId;
    private String title;
    private String subtitle;
    private String author;
    private String publisher;
    private String isbn;
    private Integer publishedYear;
    private String languageCode;
    private String description;
    private String source;
    private String sourceRef;
    private String coverImageUrl;
    private String rawMetadata;
    private boolean testData;
}
