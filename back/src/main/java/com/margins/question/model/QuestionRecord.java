package com.margins.question.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionRecord {
    private Long id;
    private Long sessionId;
    private Long windowId;
    private Long userId;
    private String questionText;
    private String questionType;
    private String status;
    private String aiModel;
    private boolean testData;
}
