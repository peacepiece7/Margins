package com.margins.session.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReadingLibraryStatsResponse {
    private int sessionCount;
    private int activeSessionCount;
    private int completedSessionCount;
    private int distinctBookCount;
    private int answeredQuestionCount;
    private int highlightCount;
    private int messageCount;
    private Integer averageProgressPercent;
}
