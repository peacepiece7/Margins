package com.margins.book.controller;

import com.margins.book.dto.BookListResponse;
import com.margins.book.dto.BookCandidateSearchRequest;
import com.margins.book.dto.BookCandidateSearchResponse;
import com.margins.book.dto.SaveBookRequest;
import com.margins.book.dto.SaveBookResponse;
import com.margins.book.dto.UpdateBookRequest;
import com.margins.book.service.BookService;
import com.margins.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @PostMapping("/search-candidates")
    public ApiResponse<BookCandidateSearchResponse> searchCandidates(@Valid @RequestBody BookCandidateSearchRequest request) {
        return ApiResponse.ok(bookService.searchCandidates(request));
    }

    @GetMapping
    public ApiResponse<BookListResponse> list() {
        return ApiResponse.ok(bookService.findSavedBooks());
    }

    @PostMapping
    public ApiResponse<SaveBookResponse> saveBook(@Valid @RequestBody SaveBookRequest request) {
        return ApiResponse.ok(bookService.saveBook(request));
    }

    @PatchMapping("/{bookId}")
    public ApiResponse<SaveBookResponse> updateBook(
        @PathVariable Long bookId,
        @Valid @RequestBody UpdateBookRequest request
    ) {
        return ApiResponse.ok(bookService.updateBook(bookId, request));
    }

    @DeleteMapping("/{bookId}")
    public ApiResponse<BookListResponse> deleteBook(@PathVariable Long bookId) {
        return ApiResponse.ok(bookService.deleteBook(bookId));
    }
}
