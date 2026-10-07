package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.api.dto.request.CreateVideoPracticeAttemptRequest;
import com.ieltspath.assessment.api.dto.response.VideoPracticeAttemptResponse;
import com.ieltspath.assessment.application.command.CreateVideoPracticeAttemptCommand;
import com.ieltspath.assessment.application.usecase.SubmitVideoPracticeUseCase;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assessments/video-practice")
@RequiredArgsConstructor
public class VideoPracticeController {

    private final CurrentUserProvider currentUser;
    private final SubmitVideoPracticeUseCase submitVideoPracticeUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VideoPracticeAttemptResponse create(
            @Valid @RequestBody CreateVideoPracticeAttemptRequest request) {
        var result = submitVideoPracticeUseCase.execute(new CreateVideoPracticeAttemptCommand(
                currentUser.requireUserId(), request.videoId(), request.segmentId(),
                request.practiceType(), request.referenceTextSnapshot()));
        return VideoPracticeAttemptResponse.from(result);
    }
}
