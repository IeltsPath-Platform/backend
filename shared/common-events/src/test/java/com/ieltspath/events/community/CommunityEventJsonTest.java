package com.ieltspath.events.community;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CommunityEventJsonTest {
    @Test
    void preservesCamelCaseFieldsTimestampAndNullReferences() throws Exception {
        ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var event = new CommunityEvent(id(1), CommunityEventTypes.USER_FOLLOWED,
                Instant.parse("2026-10-01T09:10:00Z"), id(2), id(3), null, null);
        byte[] body = json.writeValueAsBytes(event);
        assertEquals(json.readTree("""
                {"eventId":"20000000-0000-4000-8000-000000000001","eventType":"community.user-followed.v1",
                 "occurredAt":"2026-10-01T09:10:00Z","actorId":"20000000-0000-4000-8000-000000000002",
                 "recipientId":"20000000-0000-4000-8000-000000000003","postId":null,"commentId":null}
                """), json.readTree(body));
        assertEquals(event, json.readValue(body, CommunityEvent.class));
    }

    private static UUID id(int suffix) {
        return UUID.fromString("20000000-0000-4000-8000-" + String.format("%012d", suffix));
    }
}
