package com.ieltspath.community.infrastructure.adapter;

import com.ieltspath.community.domain.exception.CommunityRateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryMediaRateLimiterTest {

    private InMemoryMediaRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new InMemoryMediaRateLimiter();
    }

    @Test
    void allowsUpTo30UploadsPerHour() {
        UUID userId = UUID.randomUUID();
        for (int i = 0; i < 30; i++) {
            assertDoesNotThrow(() -> rateLimiter.checkAndAcquire(userId));
        }
        assertThrows(CommunityRateLimitExceededException.class, () -> rateLimiter.checkAndAcquire(userId));
    }

    @Test
    void separatesLimitsForDifferentUsers() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        for (int i = 0; i < 30; i++) {
            rateLimiter.checkAndAcquire(user1);
        }
        assertThrows(CommunityRateLimitExceededException.class, () -> rateLimiter.checkAndAcquire(user1));
        assertDoesNotThrow(() -> rateLimiter.checkAndAcquire(user2));
    }

    @Test
    void handlesNullUserIdGracefully() {
        assertDoesNotThrow(() -> rateLimiter.checkAndAcquire(null));
    }
}

