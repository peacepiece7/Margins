package com.margins.persona.controller;

import com.margins.common.dto.ApiResponse;
import com.margins.persona.dto.CreatePersonaRequest;
import com.margins.persona.dto.GeneratePersonasRequest;
import com.margins.persona.dto.PersonaDraftListResponse;
import com.margins.persona.dto.PersonaListResponse;
import com.margins.persona.service.PersonaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/personas")
@RequiredArgsConstructor
public class PersonaController {

    private final PersonaService personaService;

    @GetMapping
    public ApiResponse<PersonaListResponse> activePersonas(@RequestParam(value = "sessionId", required = false) Long sessionId) {
        return ApiResponse.ok(personaService.findActive(sessionId));
    }

    @PostMapping
    public ApiResponse<PersonaListResponse> create(@Valid @RequestBody CreatePersonaRequest request) {
        return ApiResponse.ok(personaService.create(request));
    }

    @PostMapping("/generate")
    public ApiResponse<PersonaDraftListResponse> generate(@Valid @RequestBody GeneratePersonasRequest request) {
        return ApiResponse.ok(personaService.generate(request));
    }
}
