package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.dto.request.SubmitEssayRequest;
import com.ieltspath.learning.api.dto.response.WritingSubmissionResponse;
import com.ieltspath.learning.application.result.EssayHistoryResult;
import com.ieltspath.learning.application.usecase.GetEssayHistoryUseCase;
import com.ieltspath.learning.application.usecase.GetWritingSubmissionUseCase;
import com.ieltspath.learning.application.usecase.SubmitEssayRevisionUseCase;
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
    private final SubmitEssayRevisionUseCase submitEssayRevisionUseCase;
    private final GetEssayHistoryUseCase getEssayHistoryUseCase;

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

    /** Rewrites the graded essay {@code id} and sends it again; see contract {@code essay-revision-v1}. */
    @PostMapping("/writing-submissions/{id}/revisions")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('CUSTOMER')")
    public WritingSubmissionResponse revise(@PathVariable("id") UUID id,
                                            @Valid @RequestBody SubmitEssayRequest request) {
        return WritingSubmissionResponse.from(submitEssayRevisionUseCase.execute(currentUser.requireUserId(), id,
                request.requestId(), request.essayText()));
    }

    @GetMapping("/writing-submissions/{id}/history")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('CUSTOMER')")
    public EssayHistoryResult history(@PathVariable("id") UUID id) {
        return getEssayHistoryUseCase.execute(currentUser.requireUserId(), id);
    }
}
