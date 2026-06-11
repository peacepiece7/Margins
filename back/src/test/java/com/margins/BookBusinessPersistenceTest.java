package com.margins;

import static org.assertj.core.api.Assertions.assertThat;

import com.margins.ai.AiProvider;
import com.margins.book.business.BookBusiness;
import com.margins.book.dto.BookCandidateSearchResponse;
import com.margins.book.dto.SaveBookRequest;
import com.margins.book.dto.SaveBookResponse;
import com.margins.book.mapper.BookMapper;
import com.margins.book.model.BookRecord;
import com.margins.session.dto.AiMessageResponse;
import com.margins.session.dto.DebateMessageRequest;
import com.margins.session.dto.SendMessageRequest;
import org.junit.jupiter.api.Test;

class BookBusinessPersistenceTest {

    @Test
    void saveBookPersistsAndReturnsGeneratedId() {
        FakeBookMapper mapper = new FakeBookMapper();
        BookBusiness business = new BookBusiness(new NoopAiProvider(), mapper);

        SaveBookResponse response = business.saveBook(SaveBookRequest.builder()
            .candidateId("candidate-1")
            .title("Persisted Book")
            .author("Reader")
            .publishedYear(2026)
            .build());

        assertThat(response.getBookId()).isEqualTo(42L);
        assertThat(response.getTitle()).isEqualTo("Persisted Book");
        assertThat(mapper.inserted.getUserId()).isEqualTo(1L);
        assertThat(mapper.inserted.getSource()).isEqualTo("ai");
        assertThat(mapper.inserted.getSourceRef()).isEqualTo("candidate-1");
    }

    private static class FakeBookMapper implements BookMapper {
        private BookRecord inserted;

        @Override
        public int insert(BookRecord record) {
            this.inserted = record;
            record.setId(42L);
            return 1;
        }
    }

    private static class NoopAiProvider implements AiProvider {
        @Override
        public BookCandidateSearchResponse suggestBooks(String query) {
            return BookCandidateSearchResponse.builder().build();
        }

        @Override
        public AiMessageResponse answerWindowMessage(Long windowId, SendMessageRequest request) {
            return null;
        }

        @Override
        public AiMessageResponse answerDebateMessage(Long windowId, DebateMessageRequest request) {
            return null;
        }
    }
}
