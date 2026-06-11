package com.margins.ai;

import com.margins.book.dto.BookCandidateDto;
import com.margins.book.dto.BookCandidateSearchResponse;
import com.margins.session.dto.AiMessageResponse;
import com.margins.session.dto.DebateMessageRequest;
import com.margins.session.dto.SendMessageRequest;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PlaceholderAiProvider implements AiProvider {

    @Override
    public BookCandidateSearchResponse suggestBooks(String query) {
        return BookCandidateSearchResponse.builder()
            .aiModel("placeholder")
            .candidates(List.of(BookCandidateDto.builder()
                .candidateId("placeholder-1")
                .title(query == null || query.isBlank() ? "Untitled Candidate" : query)
                .author("AI Candidate")
                .reason("Placeholder candidate until OpenAI integration is wired.")
                .build()))
            .build();
    }

    @Override
    public AiMessageResponse answerWindowMessage(Long windowId, SendMessageRequest request) {
        return AiMessageResponse.builder()
            .messageId(1L)
            .windowId(windowId)
            .role("assistant")
            .content("Placeholder AI response. OpenAI integration will replace this boundary.")
            .streamingReady(true)
            .aiModel("placeholder")
            .build();
    }

    @Override
    public AiMessageResponse answerDebateMessage(Long windowId, DebateMessageRequest request) {
        return AiMessageResponse.builder()
            .messageId(1L)
            .windowId(windowId)
            .role("assistant")
            .personaId(request.getPersonaId())
            .content("Placeholder persona response. Persona prompt wiring will replace this boundary.")
            .streamingReady(true)
            .aiModel("placeholder")
            .build();
    }
}
