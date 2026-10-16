package com.ieltspath.learning.domain.vo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class QuestionAttemptEventTest {

    @Test
    void durationIsKeptInsideZeroToTwoHours() {
        assertEquals(QuestionAttemptEvent.MAX_DURATION_MS, durationOf(99_999_999L));
        assertEquals(0L, durationOf(-5L));
        assertEquals(4200L, durationOf(4200L));
        assertNull(durationOf(null));
    }

    @Test
    void knowledgePointsAreKeptOnceInTheirOrder() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        QuestionAttemptEvent event = QuestionAttemptEvent.of(UUID.randomUUID(), UUID.randomUUID(),
                List.of(first, second, first), true, null, null, QuestionEventSource.PRACTICE_SET, UUID.randomUUID(), true);

        assertEquals(List.of(first, second), event.knowledgePointIds());
    }

    @Test
    void onlyAssessmentEventsCarryAnAttempt() {
        assertThrows(IllegalArgumentException.class, () -> new QuestionAttemptEvent(UUID.randomUUID(), UUID.randomUUID(),
                List.of(), true, BigDecimal.ONE, BigDecimal.ONE, null, null, QuestionEventSource.ASSESSMENT,
                UUID.randomUUID(), null, true));
        assertThrows(IllegalArgumentException.class, () -> new QuestionAttemptEvent(UUID.randomUUID(), UUID.randomUUID(),
                List.of(), true, BigDecimal.ONE, BigDecimal.ONE, null, null, QuestionEventSource.LESSON_EXERCISE,
                UUID.randomUUID(), UUID.randomUUID(), true));
    }

    @Test
    void sourceValuesMatchTheDatabaseCheck() {
        assertEquals("lesson_exercise", QuestionEventSource.LESSON_EXERCISE.value());
        assertEquals("diagnostic_probe", QuestionEventSource.DIAGNOSTIC_PROBE.value());
    }

    private static Long durationOf(Long duration) {
        return QuestionAttemptEvent.of(UUID.randomUUID(), UUID.randomUUID(), List.of(), true, duration, null,
                QuestionEventSource.LESSON_EXERCISE, UUID.randomUUID(), true).durationMs();
    }
}
