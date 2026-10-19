package com.ieltspath.community.infrastructure.adapter;

import com.ieltspath.community.application.port.MediaRateLimiter;
import com.ieltspath.community.domain.exception.CommunityRateLimitExceededException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryMediaRateLimiter implements MediaRateLimiter {

    private static final int MAX_UPLOADS_PER_HOUR = 30;
    private static final Duration WINDOW = Duration.ofHours(1);

    private final ConcurrentHashMap<UUID, Deque<Instant>> userUploads = new ConcurrentHashMap<>();

    @Override
    public void checkAndAcquire(UUID userId) {
        if (userId == null) {
            return;
        }
        Instant now = Instant.now();
        Instant cutoff = now.minus(WINDOW);

        userUploads.compute(userId, (id, timestamps) -> {
            if (timestamps == null) {
                timestamps = new ArrayDeque<>();
            }
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_UPLOADS_PER_HOUR) {
                throw new CommunityRateLimitExceededException("Too many uploads. Please try again later");
            }
            timestamps.addLast(now);
            return timestamps;
        });
    }

    void reset() {
        userUploads.clear();
    }
}

