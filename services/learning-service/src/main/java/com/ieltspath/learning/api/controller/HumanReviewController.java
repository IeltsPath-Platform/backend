package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.port.HumanReviewClient;
import com.ieltspath.learning.application.usecase.HumanReviewUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Mentor review of the learner's own graded lesson or Practice essays (contract {@code human-review-v1}). */
@RestController
@RequestMapping("/api/learning/writing-submissions/{id}/human-review")
@PreAuthorize("hasRole('CUSTOMER')")
public class HumanReviewController {
    private final CurrentUserProvider currentUser;
    private final HumanReviewUseCase reviews;

    public HumanReviewController(CurrentUserProvider currentUser, HumanReviewUseCase reviews) {
        this.currentUser = currentUser;
        this.reviews = reviews;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Requested request(@PathVariable("id") UUID id) {
        return new Requested(reviews.request(currentUser.requireUserId(), id), "REQUESTED");
    }

    @GetMapping
    public HumanReviewClient.Review review(@PathVariable("id") UUID id) {
        return reviews.review(currentUser.requireUserId(), id);
    }

    public record Requested(UUID reviewSubmissionId, String status) {}
}
