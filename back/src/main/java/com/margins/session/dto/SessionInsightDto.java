package com.margins.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInsightDto {
    private Long insightId;
    private Long sessionId;
    private String insightType;
    private String title;
    private String content;
    private String evidence;
    private Integer insightOrder;
}
