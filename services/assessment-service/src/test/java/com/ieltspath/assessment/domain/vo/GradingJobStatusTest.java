package com.ieltspath.assessment.domain.vo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GradingJobStatusTest {
    @Test void allowedMovesAreTheLifecycleSpecification() {
        for (var from : GradingJobStatus.values()) for (var to : GradingJobStatus.values()) {
            boolean expected = switch (from) {
                case QUEUED -> to == GradingJobStatus.PROCESSING || to == GradingJobStatus.CANCELLED;
                case PROCESSING -> to == GradingJobStatus.QUEUED || to == GradingJobStatus.COMPLETED
                        || to == GradingJobStatus.FAILED || to == GradingJobStatus.CANCELLED;
                case COMPLETED -> to == GradingJobStatus.QUEUED;
                case FAILED, CANCELLED -> false;
            };
            assertEquals(expected, from.canMoveTo(to), from + " -> " + to);
        }
    }
}
