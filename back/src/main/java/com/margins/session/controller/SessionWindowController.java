package com.margins.session.controller;

import com.margins.common.dto.ApiResponse;
import com.margins.session.dto.AiMessageResponse;
import com.margins.session.dto.CreateSessionWindowRequest;
import com.margins.session.dto.CreateSessionWindowResponse;
import com.margins.session.dto.DebateMessageRequest;
import com.margins.session.dto.SendMessageRequest;
import com.margins.session.service.SessionWindowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/session-windows")
@RequiredArgsConstructor
public class SessionWindowController {

    private final SessionWindowService sessionWindowService;

    @PostMapping
    public ApiResponse<CreateSessionWindowResponse> create(@Valid @RequestBody CreateSessionWindowRequest request) {
        return ApiResponse.ok(sessionWindowService.create(request));
    }

    @PostMapping("/{id}/messages")
    public ApiResponse<AiMessageResponse> sendMessage(
        @PathVariable("id") Long windowId,
        @Valid @RequestBody SendMessageRequest request
    ) {
        return ApiResponse.ok(sessionWindowService.sendMessage(windowId, request));
    }

    @PostMapping("/{id}/debate")
    public ApiResponse<AiMessageResponse> debate(
        @PathVariable("id") Long windowId,
        @Valid @RequestBody DebateMessageRequest request
    ) {
        return ApiResponse.ok(sessionWindowService.debate(windowId, request));
    }
}
