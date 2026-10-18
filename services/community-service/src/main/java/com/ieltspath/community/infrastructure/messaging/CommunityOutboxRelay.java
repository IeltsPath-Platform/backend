package com.ieltspath.community.infrastructure.messaging;

import com.ieltspath.events.community.CommunityMessaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import com.ieltspath.events.community.CommunityEventTypes;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class CommunityOutboxRelay {
    private static final Logger log = LoggerFactory.getLogger(CommunityOutboxRelay.class);
    private static final Map<String, String> ROUTING_KEYS = Map.ofEntries(
            Map.entry(CommunityEventTypes.POST_COMMENTED, CommunityEventTypes.POST_COMMENTED),
            Map.entry(CommunityEventTypes.COMMENT_REPLIED, CommunityEventTypes.COMMENT_REPLIED),
            Map.entry(CommunityEventTypes.COMMENT_MENTIONED, CommunityEventTypes.COMMENT_MENTIONED),
            Map.entry(CommunityEventTypes.POST_REACTED, CommunityEventTypes.POST_REACTED),
            Map.entry(CommunityEventTypes.USER_FOLLOWED, CommunityEventTypes.USER_FOLLOWED));

    public static Map<String, String> routingKeys() { return ROUTING_KEYS; }

    private final JdbcTemplate jdbc;
    private final RabbitTemplate rabbit;
    private final ObjectMapper objectMapper;
    private final String exchange;
    private final int batchSize;
    private final Duration confirmTimeout;
    private final boolean enabled;
    private final int maxAttempts;
    private final Duration lease;

    public CommunityOutboxRelay(JdbcTemplate jdbc, RabbitTemplate rabbit, ObjectMapper objectMapper,
                                @Value("${community.messaging.exchange:" + CommunityMessaging.EXCHANGE + "}") String exchange,
                                @Value("${community.outbox.relay.batch-size:50}") int batchSize,
                                @Value("${community.outbox.relay.confirm-timeout:PT5S}") Duration confirmTimeout,
                                @Value("${community.outbox.relay.enabled:true}") boolean enabled,
                                @Value("${community.outbox.relay.max-attempts:12}") int maxAttempts,
                                @Value("${community.outbox.relay.lease:PT60S}") Duration lease) {
        this.jdbc = jdbc;
        this.rabbit = rabbit;
        this.objectMapper = objectMapper;
        this.exchange = exchange;
        this.batchSize = Math.max(1, Math.min(batchSize, 200));
        this.confirmTimeout = confirmTimeout;
        this.enabled = enabled;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.lease = lease;
    }

    /**
     * Claims a batch with a short lease in one statement, then publishes outside any transaction so a slow
     * broker never holds row locks or a database connection. Delivery is at-least-once: the consumer
     * de-duplicates by event id.
     */
    public int publishPendingBatch() {
        if (!enabled) return 0;
        List<OutboxRow> rows = new ArrayList<>(jdbc.query("""
                UPDATE outbox_events
                SET next_attempt_at = now() + make_interval(secs => ?)
                WHERE id IN (
                    SELECT id FROM outbox_events
                    WHERE published_at IS NULL AND failed_at IS NULL
                      AND (next_attempt_at IS NULL OR next_attempt_at <= now())
                    ORDER BY created_at, id
                    LIMIT ? FOR UPDATE SKIP LOCKED)
                RETURNING id, event_type, payload::text payload, created_at, retry_count
                """, (rs, row) -> new OutboxRow(rs.getObject("id", java.util.UUID.class),
                rs.getString("event_type"), rs.getString("payload"), rs.getTimestamp("created_at").toInstant(),
                rs.getInt("retry_count")), lease.toSeconds(), batchSize));
        rows.sort(Comparator.comparing(OutboxRow::createdAt).thenComparing(OutboxRow::id));
        int published = 0;
        for (OutboxRow row : rows) {
            try {
                publish(row);
                jdbc.update("UPDATE outbox_events SET published_at=?, last_error=NULL WHERE id=?",
                        java.sql.Timestamp.from(Instant.now()), row.id());
                published++;
            } catch (UnsupportedEventException exception) {
                jdbc.update("UPDATE outbox_events SET failed_at=now(), last_error=? WHERE id=?",
                        exception.getClass().getSimpleName(), row.id());
                log.error("Community event is not publishable eventId={} type={}", row.id(), row.eventType());
            } catch (RuntimeException exception) {
                jdbc.update("""
                        UPDATE outbox_events
                        SET retry_count=retry_count+1,
                            next_attempt_at=now() + make_interval(secs => LEAST(3600, power(2, LEAST(retry_count, 11))::integer)),
                            failed_at=CASE WHEN retry_count+1 >= ? THEN now() END,
                            last_error=?
                        WHERE id=?
                        """, maxAttempts, exception.getClass().getSimpleName(), row.id());
                log.warn("Community event publish failed eventId={} type={} errorType={}",
                        row.id(), row.eventType(), exception.getClass().getSimpleName());
                // The broker is the likely cause: stop here so one outage costs one confirm timeout, not one per row.
                // The remaining claimed rows become due again when their lease expires.
                break;
            }
        }
        return published;
    }

    private void publish(OutboxRow row) {
        String route = ROUTING_KEYS.get(row.eventType());
        if (route == null) throw new UnsupportedEventException();
        try {
            JsonNode payload = objectMapper.readTree(row.payload());
            Message message = MessageBuilder.withBody(row.payload().getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding(StandardCharsets.UTF_8.name())
                    .setMessageId(row.id().toString())
                    .setType(row.eventType())
                    .setTimestamp(Date.from(row.createdAt()))
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .setHeader("event_type", row.eventType())
                    .build();
            CorrelationData correlation = new CorrelationData(row.id().toString());
            rabbit.send(exchange, route, message, correlation);
            CorrelationData.Confirm confirm = correlation.getFuture().get(confirmTimeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!confirm.isAck() || correlation.getReturned() != null) {
                throw new IllegalStateException("Broker did not confirm routed event");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing event", exception);
        } catch (Exception exception) {
            if (exception instanceof RuntimeException runtimeException) throw runtimeException;
            throw new IllegalStateException("Could not publish event", exception);
        }
    }

    private static final class UnsupportedEventException extends RuntimeException {
        UnsupportedEventException() { super("Unsupported community event type"); }
    }

    private record OutboxRow(java.util.UUID id, String eventType, String payload, Instant createdAt, int retryCount) { }
}
