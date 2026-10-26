package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.dto.request.SubmitFreeWritingRequest;
import com.ieltspath.learning.api.dto.request.SubmitFreeWritingToExaminerRequest;
import com.ieltspath.learning.api.dto.response.WritingSubmissionResponse;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.service.WritingSettings;
import com.ieltspath.learning.application.usecase.SubmitFreeWritingToExaminerUseCase;
import com.ieltspath.learning.application.usecase.SubmitFreeWritingUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Writing practice outside lessons: the learner writes for a library prompt and pays points for the AI grade. */
@RestController
@RequestMapping("/api/learning/free-writing")
@RequiredArgsConstructor
public class FreeWritingController {
    private final CurrentUserProvider currentUser;
    private final SubmitFreeWritingUseCase submitFreeWriting;
    private final SubmitFreeWritingToExaminerUseCase submitToExaminer;
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

    /**
     * Sends the essay to an examiner instead of the AI. Needs Premium with a grading credit left (402
     * {@code HUMAN_GRADING_CREDIT_REQUIRED}); the chosen examiner may refuse (409 {@code EXAMINER_UNAVAILABLE}). The result
     * is AWAITING_EXAMINER; the review is read from {@code /api/learning/writing-submissions/{id}/human-review}.
     */
    @PostMapping("/examiner-submissions")
    public WritingSubmissionResponse submitToExaminer(@Valid @RequestBody SubmitFreeWritingToExaminerRequest request) {
        return WritingSubmissionResponse.from(submitToExaminer.execute(currentUser.requireUserId(),
                request.packageVersionId(), request.questionVersionId(), request.requestId(), request.essayText(),
                request.examinerId()));
    }

    /**
     * Whether the learner's last essay for this prompt still waits for its examiner, so the screen can say so before they
     * write again. When Assessment cannot be reached the answer is "not pending": sending checks again and refuses.
     */
    @GetMapping("/examiner-submissions/pending")
    public PendingExaminerSubmission pending(@RequestParam("questionVersionId") UUID questionVersionId) {
        try {
            return submitToExaminer.pending(currentUser.requireUserId(), questionVersionId)
                    .map(id -> new PendingExaminerSubmission(true, id)).orElse(new PendingExaminerSubmission(false, null));
        } catch (LearningRequestException unavailable) {
            return new PendingExaminerSubmission(false, null);
        }
    }

    public record PendingExaminerSubmission(boolean pending, UUID submissionId) {}

    public record GradingTerms(int pointCost, int minWords, int maxWords) {}
}
