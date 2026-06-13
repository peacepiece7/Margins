package com.margins.message.controller;

import com.margins.common.dto.ApiResponse;
import com.margins.message.dto.UpdateMessageRequest;
import com.margins.message.service.MessageService;
import com.margins.session.dto.SessionMessageDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PatchMapping("/{id}")
    public ApiResponse<SessionMessageDto> update(
        @PathVariable("id") Long messageId,
        @Valid @RequestBody UpdateMessageRequest request
    ) {
        return ApiResponse.ok(messageService.update(messageId, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<SessionMessageDto> delete(@PathVariable("id") Long messageId) {
        return ApiResponse.ok(messageService.delete(messageId));
    }
}
