package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.dto.request.SubmitEssayRequest;
import com.ieltspath.learning.api.dto.response.WritingSubmissionResponse;
import com.ieltspath.learning.application.usecase.GetWritingSubmissionUseCase;
import com.ieltspath.learning.application.usecase.SubmitLessonEssayUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Lesson essays, graded by the LLM within the request (contract {@code lesson-writing-v1}). */
@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class WritingController {
    private final CurrentUserProvider currentUser;
    private final GetWritingSubmissionUseCase getWritingSubmissionUseCase;
    private final SubmitLessonEssayUseCase submitLessonEssayUseCase;

    @PostMapping("/lessons/{lessonId}/essays/{blockId}/submissions")
    public WritingSubmissionResponse submit(@PathVariable("lessonId") UUID lessonId,
                                            @PathVariable("blockId") UUID blockId,
                                            @Valid @RequestBody SubmitEssayRequest request) {
        return WritingSubmissionResponse.from(submitLessonEssayUseCase.execute(currentUser.requireUserId(), lessonId, blockId,
                request.requestId(), request.essayText()));
    }

    @GetMapping("/writing-submissions/{id}")
    public WritingSubmissionResponse get(@PathVariable("id") UUID id) {
        return WritingSubmissionResponse.from(getWritingSubmissionUseCase.execute(currentUser.requireUserId(), id));
    }
}
