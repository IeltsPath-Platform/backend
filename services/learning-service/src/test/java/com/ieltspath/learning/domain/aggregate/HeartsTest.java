package com.ieltspath.learning.domain.aggregate;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HeartsTest {
    private static final Instant T0 = Instant.parse("2026-10-17T10:00:00Z");
    private static final Duration FIVE_HOURS = Duration.ofHours(5);

    @Test
    void aNewLearnerIsFullAndHasNoNextHeart() {
        Hearts hearts = Hearts.start(UUID.randomUUID(), 5, T0);

        assertThat(hearts.hearts()).isEqualTo(5);
        assertThat(hearts.nextHeartAt()).isEmpty();
    }

    @Test
    void losingAHeartStartsTheClockAndTheNextHeartComesFiveHoursLater() {
        Hearts hearts = Hearts.start(UUID.randomUUID(), 5, T0);

        hearts.lose(1, T0.plus(Duration.ofMinutes(30)));

        assertThat(hearts.hearts()).isEqualTo(4);
        assertThat(hearts.nextHeartAt()).contains(T0.plus(Duration.ofMinutes(30)).plus(FIVE_HOURS));
    }

    @Test
    void oneHeartComesBackPerIntervalAndTheClockKeepsThePartialProgress() {
        Hearts hearts = Hearts.restore(UUID.randomUUID(), 2, 5, T0);

        hearts.regen(T0.plus(FIVE_HOURS).plus(Duration.ofHours(1)));

        assertThat(hearts.hearts()).isEqualTo(3);
        assertThat(hearts.nextHeartAt()).contains(T0.plus(FIVE_HOURS).plus(FIVE_HOURS));
    }

    @Test
    void regenerationStopsAtTheMaximum() {
        Hearts hearts = Hearts.restore(UUID.randomUUID(), 4, 5, T0);

        hearts.regen(T0.plus(Duration.ofDays(3)));

        assertThat(hearts.hearts()).isEqualTo(5);
        assertThat(hearts.full()).isTrue();
    }

    @Test
    void whileFullTheClockDoesNotRunSoARefillIsNeverWastedOnTimePassed() {
        Hearts hearts = Hearts.start(UUID.randomUUID(), 5, T0);

        hearts.regen(T0.plus(Duration.ofDays(2)));
        hearts.lose(1, T0.plus(Duration.ofDays(2)));

        assertThat(hearts.nextHeartAt()).contains(T0.plus(Duration.ofDays(2)).plus(FIVE_HOURS));
    }

    @Test
    void heartsNeverGoBelowZero() {
        Hearts hearts = Hearts.restore(UUID.randomUUID(), 2, 5, T0);

        hearts.lose(9, T0);

        assertThat(hearts.hearts()).isZero();
    }

    @Test
    void aLowerMaximumCutsTheHeartsToIt() {
        Hearts hearts = Hearts.start(UUID.randomUUID(), 7, T0);

        hearts.setMax(5, T0.plus(Duration.ofHours(1)));

        assertThat(hearts.hearts()).isEqualTo(5);
        assertThat(hearts.max()).isEqualTo(5);
        assertThat(hearts.full()).isTrue();
    }

    @Test
    void aRaisedMaximumLeavesTheCurrentHeartsAndStartsTheClock() {
        Hearts hearts = Hearts.restore(UUID.randomUUID(), 5, 5, T0);

        hearts.setMax(7, T0.plus(Duration.ofHours(1)));

        assertThat(hearts.hearts()).isEqualTo(5);
        assertThat(hearts.full()).isFalse();
    }

    @Test
    void aRefillFillsTheHeartsAndStopsTheClock() {
        Hearts hearts = Hearts.restore(UUID.randomUUID(), 0, 5, T0);

        hearts.refillFull(T0.plus(Duration.ofHours(1)));

        assertThat(hearts.full()).isTrue();
        assertThat(hearts.nextHeartAt()).isEmpty();
    }

    @Test
    void invalidValuesAreRejected() {
        assertThatThrownBy(() -> Hearts.restore(UUID.randomUUID(), 6, 5, T0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Hearts.start(UUID.randomUUID(), 0, T0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
