package com.ieltspath.assessment.domain.vo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SubmissionStatusTest {
    @Test void allowedMovesAreTheLifecycleSpecification() {
        for (var from : SubmissionStatus.values()) for (var to : SubmissionStatus.values()) {
            assertEquals(from == SubmissionStatus.DRAFT && to == SubmissionStatus.SUBMITTED,
                    from.canMoveTo(to), from + " -> " + to);
        }
    }
}
