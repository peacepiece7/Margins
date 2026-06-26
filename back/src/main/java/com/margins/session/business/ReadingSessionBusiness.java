package com.margins.session.business;

import com.margins.message.mapper.MessageMapper;
import com.margins.message.model.MessageRecord;
import com.margins.question.dto.QuestionDto;
import com.margins.question.mapper.QuestionMapper;
import com.margins.question.model.QuestionRecord;
import com.margins.session.dto.CompleteReadingSessionRequest;
import com.margins.session.dto.CreateReadingSessionRequest;
import com.margins.session.dto.CreateReadingSessionResponse;
import com.margins.session.dto.CreateSessionInsightRequest;
import com.margins.session.dto.CreateSessionTagRequest;
import com.margins.session.dto.CreateSessionHighlightRequest;
import com.margins.session.dto.ReadingLibraryStatsResponse;
import com.margins.session.dto.ReadingSessionReviewDto;
import com.margins.session.dto.ReadingSessionNextActionDto;
import com.margins.session.dto.ReadingSessionListResponse;
import com.margins.session.dto.ReadingSessionStatsDto;
import com.margins.session.dto.ReadingSessionSummaryDto;
import com.margins.session.dto.ReadingSessionTimelineResponse;
import com.margins.session.dto.SessionSearchResponse;
import com.margins.session.dto.SessionSearchResultDto;
import com.margins.session.dto.SessionHighlightDto;
import com.margins.session.dto.SessionInsightDto;
import com.margins.session.dto.SessionMessageDto;
import com.margins.session.dto.SaveReadingSessionReviewRequest;
import com.margins.session.dto.SessionTagDto;
import com.margins.session.dto.SessionWindowTimelineDto;
import com.margins.session.dto.UpdateSessionHighlightRequest;
import com.margins.session.dto.UpdateReadingSessionPinRequest;
import com.margins.session.dto.UpdateReadingSessionProgressRequest;
import com.margins.session.dto.UpdateReadingSessionTitleRequest;
import com.margins.session.mapper.ReadingSessionMapper;
import com.margins.session.mapper.ReadingSessionReviewMapper;
import com.margins.session.mapper.SessionHighlightMapper;
import com.margins.session.mapper.SessionInsightMapper;
import com.margins.session.mapper.SessionSearchMapper;
import com.margins.session.mapper.SessionTagMapper;
import com.margins.session.mapper.SessionWindowMapper;
import com.margins.session.model.ReadingSessionRecord;
import com.margins.session.model.ReadingSessionReviewRecord;
import com.margins.session.model.SessionHighlightRecord;
import com.margins.session.model.SessionInsightRecord;
import com.margins.session.model.SessionSearchResultRecord;
import com.margins.session.model.SessionTagRecord;
import com.margins.session.model.SessionWindowRecord;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class ReadingSessionBusiness {

    private static final long DEFAULT_USER_ID = 1L;
    private static final String ACTIVE_STATUS = "active";
    private static final String REVIEW_EDITOR_TYPE = "tiptap-free";
    private static final String REVIEW_DRAFT_STATUS = "draft";
    private static final Safelist REVIEW_HTML_SAFELIST = Safelist.relaxed()
        .addTags("h1", "h2", "h3")
        .addAttributes("img", "alt", "title")
        .addProtocols("img", "src", "http", "https");

    private final ReadingSessionMapper readingSessionMapper;
    private final ReadingSessionReviewMapper readingSessionReviewMapper;
    private final SessionWindowMapper sessionWindowMapper;
    private final SessionHighlightMapper sessionHighlightMapper;
    private final SessionInsightMapper sessionInsightMapper;
    private final SessionSearchMapper sessionSearchMapper;
    private final SessionTagMapper sessionTagMapper;
    private final MessageMapper messageMapper;
    private final QuestionMapper questionMapper;

    public CreateReadingSessionResponse create(CreateReadingSessionRequest request) {
        if (readingSessionMapper.countActiveBookById(request.getBookId(), DEFAULT_USER_ID) <= 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found");
        }

        ReadingSessionRecord record = ReadingSessionRecord.builder()
            .userId(DEFAULT_USER_ID)
            .bookId(request.getBookId())
            .title(request.getTitle())
            .status(ACTIVE_STATUS)
            .testData(true)
            .build();

        requireInserted(readingSessionMapper.insert(record), "Reading session could not be saved");

        return CreateReadingSessionResponse.builder()
            .sessionId(record.getId())
            .bookId(record.getBookId())
            .title(record.getTitle())
            .status(record.getStatus())
            .build();
    }

    public ReadingSessionTimelineResponse findLatestTimeline() {
        ReadingSessionRecord session = readingSessionMapper.findLatestByUserId(DEFAULT_USER_ID);
        return toTimeline(session);
    }

    public ReadingSessionTimelineResponse findTimeline(Long sessionId) {
        return toTimeline(readingSessionMapper.findByIdAndUserId(sessionId, DEFAULT_USER_ID));
    }

    public ReadingSessionListResponse findSummaries() {
        return ReadingSessionListResponse.builder()
            .sessions(readingSessionMapper.findSummariesByUserId(DEFAULT_USER_ID)
                .stream()
                .map(this::toSummaryDto)
                .toList())
            .build();
    }

    public ReadingLibraryStatsResponse findLibraryStats() {
        List<ReadingSessionSummaryDto> summaries = readingSessionMapper.findSummariesByUserId(DEFAULT_USER_ID)
            .stream()
            .map(this::toSummaryDto)
            .toList();
        int sessionCount = summaries.size();
        int completedCount = (int) summaries.stream()
            .filter((summary) -> "completed".equals(summary.getStatus()))
            .count();
        int progressCount = (int) summaries.stream()
            .map(ReadingSessionSummaryDto::getProgressPercent)
            .filter(Objects::nonNull)
            .count();
        int progressSum = summaries.stream()
            .map(ReadingSessionSummaryDto::getProgressPercent)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .sum();

        return ReadingLibraryStatsResponse.builder()
            .sessionCount(sessionCount)
            .activeSessionCount(sessionCount - completedCount)
            .completedSessionCount(completedCount)
            .distinctBookCount((int) summaries.stream().map(ReadingSessionSummaryDto::getBookId).distinct().count())
            .answeredQuestionCount(summaries.stream().mapToInt((summary) -> defaultInt(summary.getAnsweredQuestionCount())).sum())
            .highlightCount(summaries.stream().mapToInt((summary) -> defaultInt(summary.getHighlightCount())).sum())
            .messageCount(summaries.stream().mapToInt((summary) -> defaultInt(summary.getMessageCount())).sum())
            .averageProgressPercent(progressCount == 0 ? null : Math.round(progressSum * 1.0f / progressCount))
            .build();
    }

    public SessionSearchResponse search(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isEmpty()) {
            return SessionSearchResponse.builder()
                .query("")
                .results(List.of())
                .build();
        }

        return SessionSearchResponse.builder()
            .query(normalizedQuery)
            .results(sessionSearchMapper.search(DEFAULT_USER_ID, normalizedQuery, 30)
                .stream()
                .map(this::toSearchResultDto)
                .toList())
            .build();
    }

    public ReadingSessionTimelineResponse complete(Long sessionId, CompleteReadingSessionRequest request) {
        requireUpdated(readingSessionMapper.complete(sessionId, DEFAULT_USER_ID, request.getSummary()));
        return findTimeline(sessionId);
    }

    public ReadingSessionListResponse archive(Long sessionId) {
        requireUpdated(readingSessionMapper.softDelete(sessionId, DEFAULT_USER_ID));
        return findSummaries();
    }

    public ReadingSessionTimelineResponse updateTitle(Long sessionId, UpdateReadingSessionTitleRequest request) {
        requireUpdated(readingSessionMapper.updateTitle(sessionId, DEFAULT_USER_ID, request.getTitle()));
        return findTimeline(sessionId);
    }

    public ReadingSessionTimelineResponse updateProgress(Long sessionId, UpdateReadingSessionProgressRequest request) {
        requireUpdated(readingSessionMapper.updateProgress(
            sessionId,
            DEFAULT_USER_ID,
            request.getReadingGoal(),
            request.getStartPage(),
            request.getCurrentPage(),
            request.getTargetPage(),
            request.getProgressNote()
        ));
        return findTimeline(sessionId);
    }

    public ReadingSessionListResponse updatePinned(Long sessionId, UpdateReadingSessionPinRequest request) {
        requireUpdated(readingSessionMapper.updatePinned(sessionId, DEFAULT_USER_ID, request.getPinned()));
        return findSummaries();
    }

    public ReadingSessionTimelineResponse saveReview(Long sessionId, SaveReadingSessionReviewRequest request) {
        ReadingSessionRecord session = requireSession(sessionId);
        ReadingSessionReviewRecord existing = readingSessionReviewMapper.findBySessionId(sessionId, DEFAULT_USER_ID);
        String sanitizedContent = sanitizeReviewHtml(request.getContentHtml());
        ReadingSessionReviewRecord record = ReadingSessionReviewRecord.builder()
            .id(existing == null ? null : existing.getId())
            .sessionId(session.getId())
            .userId(DEFAULT_USER_ID)
            .title(request.getTitle().trim())
            .contentHtml(sanitizedContent)
            .editorType(REVIEW_EDITOR_TYPE)
            .status(normalizeReviewStatus(request.getStatus()))
            .testData(true)
            .build();

        if (existing == null) {
            requireInserted(readingSessionReviewMapper.insert(record), "Reading session review could not be saved");
        } else {
            requireUpdated(readingSessionReviewMapper.update(record), "Reading session review not found");
        }

        return findTimeline(sessionId);
    }

    public ReadingSessionTimelineResponse createHighlight(Long sessionId, CreateSessionHighlightRequest request) {
        ReadingSessionRecord session = requireSession(sessionId);

        SessionHighlightRecord record = SessionHighlightRecord.builder()
            .sessionId(session.getId())
            .bookId(session.getBookId())
            .userId(DEFAULT_USER_ID)
            .pageNumber(request.getPageNumber())
            .locationLabel(request.getLocationLabel())
            .quoteText(request.getQuoteText())
            .note(request.getNote())
            .highlightOrder(sessionHighlightMapper.selectNextOrder(session.getId()))
            .testData(true)
            .build();

        requireInserted(sessionHighlightMapper.insert(record), "Session highlight could not be saved");
        return findTimeline(sessionId);
    }

    public ReadingSessionTimelineResponse updateHighlight(Long sessionId, Long highlightId, UpdateSessionHighlightRequest request) {
        requireSession(sessionId);

        requireUpdated(sessionHighlightMapper.update(
            sessionId,
            highlightId,
            DEFAULT_USER_ID,
            request.getPageNumber(),
            request.getLocationLabel(),
            request.getQuoteText(),
            request.getNote()
        ), "Session highlight not found");
        return findTimeline(sessionId);
    }

    public ReadingSessionTimelineResponse deleteHighlight(Long sessionId, Long highlightId) {
        requireSession(sessionId);

        requireUpdated(sessionHighlightMapper.softDelete(sessionId, highlightId, DEFAULT_USER_ID), "Session highlight not found");
        return findTimeline(sessionId);
    }

    public ReadingSessionTimelineResponse createTag(Long sessionId, CreateSessionTagRequest request) {
        requireSession(sessionId);

        SessionTagRecord record = SessionTagRecord.builder()
            .sessionId(sessionId)
            .userId(DEFAULT_USER_ID)
            .label(request.getLabel().trim())
            .testData(true)
            .build();
        requireInserted(sessionTagMapper.insert(record), "Session tag could not be saved");
        return findTimeline(sessionId);
    }

    public ReadingSessionTimelineResponse deleteTag(Long sessionId, Long tagId) {
        requireSession(sessionId);
        requireUpdated(sessionTagMapper.softDelete(sessionId, tagId, DEFAULT_USER_ID), "Session tag not found");
        return findTimeline(sessionId);
    }

    public ReadingSessionTimelineResponse createInsight(Long sessionId, CreateSessionInsightRequest request) {
        requireSession(sessionId);

        SessionInsightRecord record = SessionInsightRecord.builder()
            .sessionId(sessionId)
            .userId(DEFAULT_USER_ID)
            .insightType(normalizeInsightType(request.getInsightType()))
            .title(trimToNull(request.getTitle()))
            .content(request.getContent().trim())
            .evidence(trimToNull(request.getEvidence()))
            .insightOrder(sessionInsightMapper.selectNextOrder(sessionId))
            .testData(true)
            .build();
        requireInserted(sessionInsightMapper.insert(record), "Session insight could not be saved");
        return findTimeline(sessionId);
    }

    public ReadingSessionTimelineResponse deleteInsight(Long sessionId, Long insightId) {
        requireSession(sessionId);
        requireUpdated(sessionInsightMapper.softDelete(sessionId, insightId, DEFAULT_USER_ID), "Session insight not found");
        return findTimeline(sessionId);
    }

    private ReadingSessionTimelineResponse toTimeline(ReadingSessionRecord session) {
        if (session == null) {
            return null;
        }

        List<SessionWindowTimelineDto> windows = sessionWindowMapper.findBySessionId(session.getId())
            .stream()
            .map(this::toWindowDto)
            .toList();
        List<SessionMessageDto> messages = messageMapper.findBySessionId(session.getId())
            .stream()
            .map(this::toMessageDto)
            .toList();
        List<SessionHighlightDto> highlights = sessionHighlightMapper.findBySessionId(session.getId())
            .stream()
            .map(this::toHighlightDto)
            .toList();
        List<SessionTagDto> tags = sessionTagMapper.findBySessionId(session.getId(), DEFAULT_USER_ID)
            .stream()
            .map(this::toTagDto)
            .toList();
        List<SessionInsightDto> insights = sessionInsightMapper.findBySessionId(session.getId(), DEFAULT_USER_ID)
            .stream()
            .map(this::toInsightDto)
            .toList();
        ReadingSessionReviewDto review = toReviewDto(readingSessionReviewMapper.findBySessionId(session.getId(), DEFAULT_USER_ID));
        List<QuestionDto> questions = questionMapper.findBySessionId(session.getId())
            .stream()
            .map(this::toQuestionDto)
            .toList();

        Integer progressPercent = toProgressPercent(session.getCurrentPage(), session.getTargetPage());
        ReadingSessionStatsDto stats = toStats(windows, questions, messages);

        return ReadingSessionTimelineResponse.builder()
            .sessionId(session.getId())
            .bookId(session.getBookId())
            .bookTitle(session.getBookTitle())
            .bookAuthor(session.getBookAuthor())
            .title(session.getTitle())
            .status(session.getStatus())
            .pinned(session.isPinned())
            .readingGoal(session.getReadingGoal())
            .startPage(session.getStartPage())
            .currentPage(session.getCurrentPage())
            .targetPage(session.getTargetPage())
            .progressPercent(progressPercent)
            .progressNote(session.getProgressNote())
            .summary(session.getSummary())
            .review(review)
            .stats(stats)
            .nextActions(toNextActions(session, progressPercent, windows, questions, messages, highlights, stats))
            .windows(windows)
            .highlights(highlights)
            .tags(tags)
            .insights(insights)
            .questions(questions)
            .messages(messages)
            .build();
    }

    private List<ReadingSessionNextActionDto> toNextActions(
        ReadingSessionRecord session,
        Integer progressPercent,
        List<SessionWindowTimelineDto> windows,
        List<QuestionDto> questions,
        List<SessionMessageDto> messages,
        List<SessionHighlightDto> highlights,
        ReadingSessionStatsDto stats
    ) {
        List<ReadingSessionNextActionDto> actions = new java.util.ArrayList<>();
        Long questionWindowId = windows.stream()
            .filter((window) -> "question".equals(window.getWindowType()))
            .map(SessionWindowTimelineDto::getWindowId)
            .findFirst()
            .orElse(windows.isEmpty() ? null : windows.get(0).getWindowId());
        Long debateWindowId = windows.stream()
            .filter((window) -> "debate".equals(window.getWindowType()))
            .map(SessionWindowTimelineDto::getWindowId)
            .findFirst()
            .orElse(null);

        if (progressPercent == null) {
            actions.add(nextAction(
                "set_progress",
                "Set reading progress",
                "Add a goal and page range so this session can track momentum.",
                null,
                null
            ));
        }

        QuestionDto openQuestion = firstOpenQuestion(questions, messages);
        if (questions.isEmpty()) {
            actions.add(nextAction(
                "generate_questions",
                "Generate reflection questions",
                "Create prompts for the current reflection window before writing answers.",
                questionWindowId,
                null
            ));
        } else if (openQuestion != null) {
            actions.add(nextAction(
                "answer_open_question",
                "Answer an open question",
                openQuestion.getQuestionText(),
                openQuestion.getWindowId(),
                openQuestion.getQuestionId()
            ));
        }

        if (highlights.isEmpty()) {
            actions.add(nextAction(
                "save_highlight",
                "Save a quote",
                "Capture a passage or note so the review has evidence.",
                null,
                null
            ));
        }

        if (stats.getPersonaResponseCount() == null || stats.getPersonaResponseCount() == 0) {
            actions.add(nextAction(
                "ask_persona",
                "Ask a persona",
                "Send one interpretation to the debate window for another perspective.",
                debateWindowId,
                null
            ));
        }

        if ("active".equals(session.getStatus()) && progressPercent != null && progressPercent >= 100) {
            actions.add(nextAction(
                "complete_session",
                "Complete this session",
                "Write a closeout summary now that the reading goal is fully progressed.",
                null,
                null
            ));
        }

        return actions;
    }

    private void requireUpdated(int updatedRows) {
        requireUpdated(updatedRows, "Reading session not found");
    }

    private void requireUpdated(int updatedRows, String reason) {
        if (updatedRows <= 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, reason);
        }
    }

    private void requireInserted(int insertedRows, String reason) {
        if (insertedRows <= 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, reason);
        }
    }

    private ReadingSessionRecord requireSession(Long sessionId) {
        ReadingSessionRecord session = readingSessionMapper.findByIdAndUserId(sessionId, DEFAULT_USER_ID);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reading session not found");
        }

        return session;
    }

    private QuestionDto firstOpenQuestion(List<QuestionDto> questions, List<SessionMessageDto> messages) {
        java.util.Set<Long> answeredQuestionIds = messages.stream()
            .filter((message) -> "user".equals(message.getRole()))
            .map(SessionMessageDto::getQuestionId)
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet());

        return questions.stream()
            .filter((question) -> !answeredQuestionIds.contains(question.getQuestionId()))
            .findFirst()
            .orElse(null);
    }

    private ReadingSessionNextActionDto nextAction(
        String actionId,
        String label,
        String detail,
        Long targetWindowId,
        Long targetQuestionId
    ) {
        return ReadingSessionNextActionDto.builder()
            .actionId(actionId)
            .label(label)
            .detail(detail)
            .targetWindowId(targetWindowId)
            .targetQuestionId(targetQuestionId)
            .build();
    }

    private ReadingSessionStatsDto toStats(
        List<SessionWindowTimelineDto> windows,
        List<QuestionDto> questions,
        List<SessionMessageDto> messages
    ) {
        long answeredQuestions = messages.stream()
            .filter((message) -> "user".equals(message.getRole()))
            .map(SessionMessageDto::getQuestionId)
            .filter(Objects::nonNull)
            .distinct()
            .count();
        long personaResponses = messages.stream()
            .filter((message) -> message.getPersonaId() != null)
            .count();
        long personas = messages.stream()
            .map(SessionMessageDto::getPersonaId)
            .filter(Objects::nonNull)
            .distinct()
            .count();

        return ReadingSessionStatsDto.builder()
            .windowCount(windows.size())
            .questionCount(questions.size())
            .answeredQuestionCount((int) answeredQuestions)
            .messageCount(messages.size())
            .personaResponseCount((int) personaResponses)
            .personaCount((int) personas)
            .build();
    }

    private ReadingSessionSummaryDto toSummaryDto(ReadingSessionRecord record) {
        return ReadingSessionSummaryDto.builder()
            .sessionId(record.getId())
            .bookId(record.getBookId())
            .bookTitle(record.getBookTitle())
            .bookAuthor(record.getBookAuthor())
            .title(record.getTitle())
            .status(record.getStatus())
            .pinned(record.isPinned())
            .readingGoal(record.getReadingGoal())
            .startPage(record.getStartPage())
            .currentPage(record.getCurrentPage())
            .targetPage(record.getTargetPage())
            .progressPercent(toProgressPercent(record.getCurrentPage(), record.getTargetPage()))
            .summary(record.getSummary())
            .windowCount(record.getWindowCount())
            .questionCount(record.getQuestionCount())
            .answeredQuestionCount(record.getAnsweredQuestionCount())
            .highlightCount(record.getHighlightCount())
            .messageCount(record.getMessageCount())
            .tags(sessionTagMapper.findBySessionId(record.getId(), DEFAULT_USER_ID).stream().map(this::toTagDto).toList())
            .build();
    }

    private Integer toProgressPercent(Integer currentPage, Integer targetPage) {
        if (currentPage == null || targetPage == null || targetPage <= 0) {
            return null;
        }

        int percent = Math.round((currentPage * 100.0f) / targetPage);
        return Math.max(0, Math.min(100, percent));
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String normalizeInsightType(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? "takeaway" : trimmed;
    }

    private String normalizeReviewStatus(String value) {
        String trimmed = trimToNull(value);
        if ("published".equals(trimmed)) {
            return "published";
        }
        return REVIEW_DRAFT_STATUS;
    }

    private String sanitizeReviewHtml(String value) {
        String cleaned = Jsoup.clean(value == null ? "" : value.trim(), REVIEW_HTML_SAFELIST).trim();
        if (cleaned.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reading session review content is empty");
        }
        return cleaned;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private SessionWindowTimelineDto toWindowDto(SessionWindowRecord record) {
        return SessionWindowTimelineDto.builder()
            .windowId(record.getId())
            .sessionId(record.getSessionId())
            .windowType(record.getWindowType())
            .title(record.getTitle())
            .position(record.getPosition())
            .status(record.getStatus())
            .build();
    }

    private SessionMessageDto toMessageDto(MessageRecord record) {
        return SessionMessageDto.builder()
            .messageId(record.getId())
            .sessionId(record.getSessionId())
            .windowId(record.getWindowId())
            .parentMessageId(record.getParentMessageId())
            .role(record.getRole())
            .content(record.getContent())
            .messageOrder(record.getMessageOrder())
            .aiModel(record.getAiModel())
            .personaId(record.getPersonaId())
            .questionId(record.getQuestionId())
            .streamingStatus(record.getStreamingStatus())
            .build();
    }

    private SessionHighlightDto toHighlightDto(SessionHighlightRecord record) {
        return SessionHighlightDto.builder()
            .highlightId(record.getId())
            .sessionId(record.getSessionId())
            .bookId(record.getBookId())
            .pageNumber(record.getPageNumber())
            .locationLabel(record.getLocationLabel())
            .quoteText(record.getQuoteText())
            .note(record.getNote())
            .highlightOrder(record.getHighlightOrder())
            .build();
    }

    private SessionTagDto toTagDto(SessionTagRecord record) {
        return SessionTagDto.builder()
            .tagId(record.getId())
            .sessionId(record.getSessionId())
            .label(record.getLabel())
            .build();
    }

    private SessionInsightDto toInsightDto(SessionInsightRecord record) {
        return SessionInsightDto.builder()
            .insightId(record.getId())
            .sessionId(record.getSessionId())
            .insightType(record.getInsightType())
            .title(record.getTitle())
            .content(record.getContent())
            .evidence(record.getEvidence())
            .insightOrder(record.getInsightOrder())
            .build();
    }

    private ReadingSessionReviewDto toReviewDto(ReadingSessionReviewRecord record) {
        if (record == null) {
            return null;
        }

        return ReadingSessionReviewDto.builder()
            .reviewId(record.getId())
            .sessionId(record.getSessionId())
            .title(record.getTitle())
            .contentHtml(record.getContentHtml())
            .editorType(record.getEditorType())
            .status(record.getStatus())
            .createdAt(record.getCreatedAt())
            .updatedAt(record.getUpdatedAt())
            .build();
    }

    private SessionSearchResultDto toSearchResultDto(SessionSearchResultRecord record) {
        return SessionSearchResultDto.builder()
            .sessionId(record.getSessionId())
            .sourceId(record.getSourceId())
            .resultType(record.getResultType())
            .bookTitle(record.getBookTitle())
            .sessionTitle(record.getSessionTitle())
            .snippet(record.getSnippet())
            .build();
    }

    private QuestionDto toQuestionDto(QuestionRecord record) {
        return QuestionDto.builder()
            .questionId(record.getId())
            .sessionId(record.getSessionId())
            .windowId(record.getWindowId())
            .questionText(record.getQuestionText())
            .questionType(record.getQuestionType())
            .status(record.getStatus())
            .aiModel(record.getAiModel())
            .build();
    }
}
