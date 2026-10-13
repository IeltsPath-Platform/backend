package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.dto.response.WritingSubmissionResponse;
import com.ieltspath.learning.application.usecase.GetWritingSubmissionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Reads of graded Writing essays; practice essays are submitted through their practice attempt. */
@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class WritingController {
    private final CurrentUserProvider currentUser;
    private final GetWritingSubmissionUseCase getWritingSubmissionUseCase;

    @GetMapping("/writing-submissions/{id}")
    public WritingSubmissionResponse get(@PathVariable("id") UUID id) {
        return WritingSubmissionResponse.from(getWritingSubmissionUseCase.execute(currentUser.requireUserId(), id));
    }
}
