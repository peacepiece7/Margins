package com.margins.ai;

import com.margins.book.dto.BookCandidateDto;
import com.margins.book.dto.BookCandidateSearchResponse;
import com.margins.persona.dto.GeneratePersonasRequest;
import com.margins.persona.dto.PersonaDraftDto;
import com.margins.persona.dto.PersonaDraftListResponse;
import com.margins.persona.model.PersonaRoleCatalog;
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
    public PersonaDraftListResponse suggestPersonas(GeneratePersonasRequest request) {
        int count = request.getCount() == null ? 3 : request.getCount();
        String bookTitle = request.getBookTitle() == null || request.getBookTitle().isBlank()
            ? "the selected book"
            : request.getBookTitle();
        String readingGoal = request.getReadingGoal() == null || request.getReadingGoal().isBlank()
            ? "the reader's interpretation"
            : request.getReadingGoal();

        List<PersonaDraftDto> personas = List.of(
            PersonaDraftDto.builder()
                .displayName("Context Analyst")
                .description("Tracks structure, context, and recurring evidence in " + bookTitle + ".")
                .tone("analytical")
                .roleKey(PersonaRoleCatalog.EVIDENCE_ANALYST)
                .systemPrompt("You are a context analyst for " + bookTitle + ". Ground each reply in textual evidence and session context.")
                .reason("Balances the discussion with evidence-focused interpretation.")
                .build(),
            PersonaDraftDto.builder()
                .displayName("Skeptical Reader")
                .description("Challenges assumptions and asks what the text does not yet prove.")
                .tone("critical")
                .roleKey(PersonaRoleCatalog.SKEPTIC)
                .systemPrompt("You are a skeptical reader. Challenge weak claims about " + bookTitle + " while staying concise and constructive.")
                .reason("Adds productive disagreement before the reader settles on a conclusion.")
                .build(),
            PersonaDraftDto.builder()
                .displayName("Connection Builder")
                .description("Connects the current reading goal to themes, characters, and prior notes.")
                .tone("synthetic")
                .roleKey(PersonaRoleCatalog.CONNECTOR)
                .systemPrompt("You are a connection builder. Link details from " + bookTitle + " to the reading goal: " + readingGoal + ".")
                .reason("Helps turn isolated observations into reusable reading insight.")
                .build()
        );

        return PersonaDraftListResponse.builder()
            .aiModel("placeholder")
            .personas(personas.subList(0, Math.min(count, personas.size())))
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
