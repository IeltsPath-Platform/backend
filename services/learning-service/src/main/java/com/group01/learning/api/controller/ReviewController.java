package com.group01.learning.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.learning.api.dto.ReviewResponse;
import com.group01.learning.api.dto.ReviewSubmissionResponse;
import com.group01.learning.api.dto.SubmitReviewRequest;
import com.group01.learning.application.port.ReviewStore;
import com.group01.learning.application.usecase.AssignTopicTestUseCase;
import com.group01.learning.application.usecase.ReviewUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/learning")
public class ReviewController {
    private final CurrentUserProvider currentUser;
    private final ReviewUseCase reviews;
    private final AssignTopicTestUseCase tests;

    public ReviewController(CurrentUserProvider currentUser, ReviewUseCase reviews, AssignTopicTestUseCase tests) {
        this.currentUser = currentUser;
        this.reviews = reviews;
        this.tests = tests;
    }

    @GetMapping("/reviews/{reviewId}")
    public ReviewResponse review(@PathVariable("reviewId") UUID reviewId) {
        return ReviewResponse.from(reviews.get(currentUser.requireUserId(), reviewId));
    }

    @PostMapping("/reviews/{reviewId}/submissions")
    public ReviewSubmissionResponse submit(@PathVariable("reviewId") UUID reviewId,
                                           @Valid @RequestBody SubmitReviewRequest request) {
        return ReviewSubmissionResponse.from(reviews.submit(currentUser.requireUserId(), reviewId, request.toCommand()));
    }

    @PostMapping("/topics/{id}/test-assignments")
    public TestAssignmentResponse assignTest(@PathVariable("id") UUID topicId) {
        ReviewStore.TestAssignment assignment = tests.assign(currentUser.requireUserId(), topicId);
        return new TestAssignmentResponse(assignment.assignmentId(), assignment.packageId(),
                assignment.packageVersionId());
    }

    public record TestAssignmentResponse(UUID assignmentId, UUID packageId, UUID packageVersionId) {}
}
