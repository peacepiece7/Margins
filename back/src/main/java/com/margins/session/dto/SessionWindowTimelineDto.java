package com.margins.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionWindowTimelineDto {
    private Long windowId;
    private Long sessionId;
    private String windowType;
    private String title;
    private Integer position;
    private String status;
}
