package com.group01.learning.domain.aggregate;

import com.group01.learning.domain.service.ReviewRule;
import com.group01.learning.domain.vo.ReviewStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewItemTest {
    private static ReviewItem pending(int failedSets) {
        return ReviewItem.restore(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ReviewStatus.PENDING, null, failedSets);
    }

    @Test
    void passedSetFinishesTheReview() {
        ReviewItem review = pending(0);
        UUID setId = review.assignSet(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()).id();

        assertThat(review.recordSetResult(setId, UUID.randomUUID(), true)).isEqualTo(ReviewStatus.DONE);
        assertThat(review.openSet()).isEmpty();
        assertThat(review.answeredSet()).get().satisfies(set -> assertThat(set.passed()).isTrue());
    }

    @Test
    void failedSetKeepsTheReviewPendingUntilTheLastAllowedFailure() {
        ReviewItem review = pending(ReviewRule.MAX_FAILED_REVIEW_SETS - 2);
        UUID first = review.assignSet(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()).id();
        assertThat(review.recordSetResult(first, UUID.randomUUID(), false)).isEqualTo(ReviewStatus.PENDING);

        ReviewItem reloaded = pending(ReviewRule.MAX_FAILED_REVIEW_SETS - 1);
        UUID last = reloaded.assignSet(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()).id();
        assertThat(reloaded.recordSetResult(last, UUID.randomUUID(), false)).isEqualTo(ReviewStatus.SKIPPED);
    }

    @Test
    void onlyTheOpenSetOfAPendingReviewAcceptsAnswers() {
        ReviewItem review = pending(0);
        UUID setId = review.assignSet(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()).id();

        assertThat(review.acceptsAnswersFor(UUID.randomUUID())).isFalse();
        assertThatThrownBy(() -> review.recordSetResult(UUID.randomUUID(), UUID.randomUUID(), true))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> review.assignSet(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class);

        review.recordSetResult(setId, UUID.randomUUID(), true);
        assertThat(review.acceptsAnswersFor(setId)).isFalse();
    }

    @Test
    void finishedReviewsAreFinal() {
        ReviewItem review = pending(0);
        review.skip();
        assertThat(review.status()).isEqualTo(ReviewStatus.SKIPPED);
        assertThatThrownBy(review::skip).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> review.assignSet(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class);
    }
}
