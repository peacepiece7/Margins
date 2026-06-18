package com.margins.persona.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonaDraftListResponse {
    private String aiModel;
    private List<PersonaDraftDto> personas;
}
