package com.margins;

import static org.assertj.core.api.Assertions.assertThat;

import com.margins.ai.AiProvider;
import com.margins.book.dto.BookCandidateSearchResponse;
import com.margins.message.mapper.MessageMapper;
import com.margins.message.model.MessageRecord;
import com.margins.session.business.SessionWindowBusiness;
import com.margins.session.dto.AiMessageResponse;
import com.margins.session.dto.CreateSessionWindowRequest;
import com.margins.session.dto.CreateSessionWindowResponse;
import com.margins.session.dto.DebateMessageRequest;
import com.margins.session.dto.SendMessageRequest;
import com.margins.session.mapper.SessionWindowMapper;
import com.margins.session.model.SessionWindowContext;
import com.margins.session.model.SessionWindowRecord;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SessionWindowBusinessPersistenceTest {

    @Test
    void createPersistsAndReturnsGeneratedId() {
        FakeSessionWindowMapper windowMapper = new FakeSessionWindowMapper();
        SessionWindowBusiness business = new SessionWindowBusiness(
            new StubAiProvider(),
            windowMapper,
            new FakeMessageMapper()
        );

        CreateSessionWindowResponse response = business.create(CreateSessionWindowRequest.builder()
            .sessionId(3L)
            .windowType("question")
            .title("Question")
            .build());

        assertThat(response.getWindowId()).isEqualTo(300L);
        assertThat(response.getSessionId()).isEqualTo(3L);
        assertThat(response.getStatus()).isEqualTo("open");
        assertThat(windowMapper.inserted.getPosition()).isEqualTo(5);
    }

    @Test
    void sendMessagePersistsUserAndAssistantMessages() {
        FakeMessageMapper messageMapper = new FakeMessageMapper();
        SessionWindowBusiness business = new SessionWindowBusiness(
            new StubAiProvider(),
            new FakeSessionWindowMapper(),
            messageMapper
        );

        AiMessageResponse response = business.sendMessage(10L, SendMessageRequest.builder()
            .content("What matters?")
            .questionId(9L)
            .build());

        assertThat(response.getMessageId()).isEqualTo(101L);
        assertThat(messageMapper.inserted).hasSize(2);
        assertThat(messageMapper.inserted.get(0).getRole()).isEqualTo("user");
        assertThat(messageMapper.inserted.get(0).getMessageOrder()).isEqualTo(1);
        assertThat(messageMapper.inserted.get(1).getRole()).isEqualTo("assistant");
        assertThat(messageMapper.inserted.get(1).getParentMessageId()).isEqualTo(100L);
        assertThat(messageMapper.inserted.get(1).getQuestionId()).isEqualTo(9L);
        assertThat(messageMapper.orderWindowIds).containsExactly(10L, 10L);
    }

    @Test
    void debatePersistsPersonaResponse() {
        FakeMessageMapper messageMapper = new FakeMessageMapper();
        SessionWindowBusiness business = new SessionWindowBusiness(
            new StubAiProvider(),
            new FakeSessionWindowMapper(),
            messageMapper
        );

        AiMessageResponse response = business.debate(10L, DebateMessageRequest.builder()
            .personaId(4L)
            .content("Challenge me")
            .build());

        assertThat(response.getMessageId()).isEqualTo(101L);
        assertThat(response.getPersonaId()).isEqualTo(4L);
        assertThat(messageMapper.inserted.get(1).getPersonaId()).isEqualTo(4L);
    }

    private static class FakeSessionWindowMapper implements SessionWindowMapper {
        private SessionWindowRecord inserted;

        @Override
        public int insert(SessionWindowRecord record) {
            this.inserted = record;
            record.setId(300L);
            return 1;
        }

        @Override
        public SessionWindowContext findContextById(Long id) {
            return new SessionWindowContext(id, 30L, 1L);
        }

        @Override
        public int selectNextPosition(Long sessionId) {
            return 5;
        }
    }

    private static class FakeMessageMapper implements MessageMapper {
        private final List<MessageRecord> inserted = new ArrayList<>();
        private final List<Long> orderWindowIds = new ArrayList<>();
        private int nextId = 100;
        private int nextOrder = 1;

        @Override
        public int insert(MessageRecord record) {
            record.setId((long) nextId++);
            inserted.add(record);
            return 1;
        }

        @Override
        public int selectNextOrder(Long sessionId, Long windowId) {
            orderWindowIds.add(windowId);
            return nextOrder++;
        }
    }

    private static class StubAiProvider implements AiProvider {
        @Override
        public BookCandidateSearchResponse suggestBooks(String query) {
            return BookCandidateSearchResponse.builder().build();
        }

        @Override
        public AiMessageResponse answerWindowMessage(Long windowId, SendMessageRequest request) {
            return AiMessageResponse.builder()
                .messageId(null)
                .windowId(windowId)
                .role("assistant")
                .content("Answer")
                .streamingReady(true)
                .aiModel("placeholder")
                .build();
        }

        @Override
        public AiMessageResponse answerDebateMessage(Long windowId, DebateMessageRequest request) {
            return AiMessageResponse.builder()
                .messageId(null)
                .windowId(windowId)
                .personaId(request.getPersonaId())
                .role("assistant")
                .content("Debate")
                .streamingReady(true)
                .aiModel("placeholder")
                .build();
        }
    }
}
