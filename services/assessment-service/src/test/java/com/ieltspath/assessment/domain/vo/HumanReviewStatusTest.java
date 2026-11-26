package com.ieltspath.assessment.domain.vo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HumanReviewStatusTest {
    @Test void allowedMovesAreTheLifecycleSpecification() {
        for (var from : HumanReviewStatus.values()) for (var to : HumanReviewStatus.values()) {
            boolean expected = switch (from) {
                case QUEUED -> to == HumanReviewStatus.ASSIGNED;
                case ASSIGNED -> to == HumanReviewStatus.QUEUED || to == HumanReviewStatus.IN_REVIEW;
                case IN_REVIEW -> to == HumanReviewStatus.QUEUED || to == HumanReviewStatus.COMPLETED;
                case COMPLETED -> to == HumanReviewStatus.QUEUED;
            };
            assertEquals(expected, from.canMoveTo(to), from + " -> " + to);
        }
    }
}
