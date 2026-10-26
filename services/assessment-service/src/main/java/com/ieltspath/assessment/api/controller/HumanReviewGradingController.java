package com.ieltspath.assessment.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieltspath.assessment.api.dto.request.CompleteHumanReviewRequest;
import com.ieltspath.assessment.api.dto.response.HumanReviewDraftResponse;
import com.ieltspath.assessment.application.service.HumanReviewService;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/assessments/grading-jobs")
@PreAuthorize("hasRole('EXAMINER')")
public class HumanReviewGradingController {
    private final CurrentUserProvider currentUser;
    private final HumanReviewService reviews;

    public HumanReviewGradingController(CurrentUserProvider currentUser, HumanReviewService reviews) {
        this.currentUser = currentUser;
        this.reviews = reviews;
    }

    @PostMapping("/{jobId}/human-review")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void complete(@PathVariable("jobId") UUID jobId,
                         @Valid @RequestBody CompleteHumanReviewRequest request) {
        reviews.complete(jobId, currentUser.requireUserId(), request.toCommand());
    }

    /** Saves the review being written; the body is a JSON object shaped like the final request, parts may be missing. */
    @PutMapping("/{jobId}/human-review/draft")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveDraft(@PathVariable("jobId") UUID jobId, @RequestBody JsonNode payload) {
        reviews.saveDraft(jobId, currentUser.requireUserId(), payload);
    }

    /** The saved draft of the examiner who claimed the job, or 204 when none was saved. */
    @GetMapping("/{jobId}/human-review/draft")
    public ResponseEntity<HumanReviewDraftResponse> draft(@PathVariable("jobId") UUID jobId) {
        return reviews.draft(jobId, currentUser.requireUserId())
                .map(draft -> ResponseEntity.ok(HumanReviewDraftResponse.from(draft)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
