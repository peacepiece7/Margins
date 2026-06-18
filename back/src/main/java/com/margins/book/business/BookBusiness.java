package com.margins.book.business;

import com.margins.ai.AiProvider;
import com.margins.book.dto.BookCandidateDto;
import com.margins.book.dto.BookListResponse;
import com.margins.book.dto.BookCandidateSearchRequest;
import com.margins.book.dto.BookCandidateSearchResponse;
import com.margins.book.dto.SaveBookRequest;
import com.margins.book.dto.SaveBookResponse;
import com.margins.book.mapper.BookMapper;
import com.margins.book.model.BookRecord;
import com.margins.book.provider.BookSearchProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class BookBusiness {

    private static final long DEFAULT_USER_ID = 1L;
    private static final int SAVE_TEXT_LIMIT = 255;

    private final AiProvider aiProvider;
    private final BookMapper bookMapper;
    private final List<BookSearchProvider> bookSearchProviders;

    public BookCandidateSearchResponse searchCandidates(BookCandidateSearchRequest request) {
        BookCandidateSearchResponse response = externalSearch(request.getQuery());
        if (response.getCandidates() == null || response.getCandidates().isEmpty()) {
            response = aiProvider.suggestBooks(request.getQuery());
        }
        List<BookCandidateDto> candidates = response.getCandidates() == null
            ? List.of()
            : response.getCandidates().stream()
                .map(this::sanitizeCandidate)
                .filter((candidate) -> candidate != null)
                .toList();

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
            .publishedYear(request.getPublishedYear())
            .source(sourceFromCandidateId(request.getCandidateId()))
            .sourceRef(request.getCandidateId())
            .testData(true)
            .build();

        if (bookMapper.insert(record) <= 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Book could not be saved");
        }

        return toResponse(record);
    }

    private BookCandidateSearchResponse externalSearch(String query) {
        for (BookSearchProvider provider : bookSearchProviders) {
            BookCandidateSearchResponse response = provider.search(query);
            if (response.getCandidates() != null && !response.getCandidates().isEmpty()) {
                return response;
            }
        }

        return BookCandidateSearchResponse.builder().candidates(List.of()).build();
    }

    private String sourceFromCandidateId(String candidateId) {
        if (candidateId != null && candidateId.startsWith("open-library:")) {
            return "open-library";
        }
        return "ai";
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
        String title = trimToLimit(candidate.getTitle());
        String author = trimToLimit(candidate.getAuthor());
        if (candidateId.isBlank() || title.isBlank() || author.isBlank()) {
            return null;
        }

        return BookCandidateDto.builder()
            .candidateId(candidateId)
            .title(title)
            .author(author)
            .publishedYear(candidate.getPublishedYear())
            .reason(candidate.getReason())
            .build();
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
}
