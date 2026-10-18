package com.ieltspath.community.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.community.application.port.CommunityEventOutbox;
import com.ieltspath.events.community.CommunityEventTypes;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CommunityEventOutboxAdapter implements CommunityEventOutbox {
    private static final java.time.Duration SOCIAL_DEDUPE_WINDOW = java.time.Duration.ofHours(24);
    private static final java.util.Set<String> SOCIAL_SIGNALS =
            java.util.Set.of(CommunityEventTypes.POST_REACTED, CommunityEventTypes.USER_FOLLOWED);
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    @Override
    public void record(String eventType, UUID actorId, UUID recipientId, UUID postId, UUID commentId) {
        if (!CommunityEventTypes.ALL.contains(eventType))
            throw new IllegalArgumentException("Unsupported community event type");
        if (actorId.equals(recipientId)) return;
        Instant occurredAt = Instant.now();
        if (SOCIAL_SIGNALS.contains(eventType) && alreadyNotifiedRecently(eventType, actorId, recipientId, postId, occurredAt))
            return;
        UUID eventId = UUID.randomUUID();
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

    /** Reactions and follows can be toggled freely, so the same actor/target pair notifies at most once per window. */
    private boolean alreadyNotifiedRecently(String eventType, UUID actorId, UUID recipientId, UUID postId, Instant now) {
        Integer found = jdbc.query("""
                SELECT 1 FROM outbox_events
                WHERE event_type = ? AND payload->>'actorId' = ? AND payload->>'recipientId' = ?
                  AND coalesce(payload->>'postId', '') = ? AND created_at > ?
                LIMIT 1
                """, rs -> rs.next() ? 1 : null, eventType, actorId.toString(), recipientId.toString(),
                postId == null ? "" : postId.toString(), java.sql.Timestamp.from(now.minus(SOCIAL_DEDUPE_WINDOW)));
        return found != null;
    }

    public record CommunityEvent(UUID eventId, String eventType, Instant occurredAt, UUID actorId,
                                UUID recipientId, UUID postId, UUID commentId) { }
}
