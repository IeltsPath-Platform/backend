package com.ieltspath.events;

import com.ieltspath.events.assessment.AssessmentMessaging;
import com.ieltspath.events.community.CommunityEventTypes;
import com.ieltspath.events.community.CommunityMessaging;
import com.ieltspath.events.messaging.MessageHeaders;
import com.ieltspath.events.points.PointsGrantMessaging;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EventNamesContractTest {
    @Test
    void preservesTheNamesAndRetryPolicyUsedByProducersAndConsumers() {
        assertEquals("learning.points", PointsGrantMessaging.EXCHANGE);
        assertEquals("points.grant.requested", PointsGrantMessaging.REQUESTED_ROUTING_KEY);
        assertEquals("points.grant.settled", PointsGrantMessaging.SETTLED_ROUTING_KEY);
        assertEquals("assessment.events", AssessmentMessaging.EXCHANGE);
        assertEquals("assessment.completed.v2", AssessmentMessaging.COMPLETED_ROUTING_KEY);
        assertEquals("community.events", CommunityMessaging.EXCHANGE);
        assertEquals("x-retry-count", MessageHeaders.RETRY_COUNT);
        assertEquals(30_000L, PointsGrantMessaging.RETRY_DELAY_MS);
        assertEquals(5, PointsGrantMessaging.MAX_RETRIES);
        assertEquals(List.of("community.post-commented.v1", "community.comment-replied.v1",
                "community.comment-mentioned.v1", "community.post-reacted.v1", "community.user-followed.v1"),
                CommunityEventTypes.ORDERED);
        assertEquals(Set.copyOf(CommunityEventTypes.ORDERED), CommunityEventTypes.ALL);
    }
}
