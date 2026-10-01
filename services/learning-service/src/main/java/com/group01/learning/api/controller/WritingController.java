package com.group01.learning.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.learning.api.dto.SubmitEssayRequest;
import com.group01.learning.api.dto.WritingSubmissionResponse;
import com.group01.learning.application.usecase.LessonEssayUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Lesson essays, graded by the LLM within the request (contract {@code lesson-writing-v1}). */
@RestController
@RequestMapping("/api/learning")
public class WritingController {
    private final CurrentUserProvider currentUser;
    private final LessonEssayUseCase essays;

    public WritingController(CurrentUserProvider currentUser, LessonEssayUseCase essays) {
        this.currentUser = currentUser;
        this.essays = essays;
    }

    @PostMapping("/lessons/{lessonId}/essays/{blockId}/submissions")
    public WritingSubmissionResponse submit(@PathVariable("lessonId") UUID lessonId,
                                            @PathVariable("blockId") UUID blockId,
                                            @Valid @RequestBody SubmitEssayRequest request) {
        return WritingSubmissionResponse.from(essays.submit(currentUser.requireUserId(), lessonId, blockId,
                request.requestId(), request.essayText()));
    }

    @GetMapping("/writing-submissions/{id}")
    public WritingSubmissionResponse get(@PathVariable("id") UUID id) {
        return WritingSubmissionResponse.from(essays.get(currentUser.requireUserId(), id));
    }
}
