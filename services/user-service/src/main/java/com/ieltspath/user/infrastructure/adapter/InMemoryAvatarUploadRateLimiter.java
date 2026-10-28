package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.application.port.AvatarUploadRateLimiter;
import com.ieltspath.user.application.exception.AvatarUploadRateLimitException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

@Component
public class InMemoryAvatarUploadRateLimiter implements AvatarUploadRateLimiter {
    private static final int MAX_PER_HOUR = 10;
    private static final Duration WINDOW = Duration.ofHours(1);
    private final ConcurrentHashMap<UUID, ArrayDeque<Instant>> uploads = new ConcurrentHashMap<>();

    @Override
    public void checkAndAcquire(UUID userId) {
        Instant now = Instant.now();
        uploads.compute(userId, (id, times) -> {
            if (times == null) times = new ArrayDeque<>();
            Instant cutoff = now.minus(WINDOW);
            while (!times.isEmpty() && !times.peekFirst().isAfter(cutoff)) times.removeFirst();
            if (times.size() >= MAX_PER_HOUR) throw new AvatarUploadRateLimitException();
            times.addLast(now);
            return times;
        });
    }
}
