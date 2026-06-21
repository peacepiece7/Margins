package com.margins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.margins.ai.AiProvider;
import com.margins.book.business.BookBusiness;
import com.margins.book.dto.BookCandidateDto;
import com.margins.book.dto.BookCandidateSearchRequest;
import com.margins.book.dto.BookCandidateSearchResponse;
import com.margins.book.dto.SaveBookRequest;
import com.margins.book.dto.SaveBookResponse;
import com.margins.book.dto.UpdateBookRequest;
import com.margins.book.mapper.BookMapper;
import com.margins.book.model.BookRecord;
import com.margins.book.provider.ExternalBookSearchProvider;
import com.margins.book.provider.ExternalBookSearchProperties;
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
        BookBusiness business = business(new NoopAiProvider(), new EmptyExternalBookSearchProvider(), mapper);

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
    void saveBookPersistsProviderSourceFromCandidateIdPrefix() {
        FakeBookMapper mapper = new FakeBookMapper();
        BookBusiness business = business(new NoopAiProvider(), new EmptyExternalBookSearchProvider(), mapper);

        business.saveBook(SaveBookRequest.builder()
            .candidateId("kakao:9788996991342")
            .title("미움받을 용기")
            .author("기시미 이치로, 고가 후미타케")
            .publishedYear(2014)
            .build());

        assertThat(mapper.inserted.getSource()).isEqualTo("kakao");
        assertThat(mapper.inserted.getSourceRef()).isEqualTo("kakao:9788996991342");
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
        BookBusiness business = business(new NoopAiProvider(), new EmptyExternalBookSearchProvider(), mapper);

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
        BookBusiness business = business(new NoopAiProvider(), new EmptyExternalBookSearchProvider(), mapper);

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
        BookBusiness business = business(new NoopAiProvider(), new EmptyExternalBookSearchProvider(), new FakeBookMapper());

        assertThat(business.findSavedBooks().getBooks())
            .extracting(SaveBookResponse::getTitle)
            .containsExactly("Saved Book");
    }

    @Test
    void updateBookTrimsAndPersistsEditableMetadata() {
        FakeBookMapper mapper = new FakeBookMapper();
        mapper.bookById = BookRecord.builder()
            .id(43L)
            .userId(1L)
            .title("Before")
            .author("Old")
            .build();
        BookBusiness business = business(new NoopAiProvider(), new EmptyExternalBookSearchProvider(), mapper);

        SaveBookResponse response = business.updateBook(43L, UpdateBookRequest.builder()
            .title("  Updated Book  ")
            .author("  Updated Author  ")
            .publishedYear(2026)
            .build());

        assertThat(response.getTitle()).isEqualTo("Updated Book");
        assertThat(mapper.updated.getTitle()).isEqualTo("Updated Book");
        assertThat(mapper.updated.getAuthor()).isEqualTo("Updated Author");
        assertThat(mapper.updated.getPublishedYear()).isEqualTo(2026);
    }

    @Test
    void deleteBookSoftDeletesAndReturnsRemainingList() {
        FakeBookMapper mapper = new FakeBookMapper();
        mapper.bookById = BookRecord.builder()
            .id(43L)
            .userId(1L)
            .title("Saved Book")
            .author("Saved Author")
            .build();
        BookBusiness business = business(new NoopAiProvider(), new EmptyExternalBookSearchProvider(), mapper);

        assertThat(business.deleteBook(43L).getBooks()).extracting(SaveBookResponse::getTitle)
            .containsExactly("Saved Book");

        assertThat(mapper.deletedBookId).isEqualTo(43L);
    }

    @Test
    void searchCandidatesReturnsOnlySaveCompatibleCandidates() {
        BookBusiness business = business(new CandidateAiProvider(BookCandidateSearchResponse.builder()
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
            .build()), new EmptyExternalBookSearchProvider(), new FakeBookMapper());

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

    @Test
    void searchCandidatesPrefersExternalBookApiResults() {
        BookBusiness business = business(
            new CandidateAiProvider(BookCandidateSearchResponse.builder()
                .aiModel("should-not-be-used")
                .candidates(List.of(BookCandidateDto.builder()
                    .candidateId("ai-1")
                    .title("AI Result")
                    .author("AI Author")
                    .build()))
                .build()),
            new FixedExternalBookSearchProvider(List.of(BookCandidateDto.builder()
                .candidateId("openlibrary:/works/OL27448W")
                .title("Dune")
                .author("Frank Herbert")
                .publishedYear(1965)
                .reason("Open Library search result")
                .build())),
            new FakeBookMapper()
        );

        BookCandidateSearchResponse response = business.searchCandidates(BookCandidateSearchRequest.builder()
            .query("Dune")
            .build());

        assertThat(response.getAiModel()).isEqualTo("openlibrary");
        assertThat(response.getCandidates()).singleElement()
            .satisfies((candidate) -> {
                assertThat(candidate.getCandidateId()).isEqualTo("openlibrary:/works/OL27448W");
                assertThat(candidate.getTitle()).isEqualTo("Dune");
                assertThat(candidate.getAuthor()).isEqualTo("Frank Herbert");
                assertThat(candidate.getPublishedYear()).isEqualTo(1965);
            });
    }

    @Test
    void searchCandidatesUsesConfiguredProviderBeforeFallbackProviders() {
        ExternalBookSearchProperties properties = new ExternalBookSearchProperties();
        properties.setProvider("kakao");
        BookBusiness business = new BookBusiness(
            new CandidateAiProvider(BookCandidateSearchResponse.builder()
                .aiModel("should-not-be-used")
                .candidates(List.of(BookCandidateDto.builder()
                    .candidateId("ai-1")
                    .title("AI Result")
                    .author("AI Author")
                    .build()))
                .build()),
            List.of(
                new NamedExternalBookSearchProvider("openlibrary", List.of(BookCandidateDto.builder()
                    .candidateId("openlibrary:/works/OL27448W")
                    .title("Dune")
                    .author("Frank Herbert")
                    .publishedYear(1965)
                    .build())),
                new NamedExternalBookSearchProvider("kakao", List.of(BookCandidateDto.builder()
                    .candidateId("kakao:9788996991342")
                    .title("미움받을 용기")
                    .author("기시미 이치로, 고가 후미타케")
                    .publishedYear(2014)
                    .build()))
            ),
            properties,
            new FakeBookMapper()
        );

        BookCandidateSearchResponse response = business.searchCandidates(BookCandidateSearchRequest.builder()
            .query("미움받을 용기")
            .build());

        assertThat(response.getAiModel()).isEqualTo("kakao");
        assertThat(response.getCandidates()).singleElement()
            .satisfies((candidate) -> {
                assertThat(candidate.getCandidateId()).isEqualTo("kakao:9788996991342");
                assertThat(candidate.getTitle()).isEqualTo("미움받을 용기");
                assertThat(candidate.getAuthor()).isEqualTo("기시미 이치로, 고가 후미타케");
            });
    }

    private BookBusiness business(AiProvider aiProvider, ExternalBookSearchProvider externalBookSearchProvider, BookMapper bookMapper) {
        return business(aiProvider, List.of(externalBookSearchProvider), bookMapper);
    }

    private BookBusiness business(AiProvider aiProvider, List<ExternalBookSearchProvider> externalBookSearchProviders, BookMapper bookMapper) {
        ExternalBookSearchProperties properties = new ExternalBookSearchProperties();
        properties.setProvider("openlibrary");
        return new BookBusiness(aiProvider, externalBookSearchProviders, properties, bookMapper);
    }

    private static class FakeBookMapper implements BookMapper {
        private BookRecord inserted;
        private BookRecord updated;
        private BookRecord duplicate;
        private BookRecord bookById;
        private Long deletedBookId;
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

        @Override
        public BookRecord findByIdForUser(Long bookId, Long userId) {
            return bookById;
        }

        @Override
        public int update(BookRecord record) {
            this.updated = record;
            this.bookById = record;
            return 1;
        }

        @Override
        public int softDelete(Long bookId, Long userId) {
            this.deletedBookId = bookId;
            return 1;
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

    private static class EmptyExternalBookSearchProvider implements ExternalBookSearchProvider {
        @Override
        public List<BookCandidateDto> search(String query) {
            return List.of();
        }
    }

    private static class FixedExternalBookSearchProvider implements ExternalBookSearchProvider {
        private final List<BookCandidateDto> candidates;

        private FixedExternalBookSearchProvider(List<BookCandidateDto> candidates) {
            this.candidates = candidates;
        }

        @Override
        public String providerName() {
            return "openlibrary";
        }

        @Override
        public List<BookCandidateDto> search(String query) {
            return candidates;
        }
    }

    private static class NamedExternalBookSearchProvider implements ExternalBookSearchProvider {
        private final String providerName;
        private final List<BookCandidateDto> candidates;

        private NamedExternalBookSearchProvider(String providerName, List<BookCandidateDto> candidates) {
            this.providerName = providerName;
            this.candidates = candidates;
        }

        @Override
        public String providerName() {
            return providerName;
        }

        @Override
        public List<BookCandidateDto> search(String query) {
            return candidates;
        }
    }
}
