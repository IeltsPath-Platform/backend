package com.ieltspath.events.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UserDeletedV1JsonTest {
    @Test
    void serializesVersionedUserDeletedContractWithoutPii() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserDeletedV1 event = new UserDeletedV1(eventId, UserDeletedV1.EVENT_TYPE,
                Instant.parse("2026-11-26T05:00:00Z"), userId);
        SimpleModule timeModule = new SimpleModule().addSerializer(Instant.class, ToStringSerializer.instance);
        ObjectMapper mapper = new ObjectMapper().registerModule(timeModule);
        var json = mapper.readTree(mapper.writeValueAsBytes(event));
        assertEquals(eventId.toString(), json.get("eventId").asText());
        assertEquals("user.deleted.v1", json.get("eventType").asText());
        assertEquals("2026-11-26T05:00:00Z", json.get("occurredAt").asText());
        assertEquals(userId.toString(), json.get("userId").asText());
        assertFalse(json.has("email"));
        assertFalse(json.has("displayName"));
    }
}
