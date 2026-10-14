package com.ieltspath.learning.domain.aggregate;

import com.ieltspath.learning.domain.vo.ReviewStage;
import com.ieltspath.learning.domain.vo.ReviewStatus;
import com.ieltspath.learning.domain.vo.TheoryReason;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewItemTest {
    private static ReviewItem pending(int failedSets) {
        return ReviewItem.restore(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ReviewStatus.PENDING, null, failedSets);
    }

    private static UUID assign(ReviewItem review) {
        return review.assignSet(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()).id();
    }

    @Test
    void setResultsFollowTheLadder() {
        ReviewItem passed = pending(0);
        assertThat(passed.recordSetResult(assign(passed), UUID.randomUUID(), 3, 4)).isEqualTo(ReviewStatus.DONE);
        assertThat(passed.answeredSet()).get().satisfies(set -> {
            assertThat(set.passed()).isTrue();
            assertThat(set.correct()).isEqualTo(3);
        });

        ReviewItem failed = pending(0);
        assertThat(failed.recordSetResult(assign(failed), UUID.randomUUID(), 2, 4)).isEqualTo(ReviewStatus.PENDING);
        assertThat(failed.stage()).isEqualTo(ReviewStage.THEORY);
        assertThat(failed.theoryReason()).isEqualTo(TheoryReason.SECOND_FAIL);

        ReviewItem low = pending(0);
        low.recordSetResult(assign(low), UUID.randomUUID(), 1, 4);
        assertThat(low.theoryReason()).isEqualTo(TheoryReason.LOW_SCORE);

        low.completeTheory();
        assertThat(low.stage()).isEqualTo(ReviewStage.PRACTICE);
        assertThat(low.theoryCompletedCount()).isEqualTo(1);
        // Later failures skip the theory and go straight to a new set, until one passes.
        assertThat(low.recordSetResult(assign(low), UUID.randomUUID(), 2, 4)).isEqualTo(ReviewStatus.PENDING);
        assertThat(low.stage()).isEqualTo(ReviewStage.PRACTICE);
        assertThat(low.failedSets()).isEqualTo(2);
        assertThat(low.recordSetResult(assign(low), UUID.randomUUID(), 0, 4)).isEqualTo(ReviewStatus.PENDING);
        assertThat(low.stage()).isEqualTo(ReviewStage.PRACTICE);
        assertThat(low.recordSetResult(assign(low), UUID.randomUUID(), 4, 4)).isEqualTo(ReviewStatus.DONE);
    }

    @Test
    void aReviewThatStartsWithTheoryReadsItAgainOnlyAfterItsFirstFailedSet() {
        ReviewItem review = pending(0);
        review.recordSetResult(assign(review), UUID.randomUUID(), 1, 4);
        assertThat(review.stage()).isEqualTo(ReviewStage.THEORY);
        review.completeTheory();
        review.recordSetResult(assign(review), UUID.randomUUID(), 1, 4);
        assertThat(review.stage()).isEqualTo(ReviewStage.PRACTICE);
        assertThat(review.status()).isEqualTo(ReviewStatus.PENDING);
    }

    @Test
    void stagesGuardSetsAndTheory() {
        ReviewItem practice = pending(0);
        assertThatThrownBy(practice::completeTheory).isInstanceOf(IllegalStateException.class);

        // A failed set sends the review to the theory, where no further set can be given until it is read.
        ReviewItem theory = pending(0);
        theory.recordSetResult(assign(theory), UUID.randomUUID(), 2, 4);
        assertThat(theory.stage()).isEqualTo(ReviewStage.THEORY);
        assertThatThrownBy(() -> assign(theory)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void onlyTheOpenSetOfAPendingReviewAcceptsAnswers() {
        ReviewItem review = pending(0);
        UUID setId = assign(review);

        assertThat(review.acceptsAnswersFor(UUID.randomUUID())).isFalse();
        assertThatThrownBy(() -> review.recordSetResult(UUID.randomUUID(), UUID.randomUUID(), 3, 3))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> assign(review)).isInstanceOf(IllegalStateException.class);

        review.recordSetResult(setId, UUID.randomUUID(), 3, 3);
        assertThat(review.acceptsAnswersFor(setId)).isFalse();
    }

    @Test
    void finishedReviewsAreFinal() {
        ReviewItem review = pending(0);
        review.skip();
        assertThat(review.status()).isEqualTo(ReviewStatus.SKIPPED);
        assertThatThrownBy(review::skip).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> assign(review)).isInstanceOf(IllegalStateException.class);
    }
}
