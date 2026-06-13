package com.margins.session.service;

import com.margins.session.business.ReadingSessionBusiness;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReadingSessionService {

    private final ReadingSessionBusiness readingSessionBusiness;

    @Transactional
    public CreateReadingSessionResponse create(CreateReadingSessionRequest request) {
        return readingSessionBusiness.create(request);
    }

    @Transactional(readOnly = true)
    public ReadingSessionTimelineResponse findLatestTimeline() {
        return readingSessionBusiness.findLatestTimeline();
    }

    @Transactional(readOnly = true)
    public ReadingSessionTimelineResponse findTimeline(Long sessionId) {
        return readingSessionBusiness.findTimeline(sessionId);
    }

    @Transactional(readOnly = true)
    public ReadingSessionListResponse findSummaries() {
        return readingSessionBusiness.findSummaries();
    }

    @Transactional(readOnly = true)
    public ReadingLibraryStatsResponse findLibraryStats() {
        return readingSessionBusiness.findLibraryStats();
    }

    @Transactional(readOnly = true)
    public SessionSearchResponse search(String query) {
        return readingSessionBusiness.search(query);
    }

    @Transactional
    public ReadingSessionTimelineResponse complete(Long sessionId, CompleteReadingSessionRequest request) {
        return readingSessionBusiness.complete(sessionId, request);
    }

    @Transactional
    public ReadingSessionListResponse archive(Long sessionId) {
        return readingSessionBusiness.archive(sessionId);
    }

    @Transactional
    public ReadingSessionTimelineResponse updateTitle(Long sessionId, UpdateReadingSessionTitleRequest request) {
        return readingSessionBusiness.updateTitle(sessionId, request);
    }

    @Transactional
    public ReadingSessionTimelineResponse updateProgress(Long sessionId, UpdateReadingSessionProgressRequest request) {
        return readingSessionBusiness.updateProgress(sessionId, request);
    }

    @Transactional
    public ReadingSessionListResponse updatePinned(Long sessionId, UpdateReadingSessionPinRequest request) {
        return readingSessionBusiness.updatePinned(sessionId, request);
    }

    @Transactional
    public ReadingSessionTimelineResponse createHighlight(Long sessionId, CreateSessionHighlightRequest request) {
        return readingSessionBusiness.createHighlight(sessionId, request);
    }

    @Transactional
    public ReadingSessionTimelineResponse updateHighlight(Long sessionId, Long highlightId, UpdateSessionHighlightRequest request) {
        return readingSessionBusiness.updateHighlight(sessionId, highlightId, request);
    }

    @Transactional
    public ReadingSessionTimelineResponse deleteHighlight(Long sessionId, Long highlightId) {
        return readingSessionBusiness.deleteHighlight(sessionId, highlightId);
    }

    @Transactional
    public ReadingSessionTimelineResponse createTag(Long sessionId, CreateSessionTagRequest request) {
        return readingSessionBusiness.createTag(sessionId, request);
    }

    @Transactional
    public ReadingSessionTimelineResponse deleteTag(Long sessionId, Long tagId) {
        return readingSessionBusiness.deleteTag(sessionId, tagId);
    }

    @Transactional
    public ReadingSessionTimelineResponse createInsight(Long sessionId, CreateSessionInsightRequest request) {
        return readingSessionBusiness.createInsight(sessionId, request);
    }

    @Transactional
    public ReadingSessionTimelineResponse deleteInsight(Long sessionId, Long insightId) {
        return readingSessionBusiness.deleteInsight(sessionId, insightId);
    }
}
