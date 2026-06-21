package com.margins.book.provider;

import com.margins.book.dto.BookCandidateDto;
import java.util.List;

public interface ExternalBookSearchProvider {
    default String providerName() {
        return "external";
    }

    List<BookCandidateDto> search(String query);
}
