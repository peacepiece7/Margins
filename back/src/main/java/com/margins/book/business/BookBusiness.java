package com.margins.book.business;

import com.margins.ai.AiProvider;
import com.margins.book.dto.BookCandidateSearchRequest;
import com.margins.book.dto.BookCandidateSearchResponse;
import com.margins.book.dto.SaveBookRequest;
import com.margins.book.dto.SaveBookResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookBusiness {

    private final AiProvider aiProvider;

    public BookCandidateSearchResponse searchCandidates(BookCandidateSearchRequest request) {
        return aiProvider.suggestBooks(request.getQuery());
    }

    public SaveBookResponse saveBook(SaveBookRequest request) {
        return SaveBookResponse.builder()
            .bookId(1L)
            .title(request.getTitle())
            .author(request.getAuthor())
            .build();
    }
}
