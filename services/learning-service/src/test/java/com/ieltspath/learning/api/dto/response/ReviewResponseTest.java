package com.ieltspath.learning.api.dto.response;

import com.ieltspath.learning.application.result.ReviewResult;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The learner's review carries whether it blocks and what raised it, so the app can explain it. */
class ReviewResponseTest {
    private static ReviewResult review(boolean blocking, String triggerKind) {
        return new ReviewResult(UUID.randomUUID(), "PENDING", UUID.randomUUID(), List.of(), null, UUID.randomUUID(),
                LearningSkill.WRITING, "PRACTICE", null, "NONE", 0, null, List.of(), ReviewResult.ACTIVE, null, blocking,
                List.of(), null, triggerKind);
    }

    @Test
    void anOptionalReviewIsSentAsOptionalWithItsTrigger() {
        var response = ReviewResponse.from(review(false, "WRITING_CRITERION"));

        assertThat(response.blocking()).isFalse();
        assertThat(response.triggerKind()).isEqualTo("WRITING_CRITERION");
    }

    @Test
    void aRequiredReviewIsSentAsRequired() {
        var response = ReviewResponse.from(review(true, "WRITING_CRITERION"));

        assertThat(response.blocking()).isTrue();
    }

    @Test
    void aReviewWithNoRecordedTriggerSendsNoTrigger() {
        assertThat(ReviewResponse.from(review(true, null)).triggerKind()).isNull();
    }
}
