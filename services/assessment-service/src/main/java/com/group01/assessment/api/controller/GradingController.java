package com.group01.assessment.api.controller;

import com.group01.assessment.api.dto.request.OpenResultVersionRequest;
import com.group01.assessment.api.dto.request.SaveGradingDetailsRequest;
import com.group01.assessment.api.dto.response.AssessmentResultResponse;
import com.group01.assessment.application.command.FinalizeAssessmentResultCommand;
import com.group01.assessment.application.usecase.CreateAssessmentResultUseCase;
import com.group01.assessment.application.usecase.FinalizeAssessmentResultUseCase;
import com.group01.assessment.application.usecase.SaveAssessmentResultDetailsUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Grader lifecycle of a result version: open (or regrade) → save details → finalize. Finalizing emits
 * AssessmentCompleted.v2 through the outbox. Any EXAMINER can grade any result; there is no assignment model yet.
 */
@RestController
@RequestMapping("/api/assessments/grading")
@PreAuthorize("hasAnyRole('EXAMINER','ADMIN')")
@RequiredArgsConstructor
public class GradingController {

    private final CreateAssessmentResultUseCase createAssessmentResultUseCase;
    private final SaveAssessmentResultDetailsUseCase saveAssessmentResultDetailsUseCase;
    private final FinalizeAssessmentResultUseCase finalizeAssessmentResultUseCase;

    @PostMapping("/attempts/{attemptId}/results")
    @ResponseStatus(HttpStatus.CREATED)
    public AssessmentResultResponse openResultVersion(
            @PathVariable("attemptId") UUID attemptId,
            @Valid @RequestBody(required = false) OpenResultVersionRequest request
    ) {
        Double overallBand = request == null ? null : request.overallBand();
        return AssessmentResultResponse.from(createAssessmentResultUseCase.executeForGrader(attemptId, overallBand));
    }

    @PutMapping("/results/{resultId}/details")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveDetails(
            @PathVariable("resultId") UUID resultId,
            @Valid @RequestBody SaveGradingDetailsRequest request
    ) {
        saveAssessmentResultDetailsUseCase.executeForGrader(request.toCommand(resultId));
    }

    @PostMapping("/results/{resultId}/finalize")
    public AssessmentResultResponse finalizeResult(@PathVariable("resultId") UUID resultId) {
        return AssessmentResultResponse.from(
                finalizeAssessmentResultUseCase.execute(new FinalizeAssessmentResultCommand(resultId)));
    }
}
