package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.application.exception.AvatarUploadRateLimitException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryAvatarUploadRateLimiterTest {
    @Test
    void allowsTenUploadsAnHourAndRejectsTheEleventh() {
        var limiter = new InMemoryAvatarUploadRateLimiter();
        UUID user = UUID.randomUUID();
        for (int i = 0; i < 10; i++) limiter.checkAndAcquire(user);

        assertThatThrownBy(() -> limiter.checkAndAcquire(user)).isInstanceOf(AvatarUploadRateLimitException.class);
        limiter.checkAndAcquire(UUID.randomUUID());
    }

    @Test
    void sweepForgetsUsersWhoseLastUploadIsOutsideTheWindowAndKeepsTheOthers() {
        var limiter = new InMemoryAvatarUploadRateLimiter();
        for (int i = 0; i < 50; i++) limiter.checkAndAcquire(UUID.randomUUID());
        UUID recent = UUID.randomUUID();
        limiter.checkAndAcquire(recent);
        assertThat(limiter.trackedUsers()).isEqualTo(51);

        // Two hours later every earlier upload is outside the window; a fresh one keeps its user.
        Instant later = Instant.now().plus(Duration.ofHours(2));
        assertThat(limiter.sweep(later)).isEqualTo(51);
        assertThat(limiter.trackedUsers()).isZero();

        limiter.checkAndAcquire(recent);
        assertThat(limiter.sweep(Instant.now().plusSeconds(1))).isZero();
        assertThat(limiter.trackedUsers()).isEqualTo(1);
    }
}
