package com.margins;

import static org.assertj.core.api.Assertions.assertThat;

import com.margins.session.business.ReadingSessionBusiness;
import com.margins.session.dto.CreateReadingSessionRequest;
import com.margins.session.dto.CreateReadingSessionResponse;
import com.margins.session.mapper.ReadingSessionMapper;
import com.margins.session.model.ReadingSessionRecord;
import org.junit.jupiter.api.Test;

class ReadingSessionBusinessPersistenceTest {

    @Test
    void createPersistsAndReturnsGeneratedId() {
        FakeReadingSessionMapper mapper = new FakeReadingSessionMapper();
        ReadingSessionBusiness business = new ReadingSessionBusiness(mapper);

        CreateReadingSessionResponse response = business.create(CreateReadingSessionRequest.builder()
            .bookId(7L)
            .title("My Session")
            .build());

        assertThat(response.getSessionId()).isEqualTo(77L);
        assertThat(response.getBookId()).isEqualTo(7L);
        assertThat(response.getStatus()).isEqualTo("active");
        assertThat(mapper.inserted.getUserId()).isEqualTo(1L);
        assertThat(mapper.inserted.isTestData()).isTrue();
    }

    private static class FakeReadingSessionMapper implements ReadingSessionMapper {
        private ReadingSessionRecord inserted;

        @Override
        public int insert(ReadingSessionRecord record) {
            this.inserted = record;
            record.setId(77L);
            return 1;
        }
    }
}
