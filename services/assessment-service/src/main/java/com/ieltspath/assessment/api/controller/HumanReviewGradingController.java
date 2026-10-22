package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.api.dto.request.CompleteHumanReviewRequest;
import com.ieltspath.assessment.application.service.HumanReviewService;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
}
