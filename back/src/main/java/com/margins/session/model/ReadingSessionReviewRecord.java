package com.margins.session.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingSessionReviewRecord {
    private Long id;
    private Long sessionId;
    private Long userId;
    private String title;
    private String contentHtml;
    private String editorType;
    private String status;
    private boolean testData;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
