package com.ieltspath.events.access;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubscriptionGrantedV1JsonTest {
    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    void matchesTheGoldenJsonInTheContractDocument() throws Exception {
        var event = new SubscriptionGrantedV1(id(1), SubscriptionGrantedV1.EVENT_TYPE, Instant.parse("2026-11-01T02:00:00Z"),
                id(2), id(3), "PREMIUM_MONTHLY", Instant.parse("2026-12-01T02:00:00Z"));

        byte[] body = JSON.writeValueAsBytes(event);

        assertEquals(JSON.readTree("""
                {"eventId":"40000000-0000-4000-8000-000000000001","eventType":"access.subscription-granted.v1",
                 "occurredAt":"2026-11-01T02:00:00Z","userId":"40000000-0000-4000-8000-000000000002",
                 "subscriptionId":"40000000-0000-4000-8000-000000000003","planCode":"PREMIUM_MONTHLY",
                 "endsAt":"2026-12-01T02:00:00Z"}
                """), JSON.readTree(body));
        assertEquals(event, JSON.readValue(body, SubscriptionGrantedV1.class));
    }

    @Test
    void aFieldAddedByAnNewerProducerIsIgnored() throws Exception {
        String withExtra = """
                {"eventId":"40000000-0000-4000-8000-000000000001","eventType":"access.subscription-granted.v1",
                 "occurredAt":"2026-11-01T02:00:00Z","userId":"40000000-0000-4000-8000-000000000002",
                 "subscriptionId":"40000000-0000-4000-8000-000000000003","planCode":"PREMIUM_MONTHLY",
                 "endsAt":"2026-12-01T02:00:00Z","futureField":"ignored"}
                """;

        SubscriptionGrantedV1 event = JSON.readValue(withExtra, SubscriptionGrantedV1.class);

        assertEquals(id(2), event.userId());
    }

    private static UUID id(int suffix) {
        return UUID.fromString("40000000-0000-4000-8000-" + String.format("%012d", suffix));
    }
}
