package com.margins.persona.service;

import com.margins.persona.business.PersonaBusiness;
import com.margins.persona.dto.CreatePersonaRequest;
import com.margins.persona.dto.GeneratePersonasRequest;
import com.margins.persona.dto.PersonaDraftListResponse;
import com.margins.persona.dto.PersonaListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PersonaService {

    private final PersonaBusiness personaBusiness;

    @Transactional(readOnly = true)
    public PersonaListResponse findActive() {
        return personaBusiness.findActive();
    }

    @Transactional(readOnly = true)
    public PersonaListResponse findActive(Long sessionId) {
        return personaBusiness.findActive(sessionId);
    }

    @Transactional(readOnly = true)
    public PersonaDraftListResponse generate(GeneratePersonasRequest request) {
        return personaBusiness.generate(request);
    }

    @Transactional
    public PersonaListResponse create(CreatePersonaRequest request) {
        return personaBusiness.create(request);
    }
}
