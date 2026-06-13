package com.margins.session.controller;

import com.margins.common.dto.ApiResponse;
import com.margins.session.dto.CompleteReadingSessionRequest;
import com.margins.session.dto.CreateReadingSessionRequest;
import com.margins.session.dto.CreateReadingSessionResponse;
import com.margins.session.dto.CreateSessionHighlightRequest;
import com.margins.session.dto.CreateSessionInsightRequest;
import com.margins.session.dto.CreateSessionTagRequest;
import com.margins.session.dto.ReadingLibraryStatsResponse;
import com.margins.session.dto.ReadingSessionListResponse;
import com.margins.session.dto.ReadingSessionTimelineResponse;
import com.margins.session.dto.SessionSearchResponse;
import com.margins.session.dto.UpdateSessionHighlightRequest;
import com.margins.session.dto.UpdateReadingSessionPinRequest;
import com.margins.session.dto.UpdateReadingSessionProgressRequest;
import com.margins.session.dto.UpdateReadingSessionTitleRequest;
import com.margins.session.service.ReadingSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping
    public ApiResponse<ReadingSessionListResponse> list() {
        return ApiResponse.ok(readingSessionService.findSummaries());
    }

    @GetMapping("/stats")
    public ApiResponse<ReadingLibraryStatsResponse> stats() {
        return ApiResponse.ok(readingSessionService.findLibraryStats());
    }

    @GetMapping("/search")
    public ApiResponse<SessionSearchResponse> search(@RequestParam(value = "query", required = false) String query) {
        return ApiResponse.ok(readingSessionService.search(query));
    }

    @GetMapping("/latest")
    public ApiResponse<ReadingSessionTimelineResponse> latest() {
        return ApiResponse.ok(readingSessionService.findLatestTimeline());
    }

    @GetMapping("/{id}")
    public ApiResponse<ReadingSessionTimelineResponse> timeline(@PathVariable("id") Long sessionId) {
        return ApiResponse.ok(readingSessionService.findTimeline(sessionId));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<ReadingSessionTimelineResponse> complete(
        @PathVariable("id") Long sessionId,
        @Valid @RequestBody CompleteReadingSessionRequest request
    ) {
        return ApiResponse.ok(readingSessionService.complete(sessionId, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<ReadingSessionListResponse> archive(@PathVariable("id") Long sessionId) {
        return ApiResponse.ok(readingSessionService.archive(sessionId));
    }

    @PatchMapping("/{id}/title")
    public ApiResponse<ReadingSessionTimelineResponse> updateTitle(
        @PathVariable("id") Long sessionId,
        @Valid @RequestBody UpdateReadingSessionTitleRequest request
    ) {
        return ApiResponse.ok(readingSessionService.updateTitle(sessionId, request));
    }

    @PatchMapping("/{id}/progress")
    public ApiResponse<ReadingSessionTimelineResponse> updateProgress(
        @PathVariable("id") Long sessionId,
        @Valid @RequestBody UpdateReadingSessionProgressRequest request
    ) {
        return ApiResponse.ok(readingSessionService.updateProgress(sessionId, request));
    }

    @PatchMapping("/{id}/pin")
    public ApiResponse<ReadingSessionListResponse> updatePin(
        @PathVariable("id") Long sessionId,
        @Valid @RequestBody UpdateReadingSessionPinRequest request
    ) {
        return ApiResponse.ok(readingSessionService.updatePinned(sessionId, request));
    }

    @PostMapping("/{id}/highlights")
    public ApiResponse<ReadingSessionTimelineResponse> createHighlight(
        @PathVariable("id") Long sessionId,
        @Valid @RequestBody CreateSessionHighlightRequest request
    ) {
        return ApiResponse.ok(readingSessionService.createHighlight(sessionId, request));
    }

    @PatchMapping("/{id}/highlights/{highlightId}")
    public ApiResponse<ReadingSessionTimelineResponse> updateHighlight(
        @PathVariable("id") Long sessionId,
        @PathVariable("highlightId") Long highlightId,
        @Valid @RequestBody UpdateSessionHighlightRequest request
    ) {
        return ApiResponse.ok(readingSessionService.updateHighlight(sessionId, highlightId, request));
    }

    @DeleteMapping("/{id}/highlights/{highlightId}")
    public ApiResponse<ReadingSessionTimelineResponse> deleteHighlight(
        @PathVariable("id") Long sessionId,
        @PathVariable("highlightId") Long highlightId
    ) {
        return ApiResponse.ok(readingSessionService.deleteHighlight(sessionId, highlightId));
    }

    @PostMapping("/{id}/tags")
    public ApiResponse<ReadingSessionTimelineResponse> createTag(
        @PathVariable("id") Long sessionId,
        @Valid @RequestBody CreateSessionTagRequest request
    ) {
        return ApiResponse.ok(readingSessionService.createTag(sessionId, request));
    }

    @DeleteMapping("/{id}/tags/{tagId}")
    public ApiResponse<ReadingSessionTimelineResponse> deleteTag(
        @PathVariable("id") Long sessionId,
        @PathVariable("tagId") Long tagId
    ) {
        return ApiResponse.ok(readingSessionService.deleteTag(sessionId, tagId));
    }

    @PostMapping("/{id}/insights")
    public ApiResponse<ReadingSessionTimelineResponse> createInsight(
        @PathVariable("id") Long sessionId,
        @Valid @RequestBody CreateSessionInsightRequest request
    ) {
        return ApiResponse.ok(readingSessionService.createInsight(sessionId, request));
    }

    @DeleteMapping("/{id}/insights/{insightId}")
    public ApiResponse<ReadingSessionTimelineResponse> deleteInsight(
        @PathVariable("id") Long sessionId,
        @PathVariable("insightId") Long insightId
    ) {
        return ApiResponse.ok(readingSessionService.deleteInsight(sessionId, insightId));
    }
}
