package com.ieltspath.community.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.community.application.port.CommunityEventOutbox;
import com.ieltspath.community.application.port.CommunityEventTypes;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CommunityEventOutboxAdapter implements CommunityEventOutbox {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    @Override
    public void record(String eventType, UUID actorId, UUID recipientId, UUID postId, UUID commentId) {
        if (!CommunityEventTypes.ALL.contains(eventType))
            throw new IllegalArgumentException("Unsupported community event type");
        if (actorId.equals(recipientId)) return;
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        String payload;
        try {
            payload = objectMapper.writeValueAsString(new CommunityEvent(
                    eventId, eventType, occurredAt, actorId, recipientId, postId, commentId));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize community event", exception);
        }
        jdbc.update("""
                INSERT INTO outbox_events(id, aggregate_type, aggregate_id, event_type, payload, created_at)
                VALUES (?, 'COMMUNITY', ?, ?, CAST(? AS jsonb), ?)
                """, eventId, postId == null ? recipientId.toString() : postId.toString(), eventType, payload,
                java.sql.Timestamp.from(occurredAt));
    }

    public record CommunityEvent(UUID eventId, String eventType, Instant occurredAt, UUID actorId,
                                UUID recipientId, UUID postId, UUID commentId) { }
}
