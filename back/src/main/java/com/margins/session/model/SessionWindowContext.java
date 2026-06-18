package com.margins.session.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionWindowContext {
    private Long id;
    private Long sessionId;
    private Long userId;
    private String sessionTitle;
    private String readingGoal;
    private Integer startPage;
    private Integer currentPage;
    private Integer targetPage;
    private String progressNote;
    private String summary;
    private String bookTitle;
    private String bookAuthor;

    public SessionWindowContext(Long id, Long sessionId, Long userId) {
        this.id = id;
        this.sessionId = sessionId;
        this.userId = userId;
    }
}
