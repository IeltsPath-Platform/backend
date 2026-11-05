package com.ieltspath.user.domain.aggregate;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Failed current-password attempts of one user. {@value #MAX_FAILURES} failures inside a 15-minute window lock further
 * attempts for 15 minutes. A failure after the window or the lock has ended starts a new window.
 */
@Getter
@AllArgsConstructor
public class PasswordChangeAttempts {
    public static final int MAX_FAILURES = 5;
    public static final Duration WINDOW = Duration.ofMinutes(15);

    private final UUID userId;
    private int failures;
    private LocalDateTime windowStartedAt;
    private LocalDateTime lockedUntil;

    public static PasswordChangeAttempts fresh(UUID userId, LocalDateTime now) {
        return new PasswordChangeAttempts(userId, 0, now, null);
    }

    public boolean isLocked(LocalDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void recordFailure(LocalDateTime now) {
        if (isLocked(now)) return;
        if (lockedUntil != null || !windowStartedAt.plus(WINDOW).isAfter(now)) {
            failures = 0;
            windowStartedAt = now;
            lockedUntil = null;
        }
        failures++;
        if (failures >= MAX_FAILURES) lockedUntil = now.plus(WINDOW);
    }
}
