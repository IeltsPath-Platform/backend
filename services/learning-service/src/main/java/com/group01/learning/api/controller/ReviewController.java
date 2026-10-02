package com.group01.learning.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.learning.api.dto.response.ReviewResponse;
import com.group01.learning.api.dto.response.ReviewSubmissionResponse;
import com.group01.learning.api.dto.request.SubmitReviewRequest;
import com.group01.learning.api.dto.response.TestAssignmentResponse;
import com.group01.learning.application.usecase.AssignTopicTestUseCase;
import com.group01.learning.application.usecase.GetReviewUseCase;
import com.group01.learning.application.usecase.SubmitReviewUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class ReviewController {
    private final CurrentUserProvider currentUser;
    private final GetReviewUseCase getReviewUseCase;
    private final SubmitReviewUseCase submitReviewUseCase;
    private final AssignTopicTestUseCase assignTopicTestUseCase;

    @GetMapping("/reviews/{reviewId}")
    public ReviewResponse review(@PathVariable("reviewId") UUID reviewId) {
        return ReviewResponse.from(getReviewUseCase.execute(currentUser.requireUserId(), reviewId));
    }

    @PostMapping("/reviews/{reviewId}/submissions")
    public ReviewSubmissionResponse submit(@PathVariable("reviewId") UUID reviewId,
                                           @Valid @RequestBody SubmitReviewRequest request) {
        return ReviewSubmissionResponse.from(submitReviewUseCase.execute(
                currentUser.requireUserId(), reviewId, request.toCommand()));
    }

    @PostMapping("/topics/{id}/test-assignments")
    public TestAssignmentResponse assignTest(@PathVariable("id") UUID topicId) {
        return TestAssignmentResponse.from(assignTopicTestUseCase.execute(currentUser.requireUserId(), topicId));
    }
}
