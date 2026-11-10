package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.dto.request.OpenPracticeSessionRequest;
import com.ieltspath.learning.api.dto.response.PracticeSessionResponse;
import com.ieltspath.learning.application.usecase.OpenPracticeSessionUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learning/practice-sessions")
@RequiredArgsConstructor
public class PracticeSessionController {
    private final CurrentUserProvider currentUser;
    private final OpenPracticeSessionUseCase open;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public PracticeSessionResponse open(@Valid @RequestBody OpenPracticeSessionRequest request) {
        return PracticeSessionResponse.from(open.execute(currentUser.requireUserId(), request.kind(), request.requestId()));
    }
}
