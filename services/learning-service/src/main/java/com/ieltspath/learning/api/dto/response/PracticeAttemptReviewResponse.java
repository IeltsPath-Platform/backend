package com.ieltspath.learning.api.dto.response;

import com.ieltspath.learning.application.result.PracticeAttemptView;
import com.ieltspath.learning.application.usecase.ReviewPracticeAttemptUseCase;

/** A submitted practice attempt: what it asked and what it scored. */
public record PracticeAttemptReviewResponse(PracticeAttemptView attempt, PracticeSubmissionResponse result) {
    public static PracticeAttemptReviewResponse from(ReviewPracticeAttemptUseCase.Result review) {
        return new PracticeAttemptReviewResponse(review.attempt(), PracticeSubmissionResponse.from(review.result()));
    }
}
