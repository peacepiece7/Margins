package com.margins.session.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingSessionRecord {
    private Long id;
    private Long userId;
    private Long bookId;
    private String title;
    private String status;
    private boolean testData;
}
