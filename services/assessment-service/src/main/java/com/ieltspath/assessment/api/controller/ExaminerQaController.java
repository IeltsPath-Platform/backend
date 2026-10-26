package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.api.dto.request.SubmitExaminerQaReviewRequest;
import com.ieltspath.assessment.application.port.ExaminerQueueStore;
import com.ieltspath.assessment.application.service.ExaminerQaService;
import com.ieltspath.assessment.application.usecase.ClaimGradingJobUseCase;
import com.ieltspath.assessment.application.usecase.GetJobEssayUseCase;
import com.ieltspath.assessment.application.usecase.ListGradingQueueUseCase;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/assessments/qa-reviews")
@PreAuthorize("hasRole('CONTENT_AUTHOR')")
public class ExaminerQaController {
    private final CurrentUserProvider currentUser;
    private final ListGradingQueueUseCase queue;
    private final ClaimGradingJobUseCase claim;
    private final GetJobEssayUseCase essay;
    private final ExaminerQaService qa;

    public ExaminerQaController(CurrentUserProvider currentUser, ListGradingQueueUseCase queue,
                                ClaimGradingJobUseCase claim, GetJobEssayUseCase essay,
                                ExaminerQaService qa) {
        this.currentUser = currentUser;
        this.queue = queue;
        this.claim = claim;
        this.essay = essay;
        this.qa = qa;
    }

    @GetMapping("/queue")
    public ExaminerQueueStore.QueuePage queue(@RequestParam(name = "page", defaultValue = "0") int page,
                                               @RequestParam(name = "size", defaultValue = "20") int size) {
        return queue.execute("HUMAN", "QUEUED", page, size, "QA_SAMPLE");
    }

    /** Essays the caller claimed and has not reviewed yet, so they can be found again after leaving the page. */
    @GetMapping("/mine")
    public ExaminerQueueStore.QueuePage mine(@RequestParam(name = "page", defaultValue = "0") int page,
                                              @RequestParam(name = "size", defaultValue = "20") int size) {
        return queue.claimedQaBy(currentUser.requireUserId(), page, size);
    }

    @PostMapping("/{jobId}/claim")
    public ExaminerQueueStore.QueueEntry claim(@PathVariable("jobId") UUID jobId) {
        return claim.execute(currentUser.requireUserId(), jobId);
    }

    @GetMapping("/{jobId}/essay")
    public ExaminerQueueStore.Essay essay(@PathVariable("jobId") UUID jobId) {
        return essay.execute(currentUser.requireUserId(), jobId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void review(@Valid @RequestBody SubmitExaminerQaReviewRequest request) {
        qa.submitReview(request.gradingJobId(), currentUser.requireUserId(), request.reviewerBand(),
                request.reviewerCriteria().toString(), request.note());
    }
}
