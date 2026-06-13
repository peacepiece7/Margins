package com.margins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.margins.ai.AiProvider;
import com.margins.book.business.BookBusiness;
import com.margins.book.dto.BookCandidateDto;
import com.margins.book.dto.BookCandidateSearchResponse;
import com.margins.book.dto.BookCandidateSearchRequest;
import com.margins.book.dto.SaveBookRequest;
import com.margins.book.dto.SaveBookResponse;
import com.margins.book.mapper.BookMapper;
import com.margins.book.model.BookRecord;
import com.margins.question.dto.GenerateQuestionsRequest;
import com.margins.question.dto.QuestionListResponse;
import com.margins.session.dto.AiMessageResponse;
import com.margins.session.dto.DebateMessageRequest;
import com.margins.session.dto.SendMessageRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class BookBusinessPersistenceTest {

    @Test
    void saveBookPersistsAndReturnsGeneratedId() {
        FakeBookMapper mapper = new FakeBookMapper();
        BookBusiness business = new BookBusiness(new NoopAiProvider(), mapper);

        SaveBookResponse response = business.saveBook(SaveBookRequest.builder()
            .candidateId("candidate-1")
            .title("  Persisted Book  ")
            .author("  Reader  ")
            .publishedYear(2026)
            .build());

        assertThat(response.getBookId()).isEqualTo(42L);
        assertThat(response.getTitle()).isEqualTo("Persisted Book");
        assertThat(mapper.inserted.getUserId()).isEqualTo(1L);
        assertThat(mapper.inserted.getSource()).isEqualTo("ai");
        assertThat(mapper.inserted.getSourceRef()).isEqualTo("candidate-1");
    }

    @Test
    void saveBookReusesExistingBookWithSameTitleAndAuthor() {
        FakeBookMapper mapper = new FakeBookMapper();
        mapper.duplicate = BookRecord.builder()
            .id(99L)
            .userId(1L)
            .title("Dune")
            .author("AI Candidate")
            .build();
        BookBusiness business = new BookBusiness(new NoopAiProvider(), mapper);

        SaveBookResponse response = business.saveBook(SaveBookRequest.builder()
            .candidateId("candidate-duplicate")
            .title(" dune ")
            .author(" ai candidate ")
            .publishedYear(1965)
            .build());

        assertThat(response.getBookId()).isEqualTo(99L);
        assertThat(response.getTitle()).isEqualTo("Dune");
        assertThat(mapper.inserted).isNull();
        assertThat(mapper.duplicateLookupTitle).isEqualTo("dune");
        assertThat(mapper.duplicateLookupAuthor).isEqualTo("ai candidate");
    }

    @Test
    void saveBookRejectsZeroRowInsert() {
        FakeBookMapper mapper = new FakeBookMapper();
        mapper.insertRows = 0;
        BookBusiness business = new BookBusiness(new NoopAiProvider(), mapper);

        assertThatThrownBy(() -> business.saveBook(SaveBookRequest.builder()
            .candidateId("candidate-zero")
            .title("Zero Row Book")
            .author("Reader")
            .build()))
            .isInstanceOfSatisfying(ResponseStatusException.class, (exception) -> {
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                assertThat(exception.getReason()).isEqualTo("Book could not be saved");
            });
    }

    @Test
    void findSavedBooksReturnsUserBooks() {
        BookBusiness business = new BookBusiness(new NoopAiProvider(), new FakeBookMapper());

        assertThat(business.findSavedBooks().getBooks())
            .extracting(SaveBookResponse::getTitle)
            .containsExactly("Saved Book");
    }

    @Test
    void searchCandidatesReturnsOnlySaveCompatibleCandidates() {
        BookBusiness business = new BookBusiness(new CandidateAiProvider(BookCandidateSearchResponse.builder()
            .aiModel("test-model")
            .candidates(List.of(
                BookCandidateDto.builder()
                    .candidateId("  " + "c".repeat(300) + "  ")
                    .title("  " + "t".repeat(300) + "  ")
                    .author("  " + "a".repeat(300) + "  ")
                    .publishedYear(2026)
                    .reason("reader match")
                    .build(),
                BookCandidateDto.builder()
                    .candidateId("blank-title")
                    .title("   ")
                    .author("Author")
                    .build(),
                BookCandidateDto.builder()
                    .candidateId(null)
                    .title("Missing id")
                    .author("Author")
                    .build()
            ))
            .build()), new FakeBookMapper());

        BookCandidateSearchResponse response = business.searchCandidates(BookCandidateSearchRequest.builder()
            .query("long candidate")
            .build());

        assertThat(response.getAiModel()).isEqualTo("test-model");
        assertThat(response.getCandidates()).singleElement()
            .satisfies((candidate) -> {
                assertThat(candidate.getCandidateId()).hasSize(255).doesNotStartWith(" ");
                assertThat(candidate.getTitle()).hasSize(255).doesNotStartWith(" ");
                assertThat(candidate.getAuthor()).hasSize(255).doesNotStartWith(" ");
                assertThat(candidate.getPublishedYear()).isEqualTo(2026);
                assertThat(candidate.getReason()).isEqualTo("reader match");
            });
    }

    private static class FakeBookMapper implements BookMapper {
        private BookRecord inserted;
        private BookRecord duplicate;
        private String duplicateLookupTitle;
        private String duplicateLookupAuthor;
        private int insertRows = 1;

        @Override
        public int insert(BookRecord record) {
            this.inserted = record;
            record.setId(42L);
            return insertRows;
        }

        @Override
        public BookRecord findDuplicate(Long userId, String title, String author) {
            this.duplicateLookupTitle = title;
            this.duplicateLookupAuthor = author;
            return duplicate;
        }

        @Override
        public List<BookRecord> findByUserId(Long userId) {
            return List.of(BookRecord.builder()
                .id(43L)
                .userId(userId)
                .title("Saved Book")
                .author("Saved Author")
                .build());
        }
    }

    private static class NoopAiProvider implements AiProvider {
        @Override
        public BookCandidateSearchResponse suggestBooks(String query) {
            return BookCandidateSearchResponse.builder().build();
        }

        @Override
        public QuestionListResponse suggestQuestions(Long windowId, GenerateQuestionsRequest request) {
            return QuestionListResponse.builder().build();
        }

        @Override
        public AiMessageResponse answerWindowMessage(Long windowId, SendMessageRequest request) {
            return null;
        }

        @Override
        public AiMessageResponse answerDebateMessage(Long windowId, DebateMessageRequest request) {
            return null;
        }
    }

    private static class CandidateAiProvider extends NoopAiProvider {
        private final BookCandidateSearchResponse response;

        private CandidateAiProvider(BookCandidateSearchResponse response) {
            this.response = response;
        }

        @Override
        public BookCandidateSearchResponse suggestBooks(String query) {
            return response;
        }
    }
}
