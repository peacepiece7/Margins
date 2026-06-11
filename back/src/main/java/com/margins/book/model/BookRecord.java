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
    private String author;
    private Integer publishedYear;
    private String source;
    private String sourceRef;
    private boolean testData;
}
