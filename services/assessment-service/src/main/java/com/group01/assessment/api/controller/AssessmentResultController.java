package com.group01.assessment.api.controller;

import com.group01.assessment.api.dto.request.CreateAssessmentResultRequest;
import com.group01.assessment.api.dto.response.AssessmentResultResponse;
import com.group01.assessment.application.command.CreateAssessmentResultCommand;
import com.group01.assessment.application.usecase.CreateAssessmentResultUseCase;
import com.group01.assessment.application.usecase.GetAssessmentResultUseCase;
import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/assessments/attempts/{attemptId}/result")
@RequiredArgsConstructor
public class AssessmentResultController {

    private final CurrentUserProvider currentUser;
    private final CreateAssessmentResultUseCase createAssessmentResultUseCase;
    private final GetAssessmentResultUseCase getAssessmentResultUseCase;

    @PostMapping
    public AssessmentResultResponse create(
            @PathVariable("attemptId") UUID attemptId,
            @Valid @RequestBody CreateAssessmentResultRequest request
    ) {
        var result = createAssessmentResultUseCase.execute(new CreateAssessmentResultCommand(
                currentUser.requireUserId(), attemptId, request.overallBand()
        ));
        return AssessmentResultResponse.from(result);
    }

    @GetMapping
    public AssessmentResultResponse get(@PathVariable("attemptId") UUID attemptId) {
        return AssessmentResultResponse.from(getAssessmentResultUseCase.execute(currentUser.requireUserId(), attemptId));
    }
}
