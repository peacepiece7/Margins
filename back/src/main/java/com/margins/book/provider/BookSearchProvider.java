package com.margins.book.provider;

import com.margins.book.dto.BookCandidateSearchResponse;

public interface BookSearchProvider {
    BookCandidateSearchResponse search(String query);
}
