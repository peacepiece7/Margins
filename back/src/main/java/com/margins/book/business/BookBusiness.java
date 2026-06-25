package com.margins.book.business;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.margins.ai.AiProvider;
import com.margins.book.dto.BookCandidateDto;
import com.margins.book.dto.BookListResponse;
import com.margins.book.dto.BookCandidateSearchRequest;
import com.margins.book.dto.BookCandidateSearchResponse;
import com.margins.book.dto.SaveBookRequest;
import com.margins.book.dto.SaveBookResponse;
import com.margins.book.dto.UpdateBookRequest;
import com.margins.book.mapper.BookMapper;
import com.margins.book.model.BookRecord;
import com.margins.book.provider.ExternalBookSearchProvider;
import com.margins.book.provider.ExternalBookSearchProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookBusiness {

    private static final long DEFAULT_USER_ID = 1L;
    private static final int SAVE_TEXT_LIMIT = 255;
    private static final int ISBN_TEXT_LIMIT = 32;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AiProvider aiProvider;
    private final List<ExternalBookSearchProvider> externalBookSearchProviders;
    private final ExternalBookSearchProperties externalBookSearchProperties;
    private final BookMapper bookMapper;

    public BookCandidateSearchResponse searchCandidates(BookCandidateSearchRequest request) {
        List<ExternalBookSearchProvider> providers = orderedExternalProviders();
        log.info("Book search started. preferredProvider={}, aiFallbackEnabled={}, availableProviders={}, queryPreview={}",
            preferredProviderName(),
            externalBookSearchProperties.isAiFallbackEnabled(),
            providerNames(providers),
            queryPreview(request.getQuery()));

        for (ExternalBookSearchProvider provider : providers) {
            List<BookCandidateDto> externalCandidates = sanitizeCandidates(provider.search(request.getQuery()));
            log.info("Book search provider result. provider={}, candidateCount={}",
                provider.providerName(),
                externalCandidates.size());
            if (externalCandidates.isEmpty()) {
                continue;
            }

            return BookCandidateSearchResponse.builder()
                .candidates(externalCandidates)
                .aiModel(provider.providerName())
                .build();
        }

        if (!externalBookSearchProperties.isAiFallbackEnabled()) {
            log.warn("Book search AI fallback disabled and external providers returned no candidates");
            return BookCandidateSearchResponse.builder()
                .candidates(List.of())
                .aiModel("external-none")
                .build();
        }

        log.info("Book search external providers returned no candidates; using AI fallback");
        BookCandidateSearchResponse response = aiProvider.suggestBooks(request.getQuery());
        List<BookCandidateDto> candidates = sanitizeCandidates(response.getCandidates());
        return BookCandidateSearchResponse.builder()
            .candidates(candidates)
            .aiModel(response.getAiModel())
            .build();
    }

    public BookListResponse findSavedBooks() {
        return BookListResponse.builder()
            .books(bookMapper.findByUserId(DEFAULT_USER_ID)
                .stream()
                .map(this::toResponse)
                .toList())
            .build();
    }

    public SaveBookResponse saveBook(SaveBookRequest request) {
        String title = request.getTitle().trim();
        String author = request.getAuthor() == null ? null : request.getAuthor().trim();
        BookRecord existing = bookMapper.findDuplicate(DEFAULT_USER_ID, title, author);
        if (existing != null) {
            return toResponse(existing);
        }

        BookRecord record = BookRecord.builder()
            .userId(DEFAULT_USER_ID)
            .title(title)
            .author(author)
            .isbn(trimIsbn(request.getIsbn()))
            .publishedYear(request.getPublishedYear())
            .source(sourceFromCandidateId(request.getCandidateId()))
            .sourceRef(request.getCandidateId())
            .rawMetadata(bookAiProfileMetadata(title, author, trimIsbn(request.getIsbn()), request.getPublishedYear(), request.getCandidateId()))
            .testData(true)
            .build();

        if (bookMapper.insert(record) <= 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Book could not be saved");
        }

        return toResponse(record);
    }

    public SaveBookResponse updateBook(Long bookId, UpdateBookRequest request) {
        BookRecord existing = bookMapper.findByIdForUser(bookId, DEFAULT_USER_ID);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book was not found");
        }

        String title = request.getTitle().trim();
        String author = request.getAuthor().trim();
        BookRecord duplicate = bookMapper.findDuplicate(DEFAULT_USER_ID, title, author);
        if (duplicate != null && !duplicate.getId().equals(bookId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Book already exists");
        }

        BookRecord update = BookRecord.builder()
            .id(bookId)
            .userId(DEFAULT_USER_ID)
            .title(title)
            .author(author)
            .publishedYear(request.getPublishedYear())
            .rawMetadata(bookAiProfileMetadata(title, author, existing.getIsbn(), request.getPublishedYear(), existing.getSourceRef()))
            .build();
        if (bookMapper.update(update) <= 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Book could not be updated");
        }

        return toResponse(bookMapper.findByIdForUser(bookId, DEFAULT_USER_ID));
    }

    public BookListResponse deleteBook(Long bookId) {
        BookRecord existing = bookMapper.findByIdForUser(bookId, DEFAULT_USER_ID);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book was not found");
        }
        if (bookMapper.softDelete(bookId, DEFAULT_USER_ID) <= 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Book could not be deleted");
        }

        return findSavedBooks();
    }

    private SaveBookResponse toResponse(BookRecord record) {
        return SaveBookResponse.builder()
            .bookId(record.getId())
            .title(record.getTitle())
            .author(record.getAuthor())
            .build();
    }

    private BookCandidateDto sanitizeCandidate(BookCandidateDto candidate) {
        if (candidate == null) {
            return null;
        }

        String candidateId = trimToLimit(candidate.getCandidateId());
        String isbn = trimIsbn(candidate.getIsbn());
        String title = trimToLimit(candidate.getTitle());
        String author = trimToLimit(candidate.getAuthor());
        if (candidateId.isBlank() || title.isBlank() || author.isBlank()) {
            return null;
        }

        return BookCandidateDto.builder()
            .candidateId(candidateId)
            .isbn(isbn)
            .title(title)
            .author(author)
            .publishedYear(candidate.getPublishedYear())
            .reason(candidate.getReason())
            .build();
    }

    private List<BookCandidateDto> sanitizeCandidates(List<BookCandidateDto> candidates) {
        if (candidates == null) {
            return List.of();
        }

        return candidates.stream()
            .map(this::sanitizeCandidate)
            .filter((candidate) -> candidate != null)
            .toList();
    }

    private String trimToLimit(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= SAVE_TEXT_LIMIT) {
            return trimmed;
        }
        return trimmed.substring(0, SAVE_TEXT_LIMIT);
    }

    private String trimIsbn(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        if (trimmed.length() <= ISBN_TEXT_LIMIT) {
            return trimmed;
        }
        return trimmed.substring(0, ISBN_TEXT_LIMIT);
    }

    private String sourceFromCandidateId(String candidateId) {
        if (candidateId == null || !candidateId.contains(":")) {
            return "ai";
        }

        String source = candidateId.substring(0, candidateId.indexOf(':')).trim().toLowerCase();
        return source.isBlank() ? "ai" : trimToLimit(source);
    }

    private String bookAiProfileMetadata(String title, String author, String isbn, Integer publishedYear, String candidateId) {
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        ObjectNode profile = root.putObject("aiProfile");
        profile.put("isbn", isbn == null ? "" : isbn);
        profile.put("title", title);
        profile.put("author", author == null ? "" : author);
        if (publishedYear != null) {
            profile.put("publishedYear", publishedYear);
        }
        profile.put("language", "");
        profile.putArray("genre");
        profile.putArray("mood");
        profile.put("pace", "unknown");
        ArrayNode themes = profile.putArray("themes");
        themes.add("reader-reflection");
        profile.put("summaryShort", "사용자가 등록한 책 정보를 바탕으로 생성된 초기 토론 컨텍스트입니다.");
        profile.put("summaryLong", "아직 검수된 줄거리나 전문 메타데이터가 없으므로, AI는 제목, 저자, ISBN, 사용자의 세션 기록을 우선 근거로 사용해야 합니다.");
        profile.putArray("characters");
        ArrayNode discussionAngles = profile.putArray("discussionAngles");
        discussionAngles.add("문학적 관점");
        discussionAngles.add("철학적 관점");
        discussionAngles.add("심리학적 관점");
        discussionAngles.add("역사/사회적 관점");
        profile.put("spoilerLevel", "unknown");
        ObjectNode source = profile.putObject("source");
        source.put("provider", sourceFromCandidateId(candidateId));
        source.put("confidence", "low");
        profile.put("generatedAt", "book-save");
        profile.put("reviewedByUser", false);
        try {
            return OBJECT_MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            return "{\"aiProfile\":{\"source\":{\"confidence\":\"missing\"}}}";
        }
    }

    private List<ExternalBookSearchProvider> orderedExternalProviders() {
        List<ExternalBookSearchProvider> providers = new ArrayList<>(externalBookSearchProviders);
        String preferredProvider = preferredProviderName();
        if (preferredProvider.isBlank()) {
            return providers;
        }

        providers.sort((left, right) -> {
            boolean leftPreferred = preferredProvider.equalsIgnoreCase(left.providerName());
            boolean rightPreferred = preferredProvider.equalsIgnoreCase(right.providerName());
            if (leftPreferred == rightPreferred) {
                return 0;
            }
            return leftPreferred ? -1 : 1;
        });
        return providers;
    }

    private String preferredProviderName() {
        return externalBookSearchProperties.getProvider() == null
            ? ""
            : externalBookSearchProperties.getProvider().trim().toLowerCase();
    }

    private List<String> providerNames(List<ExternalBookSearchProvider> providers) {
        return providers.stream()
            .map(ExternalBookSearchProvider::providerName)
            .toList();
    }

    private String queryPreview(String query) {
        if (query == null) {
            return "";
        }
        String trimmed = query.trim();
        if (trimmed.length() <= 40) {
            return trimmed;
        }
        return trimmed.substring(0, 40) + "...";
    }
}
