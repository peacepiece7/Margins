package com.margins.book.service;

import com.margins.book.business.BookBusiness;
import com.margins.book.dto.BookCandidateSearchRequest;
import com.margins.book.dto.BookCandidateSearchResponse;
import com.margins.book.dto.SaveBookRequest;
import com.margins.book.dto.SaveBookResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookBusiness bookBusiness;

    public BookCandidateSearchResponse searchCandidates(BookCandidateSearchRequest request) {
        return bookBusiness.searchCandidates(request);
    }

    public SaveBookResponse saveBook(SaveBookRequest request) {
        return bookBusiness.saveBook(request);
    }
}
