package com.ieltspath.events.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordResetRequestedV1JsonTest {
    @Test
    void serializesVersionedPasswordResetContract() throws Exception {
        UUID eventId = UUID.randomUUID();
        PasswordResetRequestedV1 event = new PasswordResetRequestedV1(eventId, PasswordResetRequestedV1.EVENT_TYPE,
                Instant.parse("2026-11-10T00:00:00Z"), UUID.randomUUID(), "learner@example.test", "Learner",
                "raw-reset-token", Instant.parse("2026-11-10T00:15:00Z"));
        SimpleModule timeModule = new SimpleModule().addSerializer(Instant.class, ToStringSerializer.instance);
        ObjectMapper mapper = new ObjectMapper().registerModule(timeModule);
        var json = mapper.readTree(mapper.writeValueAsBytes(event));
        assertEquals(eventId.toString(), json.get("eventId").asText());
        assertEquals("user.password-reset-requested.v1", json.get("eventType").asText());
        assertTrue(json.has("resetToken"));
        assertTrue(json.has("expiresAt"));
    }
}
