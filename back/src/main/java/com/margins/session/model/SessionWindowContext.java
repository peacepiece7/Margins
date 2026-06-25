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
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String bookIsbn;
    private String bookRawMetadata;

    public SessionWindowContext(Long id, Long sessionId, Long userId) {
        this.id = id;
        this.sessionId = sessionId;
        this.userId = userId;
    }
}
