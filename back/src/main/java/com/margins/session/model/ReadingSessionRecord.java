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
    private String bookTitle;
    private String bookAuthor;
    private String title;
    private String status;
    private boolean pinned;
    private String readingGoal;
    private Integer startPage;
    private Integer currentPage;
    private Integer targetPage;
    private String progressNote;
    private String summary;
    private Integer windowCount;
    private Integer questionCount;
    private Integer answeredQuestionCount;
    private Integer highlightCount;
    private Integer messageCount;
    private boolean testData;
}
