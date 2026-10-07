package com.ieltspath.learning.domain.aggregate;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CourseProgressTest {
    private static final Instant NOW = Instant.parse("2026-10-07T00:00:00Z");

    @Test
    void coursePassIsOneWay() {
        CourseProgress progress = CourseProgress.restore(UUID.randomUUID(), UUID.randomUUID(), null);

        assertThat(progress.pass(NOW)).isTrue();
        assertThat(progress.pass(NOW.plusSeconds(10))).isFalse();
        assertThat(progress.passedAt()).isEqualTo(NOW);
    }
}
