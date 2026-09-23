package com.group01.assessment.api.controller;

import com.group01.assessment.api.dto.request.CreateVideoPracticeAttemptRequest;
import com.group01.assessment.api.dto.response.VideoPracticeAttemptResponse;
import com.group01.assessment.application.command.CreateVideoPracticeAttemptCommand;
import com.group01.assessment.application.usecase.SubmitVideoPracticeUseCase;
import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assessments/video-practice")
@RequiredArgsConstructor
public class VideoPracticeController {

    private final CurrentUserProvider currentUser;
    private final SubmitVideoPracticeUseCase submitVideoPracticeUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VideoPracticeAttemptResponse create(
            @Valid @RequestBody CreateVideoPracticeAttemptRequest request
    ) {
        var result = submitVideoPracticeUseCase.execute(new CreateVideoPracticeAttemptCommand(
                currentUser.requireUserId(), request.videoId(), request.practiceType()
        ));
        return VideoPracticeAttemptResponse.from(result);
    }
}
