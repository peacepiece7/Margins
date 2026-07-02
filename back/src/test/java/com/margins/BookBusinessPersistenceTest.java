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
            .subtitle("  A Field Guide  ")
            .author("  Reader  ")
            .authors(List.of("  Reader  ", "Co Reader"))
            .publisher("  Margins Press  ")
            .publishedDate("  2026-07-02  ")
            .isbn("  9788996991342  ")
            .isbn10("  8996991341  ")
            .isbn13("  9788996991342  ")
            .publishedYear(2026)
            .description("  Provider description  ")
            .thumbnail("  https://books.example/cover.jpg  ")
            .language("  ko  ")
            .pageCount(304)
            .build());

        assertThat(response.getBookId()).isEqualTo(42L);
        assertThat(response.getTitle()).isEqualTo("Persisted Book");
        assertThat(response.getSubtitle()).isEqualTo("A Field Guide");
        assertThat(response.getPublisher()).isEqualTo("Margins Press");
        assertThat(response.getPublishedYear()).isEqualTo(2026);
        assertThat(response.getIsbn()).isEqualTo("9788996991342");
        assertThat(response.getCoverImageUrl()).isEqualTo("https://books.example/cover.jpg");
        assertThat(response.getLanguage()).isEqualTo("ko");
        assertThat(mapper.inserted.getUserId()).isEqualTo(1L);
        assertThat(mapper.inserted.getSubtitle()).isEqualTo("A Field Guide");
        assertThat(mapper.inserted.getPublisher()).isEqualTo("Margins Press");
        assertThat(mapper.inserted.getIsbn()).isEqualTo("9788996991342");
        assertThat(mapper.inserted.getLanguageCode()).isEqualTo("ko");
        assertThat(mapper.inserted.getDescription()).isEqualTo("Provider description");
        assertThat(mapper.inserted.getCoverImageUrl()).isEqualTo("https://books.example/cover.jpg");
        assertThat(mapper.inserted.getSource()).isEqualTo("ai");
        assertThat(mapper.inserted.getSourceRef()).isEqualTo("candidate-1");
        assertThat(mapper.inserted.getRawMetadata())
            .contains("\"providerMetadata\"")
            .contains("\"isbn13\":\"9788996991342\"")
            .contains("\"pageCount\":304")
            .contains("\"aiProfile\"")
            .contains("\"isbn\":\"9788996991342\"")
            .contains("\"discussionAngles\"")
            .contains("\"confidence\":\"low\"");
    }

    @Test
    void saveBookPersistsProviderSourceFromCandidateIdPrefix() {
        FakeBookMapper mapper = new FakeBookMapper();
        BookBusiness business = business(new NoopAiProvider(), new EmptyExternalBookSearchProvider(), mapper);

        business.saveBook(SaveBookRequest.builder()
            .candidateId("google:9788996991342")
            .title("미움받을 용기")
            .author("기시미 이치로, 고가 후미타케")
            .publishedYear(2014)
            .build());

        assertThat(mapper.inserted.getSource()).isEqualTo("google");
        assertThat(mapper.inserted.getSourceRef()).isEqualTo("google:9788996991342");
        assertThat(mapper.inserted.getRawMetadata()).contains("\"provider\":\"google\"");
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
        assertThat(mapper.enriched.getId()).isEqualTo(99L);
        assertThat(mapper.duplicateLookupTitle).isEqualTo("dune");
        assertThat(mapper.duplicateLookupAuthor).isEqualTo("ai candidate");
    }

    @Test
    void saveBookEnrichesExistingDuplicateWithMissingProviderFieldsOnly() {
        FakeBookMapper mapper = new FakeBookMapper();
        mapper.duplicate = BookRecord.builder()
            .id(99L)
            .userId(1L)
            .title("Dune")
            .author("Frank Herbert")
            .source("ai")
            .sourceRef("manual-123")
            .rawMetadata("{\"providerMetadata\":{\"provider\":\"manual\",\"candidateId\":\"manual-123\"},\"aiProfile\":{\"title\":\"Dune\"}}")
            .build();
        mapper.bookById = mapper.duplicate;
        BookBusiness business = business(new NoopAiProvider(), new EmptyExternalBookSearchProvider(), mapper);

        SaveBookResponse response = business.saveBook(SaveBookRequest.builder()
            .candidateId("google:9780441172719")
            .title("Dune")
            .subtitle("Deluxe Edition")
            .author("Frank Herbert")
            .publisher("Ace")
            .isbn("9780441172719")
            .publishedYear(1965)
            .thumbnail("https://books.example/dune.jpg")
            .language("en")
            .build());

        assertThat(mapper.inserted).isNull();
        assertThat(mapper.enriched.getSource()).isEqualTo("google");
        assertThat(mapper.enriched.getSourceRef()).isEqualTo("google:9780441172719");
        assertThat(mapper.bookById.getTitle()).isEqualTo("Dune");
        assertThat(mapper.bookById.getAuthor()).isEqualTo("Frank Herbert");
        assertThat(mapper.bookById.getSubtitle()).isEqualTo("Deluxe Edition");
        assertThat(mapper.bookById.getPublisher()).isEqualTo("Ace");
        assertThat(mapper.bookById.getIsbn()).isEqualTo("9780441172719");
        assertThat(mapper.bookById.getCoverImageUrl()).isEqualTo("https://books.example/dune.jpg");
        assertThat(mapper.bookById.getSourceRef()).isEqualTo("google:9780441172719");
        assertThat(mapper.bookById.getRawMetadata())
            .contains("\"providerMetadata\"")
            .contains("\"provider\":\"google\"")
            .contains("\"candidateId\":\"google:9780441172719\"");
        assertThat(response.getBookId()).isEqualTo(99L);
        assertThat(response.getSource()).isEqualTo("google");
        assertThat(response.getSourceRef()).isEqualTo("google:9780441172719");
        assertThat(response.getIsbn()).isEqualTo("9780441172719");
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
            .source("google")
            .sourceRef("google:9780441478125")
            .isbn("9780441478125")
            .languageCode("en")
            .rawMetadata("{\"providerMetadata\":{\"provider\":\"google\",\"candidateId\":\"google:9780441478125\",\"isbn13\":\"9780441478125\",\"publisher\":\"Ace\",\"thumbnail\":\"https://books.example/cover.jpg\",\"pageCount\":304},\"aiProfile\":{\"title\":\"Before\",\"author\":\"Old\"}}")
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
        assertThat(mapper.updated.getRawMetadata())
            .contains("\"providerMetadata\"")
            .contains("\"candidateId\":\"google:9780441478125\"")
            .contains("\"isbn13\":\"9780441478125\"")
            .contains("\"thumbnail\":\"https://books.example/cover.jpg\"")
            .contains("\"pageCount\":304")
            .contains("\"title\":\"Updated Book\"")
            .contains("\"author\":\"Updated Author\"")
            .contains("\"publishedYear\":2026")
            .doesNotContain("Before")
            .doesNotContain("Old");
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
                    .isbn("  " + "9".repeat(40) + "  ")
                    .isbn10("  0441478123  ")
                    .isbn13("  9780441478125  ")
                    .title("  " + "t".repeat(300) + "  ")
                    .subtitle("  " + "s".repeat(300) + "  ")
                    .author("  " + "a".repeat(300) + "  ")
                    .authors(List.of("  Ursula K. Le Guin  ", " "))
                    .publisher("  Ace  ")
                    .publishedDate("  2019-02-05  ")
                    .publishedYear(2026)
                    .description("  " + "d".repeat(300) + "  ")
                    .thumbnail("  http://books.google.com/thumb.jpg  ")
                    .language("  en  ")
                    .pageCount(304)
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
                assertThat(candidate.getIsbn()).hasSize(32).doesNotStartWith(" ");
                assertThat(candidate.getIsbn10()).isEqualTo("0441478123");
                assertThat(candidate.getIsbn13()).isEqualTo("9780441478125");
                assertThat(candidate.getTitle()).hasSize(255).doesNotStartWith(" ");
                assertThat(candidate.getSubtitle()).hasSize(255).doesNotStartWith(" ");
                assertThat(candidate.getAuthor()).hasSize(255).doesNotStartWith(" ");
                assertThat(candidate.getAuthors()).containsExactly("Ursula K. Le Guin");
                assertThat(candidate.getPublisher()).isEqualTo("Ace");
                assertThat(candidate.getPublishedDate()).isEqualTo("2019-02-05");
                assertThat(candidate.getPublishedYear()).isEqualTo(2026);
                assertThat(candidate.getDescription()).hasSize(255).doesNotStartWith(" ");
                assertThat(candidate.getThumbnail()).isEqualTo("http://books.google.com/thumb.jpg");
                assertThat(candidate.getLanguage()).isEqualTo("en");
                assertThat(candidate.getPageCount()).isEqualTo(304);
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
        properties.setProvider("google");
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
                new NamedExternalBookSearchProvider("google", List.of(BookCandidateDto.builder()
                    .candidateId("google:9788996991342")
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

        assertThat(response.getAiModel()).isEqualTo("google");
        assertThat(response.getCandidates()).singleElement()
            .satisfies((candidate) -> {
                assertThat(candidate.getCandidateId()).isEqualTo("google:9788996991342");
                assertThat(candidate.getTitle()).isEqualTo("미움받을 용기");
                assertThat(candidate.getAuthor()).isEqualTo("기시미 이치로, 고가 후미타케");
            });
    }

    @Test
    void searchCandidatesReturnsEmptyWhenExternalProvidersReturnEmptyAndAiFallbackIsDisabled() {
        ExternalBookSearchProperties properties = new ExternalBookSearchProperties();
        properties.setProvider("google");
        properties.setAiFallbackEnabled(false);
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
                new NamedExternalBookSearchProvider("google", List.of()),
                new NamedExternalBookSearchProvider("openlibrary", List.of())
            ),
            properties,
            new FakeBookMapper()
        );

        BookCandidateSearchResponse response = business.searchCandidates(BookCandidateSearchRequest.builder()
            .query("light and matter")
            .build());

        assertThat(response.getAiModel()).isEqualTo("external-none");
        assertThat(response.getCandidates()).isEmpty();
    }

    private BookBusiness business(AiProvider aiProvider, ExternalBookSearchProvider externalBookSearchProvider, BookMapper bookMapper) {
        return business(aiProvider, List.of(externalBookSearchProvider), bookMapper);
    }

    private BookBusiness business(AiProvider aiProvider, List<ExternalBookSearchProvider> externalBookSearchProviders, BookMapper bookMapper) {
        ExternalBookSearchProperties properties = new ExternalBookSearchProperties();
        properties.setAiFallbackEnabled(true);
        properties.setProvider("openlibrary");
        return new BookBusiness(aiProvider, externalBookSearchProviders, properties, bookMapper);
    }

    private static class FakeBookMapper implements BookMapper {
        private BookRecord inserted;
        private BookRecord updated;
        private BookRecord enriched;
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
        public int fillMissingProviderMetadata(BookRecord record) {
            this.enriched = record;
            if (bookById == null) {
                bookById = duplicate;
            }
            if (bookById != null) {
                if (bookById.getSubtitle() == null || bookById.getSubtitle().isBlank()) {
                    bookById.setSubtitle(record.getSubtitle());
                }
                if (bookById.getPublisher() == null || bookById.getPublisher().isBlank()) {
                    bookById.setPublisher(record.getPublisher());
                }
                if (bookById.getIsbn() == null || bookById.getIsbn().isBlank()) {
                    bookById.setIsbn(record.getIsbn());
                }
                if (bookById.getPublishedYear() == null) {
                    bookById.setPublishedYear(record.getPublishedYear());
                }
                if (bookById.getLanguageCode() == null || bookById.getLanguageCode().isBlank()) {
                    bookById.setLanguageCode(record.getLanguageCode());
                }
                if (bookById.getDescription() == null || bookById.getDescription().isBlank()) {
                    bookById.setDescription(record.getDescription());
                }
                boolean externalUpgrade = !"ai".equals(record.getSource())
                    && (bookById.getSource() == null
                        || bookById.getSource().isBlank()
                        || "ai".equals(bookById.getSource())
                        || bookById.getSourceRef() == null
                        || bookById.getSourceRef().isBlank()
                        || bookById.getSourceRef().startsWith("manual-"));
                if ((bookById.getSource() == null || bookById.getSource().isBlank() || "ai".equals(bookById.getSource()))
                    && !"ai".equals(record.getSource())) {
                    bookById.setSource(record.getSource());
                }
                if (externalUpgrade || bookById.getSourceRef() == null || bookById.getSourceRef().isBlank()) {
                    bookById.setSourceRef(record.getSourceRef());
                }
                if (bookById.getCoverImageUrl() == null || bookById.getCoverImageUrl().isBlank()) {
                    bookById.setCoverImageUrl(record.getCoverImageUrl());
                }
                if (externalUpgrade || bookById.getRawMetadata() == null || bookById.getRawMetadata().isBlank()) {
                    bookById.setRawMetadata(record.getRawMetadata());
                }
            }
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
