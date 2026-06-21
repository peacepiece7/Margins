package com.margins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.margins.message.mapper.MessageMapper;
import com.margins.message.model.MessageRecord;
import com.margins.question.mapper.QuestionMapper;
import com.margins.question.model.QuestionRecord;
import com.margins.session.business.ReadingSessionBusiness;
import com.margins.session.dto.CompleteReadingSessionRequest;
import com.margins.session.dto.CreateReadingSessionRequest;
import com.margins.session.dto.CreateReadingSessionResponse;
import com.margins.session.dto.CreateSessionHighlightRequest;
import com.margins.session.dto.CreateSessionInsightRequest;
import com.margins.session.dto.CreateSessionTagRequest;
import com.margins.session.dto.ReadingSessionTimelineResponse;
import com.margins.session.dto.UpdateSessionHighlightRequest;
import com.margins.session.dto.UpdateReadingSessionPinRequest;
import com.margins.session.dto.UpdateReadingSessionProgressRequest;
import com.margins.session.dto.UpdateReadingSessionTitleRequest;
import com.margins.session.mapper.ReadingSessionMapper;
import com.margins.session.mapper.SessionHighlightMapper;
import com.margins.session.mapper.SessionInsightMapper;
import com.margins.session.mapper.SessionSearchMapper;
import com.margins.session.mapper.SessionTagMapper;
import com.margins.session.mapper.SessionWindowMapper;
import com.margins.session.model.ReadingSessionRecord;
import com.margins.session.model.SessionHighlightRecord;
import com.margins.session.model.SessionInsightRecord;
import com.margins.session.model.SessionSearchResultRecord;
import com.margins.session.model.SessionTagRecord;
import com.margins.session.model.SessionWindowContext;
import com.margins.session.model.SessionWindowRecord;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ReadingSessionBusinessPersistenceTest {

    @Test
    void createPersistsAndReturnsGeneratedId() {
        FakeReadingSessionMapper mapper = new FakeReadingSessionMapper();
        ReadingSessionBusiness business = business(mapper, new FakeSessionHighlightMapper());

        CreateReadingSessionResponse response = business.create(CreateReadingSessionRequest.builder()
            .bookId(7L)
            .title("My Session")
            .build());

        assertThat(response.getSessionId()).isEqualTo(77L);
        assertThat(response.getBookId()).isEqualTo(7L);
        assertThat(response.getStatus()).isEqualTo("active");
        assertThat(mapper.inserted.getUserId()).isEqualTo(1L);
        assertThat(mapper.inserted.isTestData()).isTrue();
    }

    @Test
    void createRejectsMissingBookBeforeInsert() {
        FakeReadingSessionMapper mapper = new FakeReadingSessionMapper();
        mapper.activeBookCount = 0;
        ReadingSessionBusiness business = business(mapper, new FakeSessionHighlightMapper());

        assertNotFound("Book not found", () -> business.create(CreateReadingSessionRequest.builder()
            .bookId(404L)
            .title("Missing book session")
            .build()));
        assertThat(mapper.inserted).isNull();
    }

    @Test
    void createRejectsZeroRowInsert() {
        FakeReadingSessionMapper mapper = new FakeReadingSessionMapper();
        mapper.insertRows = 0;
        ReadingSessionBusiness business = business(mapper, new FakeSessionHighlightMapper());

        assertServerError("Reading session could not be saved", () -> business.create(CreateReadingSessionRequest.builder()
            .bookId(7L)
            .title("My Session")
            .build()));
    }

    @Test
    void findLatestTimelineReturnsSessionWindowsAndMessages() {
        ReadingSessionBusiness business = business(new FakeReadingSessionMapper(), new FakeSessionHighlightMapper());

        ReadingSessionTimelineResponse response = business.findLatestTimeline();

        assertThat(response.getSessionId()).isEqualTo(77L);
        assertThat(response.getBookTitle()).isEqualTo("Seed Book");
        assertThat(response.getWindows()).hasSize(1);
        assertThat(response.getHighlights()).hasSize(1);
        assertThat(response.getTags()).extracting("label").containsExactly("politics");
        assertThat(response.getInsights()).extracting("content").containsExactly("Power is staged before it is explained.");
        assertThat(response.getQuestions()).hasSize(1);
        assertThat(response.getMessages()).hasSize(1);
        assertThat(response.getStats().getQuestionCount()).isEqualTo(1);
        assertThat(response.getStats().getAnsweredQuestionCount()).isEqualTo(1);
        assertThat(response.getStats().getPersonaCount()).isZero();
        assertThat(response.getMessages().get(0).getContent()).isEqualTo("Saved reflection");
        assertThat(response.getNextActions()).extracting("actionId")
            .contains("set_progress", "ask_persona");
    }

    @Test
    void timelineNextActionsGuideEmptyActiveSession() {
        FakeReadingSessionMapper mapper = new FakeReadingSessionMapper();
        ReadingSessionBusiness business = business(
            mapper,
            new EmptySessionWindowMapper(),
            new EmptySessionHighlightMapper(),
            new FakeSessionInsightMapper(),
            new FakeSessionSearchMapper(),
            new FakeSessionTagMapper(),
            new EmptyMessageMapper(),
            new EmptyQuestionMapper()
        );

        ReadingSessionTimelineResponse response = business.findTimeline(77L);

        assertThat(response.getNextActions()).extracting("actionId").containsExactly(
            "set_progress",
            "generate_questions",
            "save_highlight",
            "ask_persona"
        );
    }

    @Test
    void timelineNextActionsIncludeCompletionForFullyProgressedActiveSession() {
        FakeReadingSessionMapper mapper = new FakeReadingSessionMapper();
        mapper.currentPage = 120;
        mapper.targetPage = 120;
        ReadingSessionBusiness business = business(mapper, new FakeSessionHighlightMapper());

        ReadingSessionTimelineResponse response = business.findTimeline(77L);

        assertThat(response.getProgressPercent()).isEqualTo(100);
        assertThat(response.getNextActions()).extracting("actionId").contains("complete_session");
    }

    @Test
    void findTimelineReturnsRequestedSession() {
        ReadingSessionBusiness business = business(new FakeReadingSessionMapper(), new FakeSessionHighlightMapper());

        ReadingSessionTimelineResponse response = business.findTimeline(77L);

        assertThat(response.getSessionId()).isEqualTo(77L);
        assertThat(response.getBookTitle()).isEqualTo("Seed Book");
    }

    @Test
    void findSummariesReturnsSessionLibrary() {
        ReadingSessionBusiness business = business(new FakeReadingSessionMapper(), new FakeSessionHighlightMapper());

        assertThat(business.findSummaries().getSessions()).hasSize(1);
        assertThat(business.findSummaries().getSessions().get(0).getMessageCount()).isEqualTo(2);
        assertThat(business.findSummaries().getSessions().get(0).getHighlightCount()).isEqualTo(1);
        assertThat(business.findSummaries().getSessions().get(0).getAnsweredQuestionCount()).isEqualTo(1);
        assertThat(business.findSummaries().getSessions().get(0).getProgressPercent()).isEqualTo(40);
        assertThat(business.findSummaries().getSessions().get(0).getTags()).extracting("label").containsExactly("politics");
    }

    @Test
    void findLibraryStatsAggregatesSavedSessionSummaries() {
        ReadingSessionBusiness business = business(new FakeReadingSessionMapper(), new FakeSessionHighlightMapper());

        assertThat(business.findLibraryStats())
            .satisfies((stats) -> {
                assertThat(stats.getSessionCount()).isEqualTo(1);
                assertThat(stats.getActiveSessionCount()).isEqualTo(1);
                assertThat(stats.getCompletedSessionCount()).isZero();
                assertThat(stats.getDistinctBookCount()).isEqualTo(1);
                assertThat(stats.getAnsweredQuestionCount()).isEqualTo(1);
                assertThat(stats.getHighlightCount()).isEqualTo(1);
                assertThat(stats.getMessageCount()).isEqualTo(2);
                assertThat(stats.getAverageProgressPercent()).isEqualTo(40);
            });
    }

    @Test
    void searchReturnsReadingMemoryMatches() {
        FakeSessionSearchMapper searchMapper = new FakeSessionSearchMapper();
        ReadingSessionBusiness business = business(new FakeReadingSessionMapper(), new FakeSessionHighlightMapper(), new FakeSessionInsightMapper(), searchMapper, new FakeSessionTagMapper());

        assertThat(business.search(" ceremony ").getResults()).singleElement()
            .satisfies((result) -> {
                assertThat(result.getSessionId()).isEqualTo(77L);
                assertThat(result.getResultType()).isEqualTo("insight");
                assertThat(result.getBookTitle()).isEqualTo("Seed Book");
                assertThat(result.getSnippet()).contains("ceremony");
            });
        assertThat(searchMapper.query).isEqualTo("ceremony");
        assertThat(searchMapper.limit).isEqualTo(30);
    }

    @Test
    void searchReturnsEmptyForBlankQuery() {
        FakeSessionSearchMapper searchMapper = new FakeSessionSearchMapper();
        ReadingSessionBusiness business = business(new FakeReadingSessionMapper(), new FakeSessionHighlightMapper(), new FakeSessionInsightMapper(), searchMapper, new FakeSessionTagMapper());

        assertThat(business.search(" ").getResults()).isEmpty();
        assertThat(searchMapper.query).isNull();
    }

    @Test
    void completeStoresSummaryAndReturnsCompletedTimeline() {
        FakeReadingSessionMapper mapper = new FakeReadingSessionMapper();
        ReadingSessionBusiness business = business(mapper, new FakeSessionHighlightMapper());

        ReadingSessionTimelineResponse response = business.complete(77L, CompleteReadingSessionRequest.builder()
            .summary("Finished with a clear theme.")
            .build());

        assertThat(response.getStatus()).isEqualTo("completed");
        assertThat(response.getSummary()).isEqualTo("Finished with a clear theme.");
        assertThat(mapper.completedSummary).isEqualTo("Finished with a clear theme.");
    }

    @Test
    void archiveSoftDeletesSessionAndReturnsUpdatedLibrary() {
        FakeReadingSessionMapper mapper = new FakeReadingSessionMapper();
        ReadingSessionBusiness business = business(mapper, new FakeSessionHighlightMapper());

        assertThat(business.archive(77L).getSessions()).isEmpty();

        assertThat(mapper.deletedSessionId).isEqualTo(77L);
        assertThat(mapper.deletedUserId).isEqualTo(1L);
    }

    @Test
    void updateTitleStoresSessionTitleAndReturnsTimeline() {
        FakeReadingSessionMapper mapper = new FakeReadingSessionMapper();
        ReadingSessionBusiness business = business(mapper, new FakeSessionHighlightMapper());

        ReadingSessionTimelineResponse response = business.updateTitle(77L, UpdateReadingSessionTitleRequest.builder()
            .title("Opening power notes")
            .build());

        assertThat(mapper.updatedTitle).isEqualTo("Opening power notes");
        assertThat(mapper.updatedTitleSessionId).isEqualTo(77L);
        assertThat(mapper.updatedTitleUserId).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Opening power notes");
    }

    @Test
    void updateProgressStoresGoalAndPagesOnTimeline() {
        FakeReadingSessionMapper mapper = new FakeReadingSessionMapper();
        ReadingSessionBusiness business = business(mapper, new FakeSessionHighlightMapper());

        ReadingSessionTimelineResponse response = business.updateProgress(77L, UpdateReadingSessionProgressRequest.builder()
            .readingGoal("Track how power shifts across the first act.")
            .startPage(1)
            .currentPage(64)
            .targetPage(120)
            .progressNote("The political stakes are clearer now.")
            .build());

        assertThat(response.getReadingGoal()).isEqualTo("Track how power shifts across the first act.");
        assertThat(response.getStartPage()).isEqualTo(1);
        assertThat(response.getCurrentPage()).isEqualTo(64);
        assertThat(response.getTargetPage()).isEqualTo(120);
        assertThat(response.getProgressPercent()).isEqualTo(53);
        assertThat(response.getProgressNote()).isEqualTo("The political stakes are clearer now.");
    }

    @Test
    void updatePinnedStoresPinnedStateAndReturnsLibrary() {
        FakeReadingSessionMapper mapper = new FakeReadingSessionMapper();
        ReadingSessionBusiness business = business(mapper, new FakeSessionHighlightMapper());

        assertThat(business.updatePinned(77L, UpdateReadingSessionPinRequest.builder()
                .pinned(true)
                .build())
            .getSessions()
            .get(0)
            .isPinned()).isTrue();

        assertThat(mapper.updatedPinnedSessionId).isEqualTo(77L);
        assertThat(mapper.updatedPinnedUserId).isEqualTo(1L);
        assertThat(mapper.pinned).isTrue();
    }

    @Test
    void missingSessionMutationsReturnNotFound() {
        FakeReadingSessionMapper mapper = new FakeReadingSessionMapper();
        mapper.updatedRows = 0;
        ReadingSessionBusiness business = business(mapper, new FakeSessionHighlightMapper());

        assertNotFound("Reading session not found", () -> business.complete(404L, CompleteReadingSessionRequest.builder()
            .summary("Cannot complete missing session.")
            .build()));
        assertNotFound("Reading session not found", () -> business.archive(404L));
        assertNotFound("Reading session not found", () -> business.updateTitle(404L, UpdateReadingSessionTitleRequest.builder()
            .title("Missing")
            .build()));
        assertNotFound("Reading session not found", () -> business.updateProgress(404L, UpdateReadingSessionProgressRequest.builder()
            .currentPage(1)
            .targetPage(10)
            .build()));
        assertNotFound("Reading session not found", () -> business.updatePinned(404L, UpdateReadingSessionPinRequest.builder()
            .pinned(true)
            .build()));
    }

    private void assertNotFound(String reason, Runnable action) {
        assertThatThrownBy(action::run)
            .isInstanceOf(ResponseStatusException.class)
            .satisfies((exception) -> {
                ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(responseStatusException.getReason()).isEqualTo(reason);
            });
    }

    private void assertServerError(String reason, Runnable action) {
        assertThatThrownBy(action::run)
            .isInstanceOf(ResponseStatusException.class)
            .satisfies((exception) -> {
                ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                assertThat(responseStatusException.getReason()).isEqualTo(reason);
            });
    }

    @Test
    void createHighlightStoresQuoteAndReturnsTimeline() {
        FakeReadingSessionMapper mapper = new FakeReadingSessionMapper();
        FakeSessionHighlightMapper highlightMapper = new FakeSessionHighlightMapper();
        ReadingSessionBusiness business = business(mapper, highlightMapper);

        ReadingSessionTimelineResponse response = business.createHighlight(77L, CreateSessionHighlightRequest.builder()
            .pageNumber(18)
            .locationLabel("Chapter 2")
            .quoteText("Fear is the mind-killer.")
            .note("Use this as evidence for discipline.")
            .build());

        assertThat(highlightMapper.inserted.getSessionId()).isEqualTo(77L);
        assertThat(highlightMapper.inserted.getBookId()).isEqualTo(7L);
        assertThat(highlightMapper.inserted.getHighlightOrder()).isEqualTo(2);
        assertThat(response.getHighlights()).extracting("quoteText").contains("Fear is the mind-killer.");
    }

    @Test
    void createHighlightRejectsZeroRowInsert() {
        FakeSessionHighlightMapper highlightMapper = new FakeSessionHighlightMapper();
        highlightMapper.insertRows = 0;
        ReadingSessionBusiness business = business(new FakeReadingSessionMapper(), highlightMapper);

        assertServerError("Session highlight could not be saved", () -> business.createHighlight(77L, CreateSessionHighlightRequest.builder()
            .quoteText("Fear is the mind-killer.")
            .build()));
    }

    @Test
    void updateHighlightStoresEditedQuoteAndReturnsTimeline() {
        FakeSessionHighlightMapper highlightMapper = new FakeSessionHighlightMapper();
        ReadingSessionBusiness business = business(new FakeReadingSessionMapper(), highlightMapper);

        ReadingSessionTimelineResponse response = business.updateHighlight(77L, 100L, UpdateSessionHighlightRequest.builder()
            .pageNumber(19)
            .locationLabel("Chapter 2 revised")
            .quoteText("Fear is the mind-killer, revised.")
            .note("Corrected evidence note.")
            .build());

        assertThat(highlightMapper.updatedSessionId).isEqualTo(77L);
        assertThat(highlightMapper.updatedHighlightId).isEqualTo(100L);
        assertThat(highlightMapper.updatedUserId).isEqualTo(1L);
        assertThat(response.getHighlights()).singleElement()
            .satisfies((highlight) -> {
                assertThat(highlight.getPageNumber()).isEqualTo(19);
                assertThat(highlight.getLocationLabel()).isEqualTo("Chapter 2 revised");
                assertThat(highlight.getQuoteText()).isEqualTo("Fear is the mind-killer, revised.");
                assertThat(highlight.getNote()).isEqualTo("Corrected evidence note.");
            });
    }

    @Test
    void deleteHighlightSoftDeletesQuoteAndReturnsTimeline() {
        FakeSessionHighlightMapper highlightMapper = new FakeSessionHighlightMapper();
        ReadingSessionBusiness business = business(new FakeReadingSessionMapper(), highlightMapper);

        ReadingSessionTimelineResponse response = business.deleteHighlight(77L, 100L);

        assertThat(highlightMapper.deletedSessionId).isEqualTo(77L);
        assertThat(highlightMapper.deletedHighlightId).isEqualTo(100L);
        assertThat(highlightMapper.deletedUserId).isEqualTo(1L);
        assertThat(response.getHighlights()).isEmpty();
    }

    @Test
    void createTagStoresTrimmedLabelAndReturnsTimeline() {
        FakeSessionTagMapper tagMapper = new FakeSessionTagMapper();
        ReadingSessionBusiness business = business(new FakeReadingSessionMapper(), new FakeSessionHighlightMapper(), tagMapper);

        ReadingSessionTimelineResponse response = business.createTag(77L, CreateSessionTagRequest.builder()
            .label("  theme  ")
            .build());

        assertThat(tagMapper.inserted.getSessionId()).isEqualTo(77L);
        assertThat(tagMapper.inserted.getUserId()).isEqualTo(1L);
        assertThat(tagMapper.inserted.getLabel()).isEqualTo("theme");
        assertThat(tagMapper.inserted.isTestData()).isTrue();
        assertThat(response.getTags()).extracting("label").contains("theme");
    }

    @Test
    void createTagRejectsZeroRowInsert() {
        FakeSessionTagMapper tagMapper = new FakeSessionTagMapper();
        tagMapper.insertRows = 0;
        ReadingSessionBusiness business = business(new FakeReadingSessionMapper(), new FakeSessionHighlightMapper(), tagMapper);

        assertServerError("Session tag could not be saved", () -> business.createTag(77L, CreateSessionTagRequest.builder()
            .label("theme")
            .build()));
    }

    @Test
    void deleteTagSoftDeletesLabelAndReturnsTimeline() {
        FakeSessionTagMapper tagMapper = new FakeSessionTagMapper();
        ReadingSessionBusiness business = business(new FakeReadingSessionMapper(), new FakeSessionHighlightMapper(), tagMapper);

        ReadingSessionTimelineResponse response = business.deleteTag(77L, 55L);

        assertThat(tagMapper.deletedSessionId).isEqualTo(77L);
        assertThat(tagMapper.deletedTagId).isEqualTo(55L);
        assertThat(tagMapper.deletedUserId).isEqualTo(1L);
        assertThat(response.getTags()).isEmpty();
    }

    @Test
    void createInsightStoresReviewTakeawayAndReturnsTimeline() {
        FakeSessionInsightMapper insightMapper = new FakeSessionInsightMapper();
        ReadingSessionBusiness business = business(new FakeReadingSessionMapper(), new FakeSessionHighlightMapper(), insightMapper, new FakeSessionTagMapper());

        ReadingSessionTimelineResponse response = business.createInsight(77L, CreateSessionInsightRequest.builder()
            .title("  Ritual before politics  ")
            .content("  Power is introduced through ceremony before exposition.  ")
            .evidence("  Gom Jabbar scene  ")
            .build());

        assertThat(insightMapper.inserted.getSessionId()).isEqualTo(77L);
        assertThat(insightMapper.inserted.getUserId()).isEqualTo(1L);
        assertThat(insightMapper.inserted.getInsightType()).isEqualTo("takeaway");
        assertThat(insightMapper.inserted.getTitle()).isEqualTo("Ritual before politics");
        assertThat(insightMapper.inserted.getContent()).isEqualTo("Power is introduced through ceremony before exposition.");
        assertThat(insightMapper.inserted.getEvidence()).isEqualTo("Gom Jabbar scene");
        assertThat(insightMapper.inserted.getInsightOrder()).isEqualTo(2);
        assertThat(insightMapper.inserted.isTestData()).isTrue();
        assertThat(response.getInsights()).extracting("content").contains("Power is introduced through ceremony before exposition.");
    }

    @Test
    void createInsightRejectsZeroRowInsert() {
        FakeSessionInsightMapper insightMapper = new FakeSessionInsightMapper();
        insightMapper.insertRows = 0;
        ReadingSessionBusiness business = business(new FakeReadingSessionMapper(), new FakeSessionHighlightMapper(), insightMapper, new FakeSessionTagMapper());

        assertServerError("Session insight could not be saved", () -> business.createInsight(77L, CreateSessionInsightRequest.builder()
            .content("Power is introduced through ceremony before exposition.")
            .build()));
    }

    @Test
    void deleteInsightSoftDeletesReviewTakeawayAndReturnsTimeline() {
        FakeSessionInsightMapper insightMapper = new FakeSessionInsightMapper();
        ReadingSessionBusiness business = business(new FakeReadingSessionMapper(), new FakeSessionHighlightMapper(), insightMapper, new FakeSessionTagMapper());

        ReadingSessionTimelineResponse response = business.deleteInsight(77L, 66L);

        assertThat(insightMapper.deletedSessionId).isEqualTo(77L);
        assertThat(insightMapper.deletedInsightId).isEqualTo(66L);
        assertThat(insightMapper.deletedUserId).isEqualTo(1L);
        assertThat(response.getInsights()).isEmpty();
    }

    @Test
    void childRecordMutationsReturnNotFoundWhenRowsAreMissing() {
        FakeSessionHighlightMapper highlightMapper = new FakeSessionHighlightMapper();
        highlightMapper.updatedRows = 0;
        highlightMapper.deletedRows = 0;
        FakeSessionTagMapper tagMapper = new FakeSessionTagMapper();
        tagMapper.deletedRows = 0;
        FakeSessionInsightMapper insightMapper = new FakeSessionInsightMapper();
        insightMapper.deletedRows = 0;
        ReadingSessionBusiness business = business(new FakeReadingSessionMapper(), highlightMapper, insightMapper, tagMapper);

        assertNotFound("Session highlight not found", () -> business.updateHighlight(77L, 999L, UpdateSessionHighlightRequest.builder()
            .quoteText("Missing")
            .build()));
        assertNotFound("Session highlight not found", () -> business.deleteHighlight(77L, 999L));
        assertNotFound("Session tag not found", () -> business.deleteTag(77L, 999L));
        assertNotFound("Session insight not found", () -> business.deleteInsight(77L, 999L));
    }

    @Test
    void childRecordWritesReturnNotFoundWhenParentSessionIsMissing() {
        FakeReadingSessionMapper mapper = new FakeReadingSessionMapper();
        mapper.sessionMissing = true;
        ReadingSessionBusiness business = business(mapper, new FakeSessionHighlightMapper());

        assertNotFound("Reading session not found", () -> business.createHighlight(404L, CreateSessionHighlightRequest.builder()
            .quoteText("Missing parent")
            .build()));
        assertNotFound("Reading session not found", () -> business.updateHighlight(404L, 100L, UpdateSessionHighlightRequest.builder()
            .quoteText("Missing parent")
            .build()));
        assertNotFound("Reading session not found", () -> business.deleteHighlight(404L, 100L));
        assertNotFound("Reading session not found", () -> business.createTag(404L, CreateSessionTagRequest.builder()
            .label("missing")
            .build()));
        assertNotFound("Reading session not found", () -> business.deleteTag(404L, 55L));
        assertNotFound("Reading session not found", () -> business.createInsight(404L, CreateSessionInsightRequest.builder()
            .content("Missing parent")
            .build()));
        assertNotFound("Reading session not found", () -> business.deleteInsight(404L, 66L));
    }

    private ReadingSessionBusiness business(
        FakeReadingSessionMapper readingSessionMapper,
        FakeSessionHighlightMapper sessionHighlightMapper
    ) {
        return business(readingSessionMapper, sessionHighlightMapper, new FakeSessionInsightMapper(), new FakeSessionTagMapper());
    }

    private ReadingSessionBusiness business(
        FakeReadingSessionMapper readingSessionMapper,
        FakeSessionHighlightMapper sessionHighlightMapper,
        FakeSessionTagMapper sessionTagMapper
    ) {
        return business(readingSessionMapper, sessionHighlightMapper, new FakeSessionInsightMapper(), sessionTagMapper);
    }

    private ReadingSessionBusiness business(
        FakeReadingSessionMapper readingSessionMapper,
        FakeSessionHighlightMapper sessionHighlightMapper,
        FakeSessionInsightMapper sessionInsightMapper,
        FakeSessionTagMapper sessionTagMapper
    ) {
        return business(readingSessionMapper, sessionHighlightMapper, sessionInsightMapper, new FakeSessionSearchMapper(), sessionTagMapper);
    }

    private ReadingSessionBusiness business(
        FakeReadingSessionMapper readingSessionMapper,
        FakeSessionHighlightMapper sessionHighlightMapper,
        FakeSessionInsightMapper sessionInsightMapper,
        FakeSessionSearchMapper sessionSearchMapper,
        FakeSessionTagMapper sessionTagMapper
    ) {
        return new ReadingSessionBusiness(
            readingSessionMapper,
            new FakeSessionWindowMapper(),
            sessionHighlightMapper,
            sessionInsightMapper,
            sessionSearchMapper,
            sessionTagMapper,
            new FakeMessageMapper(),
            new FakeQuestionMapper()
        );
    }

    private ReadingSessionBusiness business(
        FakeReadingSessionMapper readingSessionMapper,
        SessionWindowMapper sessionWindowMapper,
        SessionHighlightMapper sessionHighlightMapper,
        FakeSessionInsightMapper sessionInsightMapper,
        FakeSessionSearchMapper sessionSearchMapper,
        FakeSessionTagMapper sessionTagMapper,
        MessageMapper messageMapper,
        QuestionMapper questionMapper
    ) {
        return new ReadingSessionBusiness(
            readingSessionMapper,
            sessionWindowMapper,
            sessionHighlightMapper,
            sessionInsightMapper,
            sessionSearchMapper,
            sessionTagMapper,
            messageMapper,
            questionMapper
        );
    }

    private static class FakeReadingSessionMapper implements ReadingSessionMapper {
        private ReadingSessionRecord inserted;
        private String completedSummary;
        private Long deletedSessionId;
        private Long deletedUserId;
        private Long updatedTitleSessionId;
        private Long updatedTitleUserId;
        private String updatedTitle;
        private String readingGoal;
        private Integer startPage;
        private Integer currentPage;
        private Integer targetPage;
        private String progressNote;
        private Long updatedPinnedSessionId;
        private Long updatedPinnedUserId;
        private boolean pinned;
        private int insertRows = 1;
        private int updatedRows = 1;
        private int activeBookCount = 1;
        private boolean sessionMissing;

        @Override
        public int countActiveBookById(Long bookId, Long userId) {
            return activeBookCount;
        }

        @Override
        public int insert(ReadingSessionRecord record) {
            this.inserted = record;
            record.setId(77L);
            return insertRows;
        }

        @Override
        public ReadingSessionRecord findLatestByUserId(Long userId) {
            if (sessionMissing) {
                return null;
            }

            return sessionRecord(userId);
        }

        @Override
        public List<ReadingSessionRecord> findSummariesByUserId(Long userId) {
            if (deletedSessionId != null) {
                return List.of();
            }

            return List.of(ReadingSessionRecord.builder()
                .id(77L)
                .userId(userId)
                .bookId(7L)
                .bookTitle("Seed Book")
                .bookAuthor("Seed Author")
                .title(updatedTitle == null ? "My Session" : updatedTitle)
                .status("active")
                .pinned(pinned)
                .startPage(1)
                .currentPage(48)
                .targetPage(120)
                .windowCount(1)
                .questionCount(1)
                .answeredQuestionCount(1)
                .highlightCount(1)
                .messageCount(2)
                .build());
        }

        @Override
        public ReadingSessionRecord findByIdAndUserId(Long sessionId, Long userId) {
            if (sessionMissing) {
                return null;
            }

            return sessionRecord(userId);
        }

        @Override
        public int complete(Long sessionId, Long userId, String summary) {
            this.completedSummary = summary;
            return updatedRows;
        }

        @Override
        public int softDelete(Long sessionId, Long userId) {
            this.deletedSessionId = sessionId;
            this.deletedUserId = userId;
            return updatedRows;
        }

        @Override
        public int updateTitle(Long sessionId, Long userId, String title) {
            this.updatedTitleSessionId = sessionId;
            this.updatedTitleUserId = userId;
            this.updatedTitle = title;
            return updatedRows;
        }

        @Override
        public int updateProgress(
            Long sessionId,
            Long userId,
            String readingGoal,
            Integer startPage,
            Integer currentPage,
            Integer targetPage,
            String progressNote
        ) {
            this.readingGoal = readingGoal;
            this.startPage = startPage;
            this.currentPage = currentPage;
            this.targetPage = targetPage;
            this.progressNote = progressNote;
            return updatedRows;
        }

        @Override
        public int updatePinned(Long sessionId, Long userId, boolean pinned) {
            this.updatedPinnedSessionId = sessionId;
            this.updatedPinnedUserId = userId;
            this.pinned = pinned;
            return updatedRows;
        }

        private ReadingSessionRecord sessionRecord(Long userId) {
            return ReadingSessionRecord.builder()
                .id(77L)
                .userId(userId)
                .bookId(7L)
                .bookTitle("Seed Book")
                .bookAuthor("Seed Author")
                .title(updatedTitle == null ? "My Session" : updatedTitle)
                .status(completedSummary == null ? "active" : "completed")
                .pinned(pinned)
                .readingGoal(readingGoal)
                .startPage(startPage)
                .currentPage(currentPage)
                .targetPage(targetPage)
                .progressNote(progressNote)
                .summary(completedSummary)
                .build();
        }
    }

    private static class FakeSessionWindowMapper implements SessionWindowMapper {
        @Override
        public int insert(SessionWindowRecord record) {
            return 1;
        }

        @Override
        public SessionWindowRecord findById(Long id) {
            return SessionWindowRecord.builder()
                .id(id)
                .sessionId(77L)
                .windowType("question")
                .title("Question")
                .position(1)
                .status("open")
                .build();
        }

        @Override
        public SessionWindowContext findContextById(Long id) {
            return null;
        }

        @Override
        public int updateTitle(Long windowId, String title) {
            return 1;
        }

        @Override
        public int softDelete(Long windowId) {
            return 1;
        }

        @Override
        public int countActiveBySessionId(Long sessionId) {
            return findBySessionId(sessionId).size();
        }

        @Override
        public int countActiveSessionById(Long sessionId, Long userId) {
            return 1;
        }

        @Override
        public int selectNextPosition(Long sessionId) {
            return 1;
        }

        @Override
        public List<SessionWindowRecord> findBySessionId(Long sessionId) {
            return List.of(SessionWindowRecord.builder()
                .id(88L)
                .sessionId(sessionId)
                .windowType("question")
                .title("Question")
                .position(1)
                .status("open")
                .build());
        }
    }

    private static class EmptySessionWindowMapper extends FakeSessionWindowMapper {
        @Override
        public List<SessionWindowRecord> findBySessionId(Long sessionId) {
            return List.of();
        }
    }

    private static class FakeSessionHighlightMapper implements SessionHighlightMapper {
        private SessionHighlightRecord inserted;
        private Long updatedSessionId;
        private Long updatedHighlightId;
        private Long updatedUserId;
        private Integer updatedPageNumber;
        private String updatedLocationLabel;
        private String updatedQuoteText;
        private String updatedNote;
        private Long deletedSessionId;
        private Long deletedHighlightId;
        private Long deletedUserId;
        private int insertRows = 1;
        private int updatedRows = 1;
        private int deletedRows = 1;

        @Override
        public int insert(SessionHighlightRecord record) {
            if (insertRows <= 0) {
                return insertRows;
            }
            this.inserted = record;
            record.setId(101L);
            return insertRows;
        }

        @Override
        public int selectNextOrder(Long sessionId) {
            return 2;
        }

        @Override
        public List<SessionHighlightRecord> findBySessionId(Long sessionId) {
            if (deletedHighlightId != null) {
                return List.of();
            }
            if (updatedHighlightId != null) {
                return List.of(SessionHighlightRecord.builder()
                    .id(updatedHighlightId)
                    .sessionId(sessionId)
                    .bookId(7L)
                    .pageNumber(updatedPageNumber)
                    .locationLabel(updatedLocationLabel)
                    .quoteText(updatedQuoteText)
                    .note(updatedNote)
                    .highlightOrder(1)
                    .build());
            }

            if (inserted != null) {
                return List.of(
                    SessionHighlightRecord.builder()
                        .id(100L)
                        .sessionId(sessionId)
                        .bookId(7L)
                        .pageNumber(12)
                        .locationLabel("Chapter 1")
                        .quoteText("Saved highlight")
                        .note("Existing note")
                        .highlightOrder(1)
                        .build(),
                    inserted
                );
            }

            return List.of(SessionHighlightRecord.builder()
                .id(100L)
                .sessionId(sessionId)
                .bookId(7L)
                .pageNumber(12)
                .locationLabel("Chapter 1")
                .quoteText("Saved highlight")
                .note("Existing note")
                .highlightOrder(1)
                .build());
        }

        @Override
        public int update(
            Long sessionId,
            Long highlightId,
            Long userId,
            Integer pageNumber,
            String locationLabel,
            String quoteText,
            String note
        ) {
            this.updatedSessionId = sessionId;
            this.updatedHighlightId = highlightId;
            this.updatedUserId = userId;
            this.updatedPageNumber = pageNumber;
            this.updatedLocationLabel = locationLabel;
            this.updatedQuoteText = quoteText;
            this.updatedNote = note;
            return updatedRows;
        }

        @Override
        public int softDelete(Long sessionId, Long highlightId, Long userId) {
            this.deletedSessionId = sessionId;
            this.deletedHighlightId = highlightId;
            this.deletedUserId = userId;
            return deletedRows;
        }
    }

    private static class EmptySessionHighlightMapper extends FakeSessionHighlightMapper {
        @Override
        public List<SessionHighlightRecord> findBySessionId(Long sessionId) {
            return List.of();
        }
    }

    private static class FakeSessionTagMapper implements SessionTagMapper {
        private SessionTagRecord inserted;
        private Long deletedSessionId;
        private Long deletedTagId;
        private Long deletedUserId;
        private int insertRows = 1;
        private int deletedRows = 1;

        @Override
        public int insert(SessionTagRecord record) {
            if (insertRows <= 0) {
                return insertRows;
            }
            this.inserted = record;
            record.setId(56L);
            return insertRows;
        }

        @Override
        public List<SessionTagRecord> findBySessionId(Long sessionId, Long userId) {
            if (deletedTagId != null) {
                return List.of();
            }
            if (inserted != null) {
                return List.of(
                    SessionTagRecord.builder()
                        .id(55L)
                        .sessionId(sessionId)
                        .userId(userId)
                        .label("politics")
                        .build(),
                    inserted
                );
            }

            return List.of(SessionTagRecord.builder()
                .id(55L)
                .sessionId(sessionId)
                .userId(userId)
                .label("politics")
                .build());
        }

        @Override
        public List<SessionTagRecord> findBySessionIds(List<Long> sessionIds, Long userId) {
            return sessionIds.stream()
                .flatMap((sessionId) -> findBySessionId(sessionId, userId).stream())
                .toList();
        }

        @Override
        public int softDelete(Long sessionId, Long tagId, Long userId) {
            this.deletedSessionId = sessionId;
            this.deletedTagId = tagId;
            this.deletedUserId = userId;
            return deletedRows;
        }
    }

    private static class FakeSessionInsightMapper implements SessionInsightMapper {
        private SessionInsightRecord inserted;
        private Long deletedSessionId;
        private Long deletedInsightId;
        private Long deletedUserId;
        private int insertRows = 1;
        private int deletedRows = 1;

        @Override
        public int insert(SessionInsightRecord record) {
            if (insertRows <= 0) {
                return insertRows;
            }
            this.inserted = record;
            record.setId(67L);
            return insertRows;
        }

        @Override
        public int selectNextOrder(Long sessionId) {
            return 2;
        }

        @Override
        public List<SessionInsightRecord> findBySessionId(Long sessionId, Long userId) {
            if (deletedInsightId != null) {
                return List.of();
            }
            if (inserted != null) {
                return List.of(
                    SessionInsightRecord.builder()
                        .id(66L)
                        .sessionId(sessionId)
                        .userId(userId)
                        .insightType("takeaway")
                        .title("Ritual and power")
                        .content("Power is staged before it is explained.")
                        .evidence("Opening ceremony")
                        .insightOrder(1)
                        .build(),
                    inserted
                );
            }

            return List.of(SessionInsightRecord.builder()
                .id(66L)
                .sessionId(sessionId)
                .userId(userId)
                .insightType("takeaway")
                .title("Ritual and power")
                .content("Power is staged before it is explained.")
                .evidence("Opening ceremony")
                .insightOrder(1)
                .build());
        }

        @Override
        public int softDelete(Long sessionId, Long insightId, Long userId) {
            this.deletedSessionId = sessionId;
            this.deletedInsightId = insightId;
            this.deletedUserId = userId;
            return deletedRows;
        }
    }

    private static class FakeSessionSearchMapper implements SessionSearchMapper {
        private String query;
        private int limit;

        @Override
        public List<SessionSearchResultRecord> search(Long userId, String query, int limit) {
            this.query = query;
            this.limit = limit;
            return List.of(SessionSearchResultRecord.builder()
                .sessionId(77L)
                .sourceId(66L)
                .resultType("insight")
                .bookTitle("Seed Book")
                .sessionTitle("My Session")
                .snippet("Power becomes visible through ceremony.")
                .build());
        }
    }

    private static class FakeMessageMapper implements MessageMapper {
        @Override
        public int insert(MessageRecord record) {
            return 1;
        }

        @Override
        public int selectNextOrder(Long sessionId, Long windowId) {
            return 1;
        }

        @Override
        public List<MessageRecord> findBySessionId(Long sessionId) {
            return List.of(MessageRecord.builder()
                .id(99L)
                .sessionId(sessionId)
                .windowId(88L)
                .role("user")
                .content("Saved reflection")
                .questionId(42L)
                .messageOrder(1)
                .streamingStatus("complete")
                .build());
        }

        @Override
        public MessageRecord findEditableById(Long messageId, Long userId) {
            return null;
        }

        @Override
        public int updateContent(Long messageId, Long userId, String content) {
            return 1;
        }

        @Override
        public int softDelete(Long messageId, Long userId) {
            return 1;
        }
    }

    private static class EmptyMessageMapper extends FakeMessageMapper {
        @Override
        public List<MessageRecord> findBySessionId(Long sessionId) {
            return List.of();
        }
    }

    private static class FakeQuestionMapper implements QuestionMapper {
        @Override
        public int insert(QuestionRecord record) {
            return 1;
        }

        @Override
        public List<QuestionRecord> findBySessionId(Long sessionId) {
            return List.of(QuestionRecord.builder()
                .id(42L)
                .sessionId(sessionId)
                .windowId(88L)
                .questionText("What changed?")
                .questionType("reflection")
                .status("active")
                .aiModel("placeholder")
                .build());
        }

        @Override
        public List<QuestionRecord> findByWindowId(Long windowId) {
            return List.of();
        }

        @Override
        public QuestionRecord findActiveById(Long questionId, Long userId) {
            return null;
        }

        @Override
        public int countActiveUserAnswers(Long questionId) {
            return 0;
        }

        @Override
        public int softDelete(Long questionId, Long userId) {
            return 1;
        }
    }

    private static class EmptyQuestionMapper extends FakeQuestionMapper {
        @Override
        public List<QuestionRecord> findBySessionId(Long sessionId) {
            return List.of();
        }
    }
}
