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
}
