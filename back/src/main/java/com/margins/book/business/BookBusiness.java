package com.margins.book.business;

import com.margins.ai.AiProvider;
import com.margins.book.dto.BookCandidateSearchRequest;
import com.margins.book.dto.BookCandidateSearchResponse;
import com.margins.book.dto.SaveBookRequest;
import com.margins.book.dto.SaveBookResponse;
import com.margins.book.mapper.BookMapper;
import com.margins.book.model.BookRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookBusiness {

    private static final long DEFAULT_USER_ID = 1L;

    private final AiProvider aiProvider;
    private final BookMapper bookMapper;

    public BookCandidateSearchResponse searchCandidates(BookCandidateSearchRequest request) {
        return aiProvider.suggestBooks(request.getQuery());
    }

    public SaveBookResponse saveBook(SaveBookRequest request) {
        BookRecord record = BookRecord.builder()
            .userId(DEFAULT_USER_ID)
            .title(request.getTitle())
            .author(request.getAuthor())
            .publishedYear(request.getPublishedYear())
            .source("ai")
            .sourceRef(request.getCandidateId())
            .testData(true)
            .build();

        bookMapper.insert(record);

        return SaveBookResponse.builder()
            .bookId(record.getId())
            .title(record.getTitle())
            .author(record.getAuthor())
            .build();
    }
}
