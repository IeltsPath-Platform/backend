package com.group01.learning.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.learning.api.dto.response.ReviewResponse;
import com.group01.learning.api.dto.response.ReviewSubmissionResponse;
import com.group01.learning.api.dto.request.SubmitReviewRequest;
import com.group01.learning.api.dto.response.TestAssignmentResponse;
import com.group01.learning.application.usecase.AssignTopicTestUseCase;
import com.group01.learning.application.usecase.GetReviewUseCase;
import com.group01.learning.application.usecase.SubmitReviewUseCase;
import com.group01.learning.application.usecase.ListReviewsUseCase;
import com.group01.learning.application.usecase.SubmitTheoryCheckUseCase;
import com.group01.learning.api.dto.request.SubmitExerciseRequest;
import com.group01.learning.api.dto.response.TheoryCheckResponse;
import com.group01.learning.api.dto.response.ReviewListResponse;
import com.group01.learning.domain.vo.LearningSkill;
import com.group01.learning.domain.vo.ReviewStatus;
import com.group01.learning.application.exception.LearningRequestException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class ReviewController {
    private final CurrentUserProvider currentUser;
    private final GetReviewUseCase getReviewUseCase;
    private final SubmitReviewUseCase submitReviewUseCase;
    private final AssignTopicTestUseCase assignTopicTestUseCase;
    private final ListReviewsUseCase listReviewsUseCase;
    private final SubmitTheoryCheckUseCase submitTheoryCheckUseCase;

    @GetMapping("/reviews")
    public List<ReviewListResponse> reviews(@RequestParam(name = "status", defaultValue = "PENDING") ReviewStatus status,
                                            @RequestParam(name = "skill", required = false) LearningSkill skill,
                                            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        if (limit < 1 || limit > 100) {
            throw new LearningRequestException(400, "INVALID_LIMIT", "limit must be between 1 and 100");
        }
        return listReviewsUseCase.execute(currentUser.requireUserId(), status, skill, limit).stream()
                .map(ReviewListResponse::from).toList();
    }

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

    @PostMapping("/reviews/{reviewId}/theory-check")
    public TheoryCheckResponse theoryCheck(@PathVariable("reviewId") UUID reviewId,
                                           @Valid @RequestBody SubmitExerciseRequest request) {
        return TheoryCheckResponse.from(submitTheoryCheckUseCase.execute(
                currentUser.requireUserId(), reviewId, request.toCommand()));
    }

    @PostMapping("/topics/{id}/test-assignments")
    public TestAssignmentResponse assignTest(@PathVariable("id") UUID topicId) {
        return TestAssignmentResponse.from(assignTopicTestUseCase.execute(currentUser.requireUserId(), topicId));
    }
}
