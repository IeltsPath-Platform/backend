package com.ieltspath.user.domain.aggregate;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordChangeAttemptsTest {
    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 28, 10, 0);

    @Test
    void locksOnTheFifthFailureInsideTheWindow() {
        var attempts = PasswordChangeAttempts.fresh(UUID.randomUUID(), START);
        for (int i = 0; i < 4; i++) attempts.recordFailure(START.plusMinutes(i));

        assertThat(attempts.isLocked(START.plusMinutes(4))).isFalse();
        attempts.recordFailure(START.plusMinutes(4));

        assertThat(attempts.getFailures()).isEqualTo(5);
        assertThat(attempts.isLocked(START.plusMinutes(5))).isTrue();
        assertThat(attempts.getLockedUntil()).isEqualTo(START.plusMinutes(4).plus(PasswordChangeAttempts.WINDOW));
    }

    @Test
    void theLockEndsAfterFifteenMinutes() {
        var attempts = PasswordChangeAttempts.fresh(UUID.randomUUID(), START);
        for (int i = 0; i < 5; i++) attempts.recordFailure(START);

        assertThat(attempts.isLocked(START.plusMinutes(14))).isTrue();
        assertThat(attempts.isLocked(START.plusMinutes(15))).isFalse();
    }

    @Test
    void aFailureAfterTheWindowStartsANewWindow() {
        var attempts = PasswordChangeAttempts.fresh(UUID.randomUUID(), START);
        for (int i = 0; i < 4; i++) attempts.recordFailure(START);

        attempts.recordFailure(START.plusMinutes(16));

        assertThat(attempts.getFailures()).isEqualTo(1);
        assertThat(attempts.getWindowStartedAt()).isEqualTo(START.plusMinutes(16));
        assertThat(attempts.isLocked(START.plusMinutes(16))).isFalse();
    }

    @Test
    void aFailureAfterAnExpiredLockStartsANewWindow() {
        var attempts = PasswordChangeAttempts.fresh(UUID.randomUUID(), START);
        for (int i = 0; i < 5; i++) attempts.recordFailure(START);

        attempts.recordFailure(START.plusMinutes(20));

        assertThat(attempts.getFailures()).isEqualTo(1);
        assertThat(attempts.getLockedUntil()).isNull();
    }

    @Test
    void failuresWhileLockedDoNotExtendTheLock() {
        var attempts = PasswordChangeAttempts.fresh(UUID.randomUUID(), START);
        for (int i = 0; i < 5; i++) attempts.recordFailure(START);
        var lockedUntil = attempts.getLockedUntil();

        attempts.recordFailure(START.plusMinutes(5));

        assertThat(attempts.getLockedUntil()).isEqualTo(lockedUntil);
        assertThat(attempts.getFailures()).isEqualTo(5);
    }
}
