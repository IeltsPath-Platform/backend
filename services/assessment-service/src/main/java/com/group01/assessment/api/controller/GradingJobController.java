package com.group01.assessment.api.controller;

import com.group01.assessment.api.dto.request.CreateGradingJobRequest;
import com.group01.assessment.api.dto.response.GradingJobResponse;
import com.group01.assessment.application.command.CreateGradingJobCommand;
import com.group01.assessment.application.usecase.CreateGradingJobUseCase;
import com.group01.assessment.application.usecase.GetGradingJobUseCase;
import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/assessments/grading-jobs")
@RequiredArgsConstructor
public class GradingJobController {

    private final CurrentUserProvider currentUser;
    private final GetGradingJobUseCase getGradingJobUseCase;
    private final CreateGradingJobUseCase createGradingJobUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GradingJobResponse create(@Valid @RequestBody CreateGradingJobRequest request) {
        var result = createGradingJobUseCase.execute(new CreateGradingJobCommand(
                currentUser.requireUserId(),
                request.submissionId(),
                request.skill(),
                request.gradingMode(),
                request.pointCostSnapshot(),
                request.idempotencyKey()
        ));
        return GradingJobResponse.from(result);
    }

    @GetMapping("/{id}")
    public GradingJobResponse get(@PathVariable("id") UUID id) {
        return GradingJobResponse.from(getGradingJobUseCase.execute(currentUser.requireUserId(), id));
    }
}
