package com.margins.session.controller;

import com.margins.common.dto.ApiResponse;
import com.margins.question.dto.CreateQuestionRequest;
import com.margins.question.dto.GenerateQuestionsRequest;
import com.margins.question.dto.QuestionListResponse;
import com.margins.session.dto.AiMessageListResponse;
import com.margins.session.dto.AiMessageResponse;
import com.margins.session.dto.CreateSessionWindowRequest;
import com.margins.session.dto.CreateSessionWindowResponse;
import com.margins.session.dto.DebateAllMessageRequest;
import com.margins.session.dto.DebateMessageRequest;
import com.margins.session.dto.SendMessageRequest;
import com.margins.session.dto.UpdateSessionWindowTitleRequest;
import com.margins.session.service.SessionWindowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/session-windows")
@RequiredArgsConstructor
public class SessionWindowController {

    private final SessionWindowService sessionWindowService;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ApiResponse<CreateSessionWindowResponse> create(@Valid @RequestBody CreateSessionWindowRequest request) {
        return ApiResponse.ok(sessionWindowService.create(request));
    }

    @PatchMapping("/{id}/title")
    public ApiResponse<CreateSessionWindowResponse> updateTitle(
        @PathVariable("id") Long windowId,
        @Valid @RequestBody UpdateSessionWindowTitleRequest request
    ) {
        return ApiResponse.ok(sessionWindowService.updateTitle(windowId, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<CreateSessionWindowResponse> archive(@PathVariable("id") Long windowId) {
        return ApiResponse.ok(sessionWindowService.archive(windowId));
    }

    @PostMapping("/{id}/messages")
    public ApiResponse<AiMessageResponse> sendMessage(
        @PathVariable("id") Long windowId,
        @Valid @RequestBody SendMessageRequest request
    ) {
        return ApiResponse.ok(sessionWindowService.sendMessage(windowId, request));
    }

    @PostMapping(value = "/{id}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public StreamingResponseBody streamMessage(
        @PathVariable("id") Long windowId,
        @Valid @RequestBody SendMessageRequest request
    ) {
        return (outputStream) -> {
            writeEvent(outputStream, "message.start", streamPayload(windowId, "", request.getClientCorrelationId()));
            try {
                AiMessageResponse response = sessionWindowService.streamMessage(windowId, request, (delta) -> writeDelta(outputStream, windowId, delta, request.getClientCorrelationId()));
                writeEvent(outputStream, "message.done", response);
            } catch (UncheckedIOException exception) {
                throw exception.getCause();
            } catch (RuntimeException exception) {
                writeEvent(outputStream, "message.error", errorPayload(windowId, exception, request.getClientCorrelationId()));
            }
        };
    }

    @GetMapping("/{id}/questions")
    public ApiResponse<QuestionListResponse> questions(@PathVariable("id") Long windowId) {
        return ApiResponse.ok(sessionWindowService.questions(windowId));
    }

    @PostMapping("/{id}/questions")
    public ApiResponse<QuestionListResponse> createQuestion(
        @PathVariable("id") Long windowId,
        @Valid @RequestBody CreateQuestionRequest request
    ) {
        return ApiResponse.ok(sessionWindowService.createQuestion(windowId, request));
    }

    @PostMapping("/{id}/questions/generate")
    public ApiResponse<QuestionListResponse> generateQuestions(
        @PathVariable("id") Long windowId,
        @Valid @RequestBody GenerateQuestionsRequest request
    ) {
        return ApiResponse.ok(sessionWindowService.generateQuestions(windowId, request));
    }

    @PostMapping("/{id}/questions/suggest")
    public ApiResponse<QuestionListResponse> suggestQuestions(
        @PathVariable("id") Long windowId,
        @Valid @RequestBody GenerateQuestionsRequest request
    ) {
        return ApiResponse.ok(sessionWindowService.suggestQuestions(windowId, request));
    }

    @PostMapping("/{id}/debate")
    public ApiResponse<AiMessageResponse> debate(
        @PathVariable("id") Long windowId,
        @Valid @RequestBody DebateMessageRequest request
    ) {
        return ApiResponse.ok(sessionWindowService.debate(windowId, request));
    }

    @PostMapping("/{id}/debate/all")
    public ApiResponse<AiMessageListResponse> debateAll(
        @PathVariable("id") Long windowId,
        @Valid @RequestBody DebateAllMessageRequest request
    ) {
        return ApiResponse.ok(sessionWindowService.debateAll(windowId, request));
    }

    private void writeEvent(OutputStream outputStream, String eventName, Object data) throws IOException {
        outputStream.write(("event: " + eventName + "\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(("data: " + objectMapper.writeValueAsString(data) + "\n\n").getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    private void writeDelta(OutputStream outputStream, Long windowId, String delta, String clientCorrelationId) {
        try {
            writeEvent(outputStream, "message.delta", streamPayload(windowId, delta, clientCorrelationId));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private Map<String, Object> streamPayload(Long windowId, String delta, String clientCorrelationId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("windowId", windowId);
        payload.put("delta", delta);
        if (clientCorrelationId != null) {
            payload.put("clientCorrelationId", clientCorrelationId);
        }
        return payload;
    }

    private Map<String, Object> errorPayload(Long windowId, RuntimeException exception, String clientCorrelationId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("windowId", windowId);
        payload.put("message", exception.getMessage() == null ? "Streaming message failed" : exception.getMessage());
        if (clientCorrelationId != null) {
            payload.put("clientCorrelationId", clientCorrelationId);
        }
        return payload;
    }

}
