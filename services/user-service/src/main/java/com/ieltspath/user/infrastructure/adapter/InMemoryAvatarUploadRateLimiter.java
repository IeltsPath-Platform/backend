package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.application.exception.AvatarUploadRateLimitException;
import com.ieltspath.user.application.port.AvatarUploadRateLimiter;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Counts uploads per user in memory, so the limit applies per service instance and resets on restart. */
@Component
public class InMemoryAvatarUploadRateLimiter implements AvatarUploadRateLimiter {
    private static final int MAX_PER_HOUR = 10;
    private static final Duration WINDOW = Duration.ofHours(1);
    private static final int SWEEP_EVERY = 256;
    private final ConcurrentHashMap<UUID, ArrayDeque<Instant>> uploads = new ConcurrentHashMap<>();
    private final AtomicInteger calls = new AtomicInteger();

    @Override
    public void checkAndAcquire(UUID userId) {
        Instant now = Instant.now();
        if (calls.incrementAndGet() % SWEEP_EVERY == 0) sweep(now);
        uploads.compute(userId, (id, times) -> {
            if (times == null) times = new ArrayDeque<>();
            Instant cutoff = now.minus(WINDOW);
            while (!times.isEmpty() && !times.peekFirst().isAfter(cutoff)) times.removeFirst();
            if (times.size() >= MAX_PER_HOUR) throw new AvatarUploadRateLimitException();
            times.addLast(now);
            return times;
        });
    }

    /** Forgets users whose newest upload is outside the window, so the map does not grow with the number of users. */
    int sweep(Instant now) {
        Instant cutoff = now.minus(WINDOW);
        int before = uploads.size();
        for (UUID userId : uploads.keySet()) {
            uploads.computeIfPresent(userId, (id, times) ->
                    times.isEmpty() || !times.peekLast().isAfter(cutoff) ? null : times);
        }
        return before - uploads.size();
    }

    int trackedUsers() {
        return uploads.size();
    }
}
