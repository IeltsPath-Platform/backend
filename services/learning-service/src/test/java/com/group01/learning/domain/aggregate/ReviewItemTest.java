package com.group01.learning.domain.aggregate;

import com.group01.learning.domain.vo.ReviewStage;
import com.group01.learning.domain.vo.ReviewStatus;
import com.group01.learning.domain.vo.TheoryReason;
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
        assertThat(low.recordSetResult(assign(low), UUID.randomUUID(), 2, 4)).isEqualTo(ReviewStatus.SKIPPED);
    }

    @Test
    void stagesGuardSetsAndTheory() {
        ReviewItem practice = pending(0);
        assertThatThrownBy(practice::completeTheory).isInstanceOf(IllegalStateException.class);

        ReviewItem theory = pending(0);
        theory.startWithTheory(TheoryReason.WRONG_IN_LESSON);
        assertThat(theory.isFresh()).isFalse();
        assertThatThrownBy(() -> assign(theory)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> theory.startWithTheory(TheoryReason.WRONG_IN_LESSON))
                .isInstanceOf(IllegalStateException.class);
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
