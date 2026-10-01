package com.group01.assessment.api.controller;

import com.group01.assessment.api.dto.response.AssessmentResultResponse;
import com.group01.assessment.application.usecase.GetAssessmentResultUseCase;
import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/assessments/attempts/{attemptId}/result")
@RequiredArgsConstructor
public class AssessmentResultController {

    private final CurrentUserProvider currentUser;
    private final GetAssessmentResultUseCase getAssessmentResultUseCase;

    @GetMapping
    public AssessmentResultResponse get(@PathVariable("attemptId") UUID attemptId) {
        return AssessmentResultResponse.from(getAssessmentResultUseCase.execute(currentUser.requireUserId(), attemptId));
    }
}
