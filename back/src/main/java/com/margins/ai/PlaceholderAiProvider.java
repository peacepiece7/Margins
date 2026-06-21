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
                .questionText(focus + "에서 어떤 디테일이 이 책을 이해하는 방식을 바꾸었나요?")
                .build(),
            QuestionDto.builder()
                .windowId(windowId)
                .questionType("evidence")
                .status("active")
                .aiModel("placeholder")
                .questionText("지금의 해석을 뒷받침하는 근거로 어떤 구절을 고르겠나요?")
                .build(),
            QuestionDto.builder()
                .windowId(windowId)
                .questionType("connection")
                .status("active")
                .aiModel("placeholder")
                .questionText("다음 대화에서는 어떤 긴장감이나 대비를 더 탐구하면 좋을까요?")
                .build()
        );

        return QuestionListResponse.builder()
            .questions(questions.subList(0, Math.min(count, questions.size())))
            .build();
    }

    @Override
    public AiMessageResponse answerWindowMessage(Long windowId, SendMessageRequest request) {
        String content = "AI 연결이 준비되지 않아 임시 응답입니다. "
            + "남긴 답변의 핵심 근거를 하나 고르고, 그 근거가 해석을 어떻게 바꾸는지 이어서 정리해 보세요. "
            + "입력: "
            + summarize(request.getContent());

        return AiMessageResponse.builder()
            .messageId(null)
            .windowId(windowId)
            .role("assistant")
            .content(content)
            .streamingReady(true)
            .aiModel("placeholder")
            .build();
    }

    @Override
    public AiMessageResponse answerDebateMessage(Long windowId, DebateMessageRequest request) {
        String content = "AI 연결이 준비되지 않아 임시 토론 응답입니다. "
            + "먼저 주장과 근거를 분리해 보겠습니다. "
            + "반대 관점에서 확인할 질문은 이 해석을 뒷받침하는 장면이 충분한지입니다. "
            + "입력: "
            + summarize(request.getContent());

        return AiMessageResponse.builder()
            .messageId(null)
            .windowId(windowId)
            .role("assistant")
            .personaId(request.getPersonaId())
            .content(content)
            .streamingReady(true)
            .aiModel("placeholder")
            .build();
    }

    private String summarize(String content) {
        if (content == null || content.isBlank()) {
            return "내용 없음";
        }
        String trimmed = content.trim();
        return trimmed.length() <= 80 ? trimmed : trimmed.substring(0, 80) + "...";
    }
}
