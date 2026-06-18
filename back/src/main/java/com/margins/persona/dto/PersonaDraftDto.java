package com.margins.persona.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonaDraftDto {
    private String displayName;
    private String description;
    private String tone;
    private String roleKey;
    private String systemPrompt;
    private String reason;
}
