package com.margins.ai;

import com.margins.book.dto.BookCandidateDto;
import com.margins.book.dto.BookCandidateSearchResponse;
import com.margins.question.dto.GenerateQuestionsRequest;
import com.margins.question.dto.QuestionDto;
import com.margins.question.dto.QuestionListResponse;
import com.margins.session.dto.AiMessageResponse;
import com.margins.session.dto.DebateMessageRequest;
import com.margins.session.dto.SendMessageRequest;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "margins.ai.provider", havingValue = "placeholder", matchIfMissing = true)
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
    public QuestionListResponse suggestQuestions(Long windowId, GenerateQuestionsRequest request) {
        int count = request.getCount() == null ? 3 : request.getCount();
        String focus = request.getFocus() == null || request.getFocus().isBlank()
            ? "this reading"
            : request.getFocus();

        List<QuestionDto> questions = List.of(
            QuestionDto.builder()
                .windowId(windowId)
                .questionType("reflection")
                .status("active")
                .aiModel("placeholder")
                .questionText("What detail from " + focus + " changed how you understood the book?")
                .build(),
            QuestionDto.builder()
                .windowId(windowId)
                .questionType("evidence")
                .status("active")
                .aiModel("placeholder")
                .questionText("Which passage would you use as evidence for your current interpretation?")
                .build(),
            QuestionDto.builder()
                .windowId(windowId)
                .questionType("connection")
                .status("active")
                .aiModel("placeholder")
                .questionText("What tension or contrast should the next discussion explore?")
                .build()
        );

        return QuestionListResponse.builder()
            .questions(questions.subList(0, Math.min(count, questions.size())))
            .build();
    }

    @Override
    public AiMessageResponse answerWindowMessage(Long windowId, SendMessageRequest request) {
        return AiMessageResponse.builder()
            .messageId(null)
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
            .messageId(null)
            .windowId(windowId)
            .role("assistant")
            .personaId(request.getPersonaId())
            .content("Placeholder persona response. Persona prompt wiring will replace this boundary.")
            .streamingReady(true)
            .aiModel("placeholder")
            .build();
    }
}
