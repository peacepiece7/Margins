package com.margins.ai;

import com.margins.book.dto.BookCandidateSearchResponse;
import com.margins.session.dto.AiMessageResponse;
import com.margins.session.dto.DebateMessageRequest;
import com.margins.session.dto.SendMessageRequest;

public interface AiProvider {
    BookCandidateSearchResponse suggestBooks(String query);

    AiMessageResponse answerWindowMessage(Long windowId, SendMessageRequest request);

    AiMessageResponse answerDebateMessage(Long windowId, DebateMessageRequest request);
}
