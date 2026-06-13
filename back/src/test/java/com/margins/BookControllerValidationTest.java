package com.margins;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.margins.auth.filter.AuthTokenFilter;
import com.margins.book.controller.BookController;
import com.margins.book.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = BookController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = AuthTokenFilter.class)
)
class BookControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;

    @Test
    void searchRejectsBlankQuery() throws Exception {
        mockMvc.perform(post("/api/books/search-candidates")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"   \"}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(bookService);
    }

    @Test
    void saveRejectsMissingTitle() throws Exception {
        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"candidateId\":\"c1\",\"author\":\"Author\"}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(bookService);
    }

    @Test
    void saveRejectsOverlongTitleBeforeBusinessLogic() throws Exception {
        String title = "a".repeat(256);

        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"candidateId":"c1","title":"%s","author":"Author"}
                    """.formatted(title)))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(bookService);
    }
}
