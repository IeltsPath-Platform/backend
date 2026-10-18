package com.ieltspath.learning.infrastructure.messaging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Access declares the same exchange and the same retry settings. A queue declared with other arguments than the broker
 * already holds fails with PRECONDITION_FAILED, so these values are pinned here and in Access's matching test; change
 * both together.
 */
class PointsGrantTopologyContractTest {
    @Test
    void theExchangeRoutingKeysAndRetrySettingsMatchWhatAccessDeclares() {
        assertEquals("learning.points", RewardGrantMessagingConfig.EXCHANGE);
        assertEquals("points.grant.requested", RewardGrantMessagingConfig.REQUESTED_ROUTING_KEY);
        assertEquals("points.grant.settled", RewardGrantMessagingConfig.SETTLED_ROUTING_KEY);
        assertEquals(30_000L, RewardGrantMessagingConfig.RETRY_DELAY_MS);
        assertEquals(5, RewardGrantMessagingConfig.MAX_RETRIES);
        assertEquals("x-retry-count", MessageRetry.ATTEMPTS_HEADER);
    }
}
