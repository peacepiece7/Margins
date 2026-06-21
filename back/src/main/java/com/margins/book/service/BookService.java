package com.margins.book.service;

import com.margins.book.business.BookBusiness;
import com.margins.book.dto.BookListResponse;
import com.margins.book.dto.BookCandidateSearchRequest;
import com.margins.book.dto.BookCandidateSearchResponse;
import com.margins.book.dto.SaveBookRequest;
import com.margins.book.dto.SaveBookResponse;
import com.margins.book.dto.UpdateBookRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookBusiness bookBusiness;

    public BookCandidateSearchResponse searchCandidates(BookCandidateSearchRequest request) {
        return bookBusiness.searchCandidates(request);
    }

    @Transactional(readOnly = true)
    public BookListResponse findSavedBooks() {
        return bookBusiness.findSavedBooks();
    }

    @Transactional
    public SaveBookResponse saveBook(SaveBookRequest request) {
        return bookBusiness.saveBook(request);
    }

    @Transactional
    public SaveBookResponse updateBook(Long bookId, UpdateBookRequest request) {
        return bookBusiness.updateBook(bookId, request);
    }

    @Transactional
    public BookListResponse deleteBook(Long bookId) {
        return bookBusiness.deleteBook(bookId);
    }
}
