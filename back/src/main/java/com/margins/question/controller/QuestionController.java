package com.margins.question.controller;

import com.margins.common.dto.ApiResponse;
import com.margins.question.dto.QuestionDto;
import com.margins.session.service.SessionWindowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final SessionWindowService sessionWindowService;

    @DeleteMapping("/{id}")
    public ApiResponse<QuestionDto> delete(@PathVariable("id") Long questionId) {
        return ApiResponse.ok(sessionWindowService.deleteQuestion(questionId));
    }
}
