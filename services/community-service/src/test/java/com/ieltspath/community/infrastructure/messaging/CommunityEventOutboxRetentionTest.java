package com.ieltspath.community.infrastructure.messaging;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CommunityEventOutboxRetentionTest {
    @Test
    void socialDeduplicationWindowRemainsShorterThanMinimumPurgeRetention() {
        assertTrue(CommunityEventOutboxAdapter.SOCIAL_DEDUPE_WINDOW.compareTo(Duration.ofDays(2)) < 0);
    }
}
