package com.ieltspath.learning.domain.aggregate;

import com.ieltspath.learning.domain.vo.BandLevel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class LearnerPlacementTest {
    private static final Instant COMPLETED = Instant.parse("2026-10-06T10:00:00Z");

    @Test
    void newerPlacementReplacesTheEstimateButOlderOrEqualDifferentAttemptsDoNot() {
        UUID first = UUID.randomUUID();
        LearnerPlacement placement = LearnerPlacement.create(UUID.randomUUID(), band("5.5"), first, COMPLETED);
        UUID newer = UUID.randomUUID();
        assertThat(placement.record(band("6.0"), newer, COMPLETED.plusSeconds(1))).isTrue();
        assertThat(placement.record(band("4.0"), first, COMPLETED)).isFalse();
        assertThat(placement.record(band("7.0"), UUID.randomUUID(), COMPLETED.plusSeconds(1))).isFalse();
        assertThat(placement.band()).isEqualTo(band("6.0"));
        assertThat(placement.attemptId()).isEqualTo(newer);
        assertThat(placement.completedAt()).isEqualTo(COMPLETED.plusSeconds(1));
    }

    @Test
    void regradeOfTheCurrentAttemptReplacesTheEstimateRegardlessOfCompletionTime() {
        UUID attempt = UUID.randomUUID();
        LearnerPlacement placement = LearnerPlacement.create(UUID.randomUUID(), band("5.5"), attempt, COMPLETED);
        assertThat(placement.record(band("6.5"), attempt, COMPLETED)).isTrue();
        assertThat(placement.band()).isEqualTo(band("6.5"));
        assertThat(placement.record(band("6.0"), attempt, COMPLETED.minusSeconds(1))).isTrue();
        assertThat(placement.band()).isEqualTo(band("6.0"));
    }

    @Test
    void invalidReplacementDoesNotPartiallyChangeThePlacement() {
        UUID attempt = UUID.randomUUID();
        LearnerPlacement placement = LearnerPlacement.create(UUID.randomUUID(), band("5.5"), attempt, COMPLETED);
        assertThatThrownBy(() -> placement.record(band("6.0"), null, COMPLETED.plusSeconds(1)))
                .isInstanceOf(NullPointerException.class);
        assertThat(placement.band()).isEqualTo(band("5.5"));
        assertThat(placement.attemptId()).isEqualTo(attempt);
    }

    private static BandLevel band(String value) { return new BandLevel(new BigDecimal(value)); }
}
