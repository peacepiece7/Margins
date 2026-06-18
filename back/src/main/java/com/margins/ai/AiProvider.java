package com.margins.ai;

import com.margins.book.dto.BookCandidateSearchResponse;
import com.margins.persona.dto.GeneratePersonasRequest;
import com.margins.persona.dto.PersonaDraftListResponse;
import com.margins.question.dto.GenerateQuestionsRequest;
import com.margins.question.dto.QuestionListResponse;
import com.margins.session.dto.AiMessageResponse;
import com.margins.session.dto.DebateMessageRequest;
import com.margins.session.dto.SendMessageRequest;
import java.util.function.Consumer;

public interface AiProvider {
    BookCandidateSearchResponse suggestBooks(String query);

    PersonaDraftListResponse suggestPersonas(GeneratePersonasRequest request);

    QuestionListResponse suggestQuestions(Long windowId, GenerateQuestionsRequest request);

    AiMessageResponse answerWindowMessage(Long windowId, SendMessageRequest request);

    default AiMessageResponse streamWindowMessage(Long windowId, SendMessageRequest request, Consumer<String> deltaConsumer) {
        AiMessageResponse response = answerWindowMessage(windowId, request);
        chunks(response.getContent()).forEach(deltaConsumer);
        return response;
    }

    AiMessageResponse answerDebateMessage(Long windowId, DebateMessageRequest request);

    private Iterable<String> chunks(String content) {
        String safeContent = content == null ? "" : content;
        int chunkSize = 24;
        java.util.Map<Integer, String> result = new java.util.LinkedHashMap<>();
        for (int start = 0; start < safeContent.length(); start += chunkSize) {
            int end = Math.min(start + chunkSize, safeContent.length());
            result.put(start, safeContent.substring(start, end));
        }
        if (result.isEmpty()) {
            result.put(0, "");
        }
        return result.values();
    }
}
