package com.margins.persona.business;

import com.margins.ai.AiProvider;
import com.margins.ai.AiSafetyPolicy;
import com.margins.persona.dto.CreatePersonaRequest;
import com.margins.persona.dto.GeneratePersonasRequest;
import com.margins.persona.dto.PersonaDraftDto;
import com.margins.persona.dto.PersonaDraftListResponse;
import com.margins.persona.dto.PersonaDto;
import com.margins.persona.dto.PersonaListResponse;
import com.margins.persona.mapper.PersonaMapper;
import com.margins.persona.model.PersonaRecord;
import com.margins.persona.model.PersonaRoleCatalog;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class PersonaBusiness {

    private final AiProvider aiProvider;
    private final PersonaMapper personaMapper;
    private final AiSafetyPolicy aiSafetyPolicy;

    public PersonaListResponse findActive() {
        return PersonaListResponse.builder()
            .personas(personaMapper.findActive().stream().map(this::toDto).toList())
            .build();
    }

    public PersonaListResponse findActive(Long sessionId) {
        if (sessionId == null) {
            return findActive();
        }
        return PersonaListResponse.builder()
            .personas(personaMapper.findActiveForSession(sessionId).stream().map(this::toDto).toList())
            .build();
    }

    public PersonaDraftListResponse generate(GeneratePersonasRequest request) {
        PersonaDraftListResponse response = aiProvider.suggestPersonas(request);
        return PersonaDraftListResponse.builder()
            .aiModel(response.getAiModel())
            .personas(normalizeDraftRoles(response, request.getBookTitle()))
            .build();
    }

    public PersonaListResponse create(CreatePersonaRequest request) {
        boolean explicitRoleKey = request.getRoleKey() != null && !request.getRoleKey().isBlank();
        String roleKey = roleKeyForRequest(request);
        if (request.getSessionId() != null && roleKey != null
            && personaMapper.countActiveBySessionAndRoleKey(request.getSessionId(), roleKey) > 0 && explicitRoleKey) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Persona role already exists for this session");
        }
        if (request.getSessionId() != null && roleKey != null
            && personaMapper.countActiveBySessionAndRoleKey(request.getSessionId(), roleKey) > 0) {
            roleKey = firstUnusedRoleForSession(request.getSessionId(), roleKey);
        }

        PersonaRecord record = PersonaRecord.builder()
            .name(uniqueInternalName(request.getDisplayName()))
            .displayName(request.getDisplayName())
            .description(request.getDescription())
            .systemPrompt(request.getSystemPrompt())
            .tone(request.getTone())
            .roleKey(roleKey)
            .sourceSessionId(request.getSessionId())
            .active(true)
            .build();

        if (personaMapper.insert(record) <= 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Persona could not be saved");
        }
        return findActive(request.getSessionId());
    }

    private String roleKeyForRequest(CreatePersonaRequest request) {
        String normalized = PersonaRoleCatalog.normalize(request.getRoleKey());
        if (normalized != null) {
            return normalized;
        }

        String text = String.join(" ",
            safe(request.getDisplayName()),
            safe(request.getDescription()),
            safe(request.getTone()),
            safe(request.getSystemPrompt()))
            .toLowerCase(Locale.ROOT);
        if (text.matches(".*(skeptic|critic|challenge|assumption|doubt|weak).*")) {
            return PersonaRoleCatalog.SKEPTIC;
        }
        if (text.matches(".*(empathy|empathetic|emotion|feeling|warm|lived).*")) {
            return PersonaRoleCatalog.EMPATHY_READER;
        }
        if (text.matches(".*(style|form|language|voice|sentence|craft).*")) {
            return PersonaRoleCatalog.STYLE_READER;
        }
        if (text.matches(".*(connect|synthesis|theme|pattern|relation).*")) {
            return PersonaRoleCatalog.CONNECTOR;
        }
        return PersonaRoleCatalog.EVIDENCE_ANALYST;
    }

    private String firstUnusedRoleForSession(Long sessionId, String fallback) {
        for (String roleKey : PersonaRoleCatalog.defaultOrder()) {
            if (personaMapper.countActiveBySessionAndRoleKey(sessionId, roleKey) <= 0) {
                return roleKey;
            }
        }
        return fallback;
    }

    private List<PersonaDraftDto> normalizeDraftRoles(PersonaDraftListResponse response, String bookTitle) {
        List<PersonaDraftDto> personas = response.getPersonas() == null
            ? List.of()
            : response.getPersonas();
        Set<String> used = new HashSet<>();
        List<PersonaDraftDto> normalized = new ArrayList<>();
        for (int index = 0; index < personas.size(); index++) {
            PersonaDraftDto draft = personas.get(index);
            String roleKey = PersonaRoleCatalog.normalize(draft.getRoleKey(), index);
            if (used.contains(roleKey)) {
                roleKey = firstUnusedRole(used, index);
            }
            used.add(roleKey);
            normalized.add(aiSafetyPolicy.safePersonaDraft(draft, roleKey, bookTitle));
        }
        return normalized;
    }

    private String firstUnusedRole(Set<String> used, int fallbackIndex) {
        for (String roleKey : PersonaRoleCatalog.defaultOrder()) {
            if (!used.contains(roleKey)) {
                return roleKey;
            }
        }
        return PersonaRoleCatalog.normalize(null, fallbackIndex);
    }

    private String safe(String value) {
        return value == null ? "" : value;
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
            .roleKey(record.getRoleKey())
            .build();
    }
}
