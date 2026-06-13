package com.margins.session.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInsightRecord {
    private Long id;
    private Long sessionId;
    private Long userId;
    private String insightType;
    private String title;
    private String content;
    private String evidence;
    private Integer insightOrder;
    private boolean testData;
}
