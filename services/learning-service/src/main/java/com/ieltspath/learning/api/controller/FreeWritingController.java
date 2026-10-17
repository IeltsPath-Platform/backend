package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.dto.request.SubmitFreeWritingRequest;
import com.ieltspath.learning.api.dto.response.WritingSubmissionResponse;
import com.ieltspath.learning.application.service.WritingSettings;
import com.ieltspath.learning.application.usecase.SubmitFreeWritingUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Writing practice outside lessons: the learner writes for a library prompt and pays points for the AI grade. */
@RestController
@RequestMapping("/api/learning/free-writing")
@RequiredArgsConstructor
public class FreeWritingController {
    private final CurrentUserProvider currentUser;
    private final SubmitFreeWritingUseCase submitFreeWriting;
    private final WritingSettings settings;

    /** What grading costs and the shortest essay accepted, so the confirm dialog can show them before sending. */
    @GetMapping("/grading-terms")
    public GradingTerms terms() {
        return new GradingTerms(settings.pointCost(), settings.minWords(), settings.maxWords());
    }

    @PostMapping("/submissions")
    public WritingSubmissionResponse submit(@Valid @RequestBody SubmitFreeWritingRequest request) {
        return WritingSubmissionResponse.from(submitFreeWriting.execute(currentUser.requireUserId(),
                request.packageVersionId(), request.questionVersionId(), request.requestId(), request.essayText()));
    }

    public record GradingTerms(int pointCost, int minWords, int maxWords) {}
}
