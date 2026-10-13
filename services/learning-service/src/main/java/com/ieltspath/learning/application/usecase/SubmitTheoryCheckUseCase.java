package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.ReviewTheoryCheckLog;
import com.ieltspath.learning.application.result.TheoryCheckResult;
import com.ieltspath.learning.domain.aggregate.ReviewItem;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.vo.ReviewStage;
import com.ieltspath.learning.domain.vo.ReviewStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Confirms that the learner read the theory of a review and returns the review to practice. The theory is the lesson
 * summary and there are no quick-check questions, so the answers must be empty. Replaying a {@code requestId} returns
 * the stored result.
 */
@Service
public class SubmitTheoryCheckUseCase {
    private final LearnerLock lock;
    private final ReviewItemRepository reviews;
    private final ReviewTheoryCheckLog checks;

    public SubmitTheoryCheckUseCase(LearnerLock lock, ReviewItemRepository reviews, ReviewTheoryCheckLog checks) {
        this.lock = lock;
        this.reviews = reviews;
        this.checks = checks;
    }

    @Transactional
    public TheoryCheckResult execute(UUID userId, UUID reviewId, SubmitExerciseCommand command) {
        lock.lock(userId);
        var saved = checks.find(command.requestId());
        if (saved.isPresent()) {
            if (!saved.get().userId().equals(userId) || !saved.get().reviewId().equals(reviewId)) {
                throw new LearningRequestException(409, "REQUEST_CONFLICT", "requestId belongs to another submission");
            }
            return saved.get().response();
        }
        ReviewItem review = reviews.findOwned(userId, reviewId)
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Review was not found"));
        if (review.status() != ReviewStatus.PENDING || review.stage() != ReviewStage.THEORY) {
            throw new LearningRequestException(409, "THEORY_NOT_REQUIRED", "This review is not at the theory stage");
        }
        if (!command.answers().isEmpty()) {
            throw new LearningRequestException(422, "INVALID_ANSWERS", "This review has no quick-check questions");
        }
        review.completeTheory();
        reviews.save(review);
        TheoryCheckResult response = new TheoryCheckResult(reviewId, 0, 0, List.of(), review.stage().name());
        checks.save(userId, reviewId, List.of(), command, response);
        return response;
    }
}
