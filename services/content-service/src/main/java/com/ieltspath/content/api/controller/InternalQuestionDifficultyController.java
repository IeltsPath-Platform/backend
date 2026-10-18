package com.ieltspath.content.api.controller;

import com.ieltspath.content.api.dto.internal.QuestionDifficultiesRequest;
import com.ieltspath.content.api.dto.internal.QuestionDifficultiesResponse;
import com.ieltspath.content.application.usecase.GetQuestionDifficultiesUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/learning-content/questions")
@RequiredArgsConstructor
public class InternalQuestionDifficultyController {
    private final GetQuestionDifficultiesUseCase getQuestionDifficulties;

    @PostMapping("/difficulties")
    public QuestionDifficultiesResponse questionDifficulties(
            @Valid @RequestBody QuestionDifficultiesRequest request) {
        return new QuestionDifficultiesResponse(getQuestionDifficulties.execute(request.questionVersionIds()));
    }
}
