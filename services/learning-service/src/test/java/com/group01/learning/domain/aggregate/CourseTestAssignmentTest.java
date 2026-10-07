package com.group01.learning.domain.aggregate;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseTestAssignmentTest {
    @Test
    void consumesOnceAndAppliesTheSharedPassMark() {
        CourseTestAssignment passing = CourseTestAssignment.assign(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        UUID attemptId = UUID.randomUUID();

        assertThat(passing.consume(attemptId, new BigDecimal("70.0000"))).isTrue();
        assertThat(passing.isConsumed()).isTrue();
        assertThat(passing.consumedAttemptId()).isEqualTo(attemptId);
        assertThat(passing.percent()).isEqualByComparingTo("70");
        assertThatThrownBy(() -> passing.consume(UUID.randomUUID(), BigDecimal.TEN))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aFailingScoreConsumesTheAssignmentWithoutPassingTheCourse() {
        CourseTestAssignment assignment = CourseTestAssignment.assign(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        assertThat(assignment.consume(UUID.randomUUID(), new BigDecimal("69.9999"))).isFalse();
        assertThat(assignment.isConsumed()).isTrue();
    }
}
