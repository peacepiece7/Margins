package com.margins.persona.business;

import com.margins.persona.dto.CreatePersonaRequest;
import com.margins.persona.dto.PersonaDto;
import com.margins.persona.dto.PersonaListResponse;
import com.margins.persona.mapper.PersonaMapper;
import com.margins.persona.model.PersonaRecord;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class PersonaBusiness {

    private final PersonaMapper personaMapper;

    public PersonaListResponse findActive() {
        return PersonaListResponse.builder()
            .personas(personaMapper.findActive().stream().map(this::toDto).toList())
            .build();
    }

    public PersonaListResponse create(CreatePersonaRequest request) {
        PersonaRecord record = PersonaRecord.builder()
            .name(uniqueInternalName(request.getDisplayName()))
            .displayName(request.getDisplayName())
            .description(request.getDescription())
            .systemPrompt(request.getSystemPrompt())
            .tone(request.getTone())
            .active(true)
            .build();

        if (personaMapper.insert(record) <= 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Persona could not be saved");
        }
        return findActive();
    }

    private String uniqueInternalName(String displayName) {
        String slug = displayName.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
        String base = slug.isBlank() ? "reader-persona" : slug;
        return "reader-" + base + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private PersonaDto toDto(PersonaRecord record) {
        return PersonaDto.builder()
            .personaId(record.getId())
            .name(record.getName())
            .displayName(record.getDisplayName())
            .description(record.getDescription())
            .tone(record.getTone())
            .build();
    }
}
