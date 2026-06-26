package com.margins.session.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingSessionReviewDto {
    private Long reviewId;
    private Long sessionId;
    private String title;
    private String contentHtml;
    private String editorType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
