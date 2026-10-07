package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.api.dto.request.CreateLearnerSubmissionRequest;
import com.ieltspath.assessment.api.dto.response.LearnerSubmissionResponse;
import com.ieltspath.assessment.application.command.CreateLearnerSubmissionCommand;
import com.ieltspath.assessment.application.usecase.CreateLearnerSubmissionUseCase;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assessments/submissions")
@RequiredArgsConstructor
public class LearnerSubmissionController {

    private final CurrentUserProvider currentUser;
    private final CreateLearnerSubmissionUseCase createLearnerSubmissionUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LearnerSubmissionResponse create(@Valid @RequestBody CreateLearnerSubmissionRequest request) {
        var result = createLearnerSubmissionUseCase.execute(new CreateLearnerSubmissionCommand(
                currentUser.requireUserId(),
                request.attemptItemId(),
                request.promptSnapshot(),
                request.skill(),
                request.textPayload(),
                request.audioReference(),
                request.submissionKey()
        ));
        return LearnerSubmissionResponse.from(result);
    }
}
