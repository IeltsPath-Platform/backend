package com.ieltspath.access.infrastructure.messaging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Learning declares the same exchange and the same retry settings. A queue declared with other arguments than the broker
 * already holds fails with PRECONDITION_FAILED, so these values are pinned here and in Learning's matching test; change
 * both together.
 */
class PointsGrantTopologyContractTest {
    @Test
    void theExchangeRoutingKeysAndRetrySettingsMatchWhatLearningDeclares() {
        assertEquals("learning.points", LearningPointsMessagingConfig.EXCHANGE);
        assertEquals("points.grant.requested", LearningPointsMessagingConfig.GRANT_ROUTING_KEY);
        assertEquals("points.grant.settled", LearningPointsMessagingConfig.SETTLED_ROUTING_KEY);
        assertEquals(30_000L, LearningPointsMessagingConfig.RETRY_DELAY_MS);
        assertEquals(5, LearningPointsMessagingConfig.MAX_RETRIES);
        assertEquals("x-retry-count", MessageRetry.ATTEMPTS_HEADER);
    }
}
