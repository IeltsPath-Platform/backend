package com.ieltspath.community.infrastructure;

import com.ieltspath.events.community.CommunityEventTypes;
import com.ieltspath.community.infrastructure.messaging.CommunityOutboxRelay;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommunityEventRoutingContractTest {
    @Test
    void everyOutboxEventTypeHasExactlyOneRelayRoutingKey() {
        assertEquals(CommunityEventTypes.ALL, CommunityOutboxRelay.routingKeys().keySet());
        CommunityOutboxRelay.routingKeys().forEach((eventType, routingKey) -> assertEquals(eventType, routingKey));
    }
}
