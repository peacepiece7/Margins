package com.margins.session.controller;

import com.margins.common.dto.ApiResponse;
import com.margins.session.dto.CreateReadingSessionRequest;
import com.margins.session.dto.CreateReadingSessionResponse;
import com.margins.session.service.ReadingSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reading-sessions")
@RequiredArgsConstructor
public class ReadingSessionController {

    private final ReadingSessionService readingSessionService;

    @PostMapping
    public ApiResponse<CreateReadingSessionResponse> create(@Valid @RequestBody CreateReadingSessionRequest request) {
        return ApiResponse.ok(readingSessionService.create(request));
    }
}
