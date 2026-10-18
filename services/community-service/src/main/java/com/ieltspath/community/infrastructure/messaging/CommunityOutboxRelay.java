package com.ieltspath.community.infrastructure.messaging;

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
import com.ieltspath.community.application.port.CommunityEventTypes;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
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

    public CommunityOutboxRelay(JdbcTemplate jdbc, RabbitTemplate rabbit, ObjectMapper objectMapper,
                                @Value("${community.messaging.exchange:community.events}") String exchange,
                                @Value("${community.outbox.relay.batch-size:50}") int batchSize,
                                @Value("${community.outbox.relay.confirm-timeout:PT5S}") Duration confirmTimeout,
                                @Value("${community.outbox.relay.enabled:true}") boolean enabled) {
        this.jdbc = jdbc;
        this.rabbit = rabbit;
        this.objectMapper = objectMapper;
        this.exchange = exchange;
        this.batchSize = Math.max(1, Math.min(batchSize, 200));
        this.confirmTimeout = confirmTimeout;
        this.enabled = enabled;
    }

    @Transactional
    public int publishPendingBatch() {
        if (!enabled) return 0;
        List<OutboxRow> rows = jdbc.query("""
                SELECT id,event_type,payload::text payload,created_at,retry_count
                FROM outbox_events
                WHERE published_at IS NULL AND (next_attempt_at IS NULL OR next_attempt_at <= now())
                ORDER BY created_at,id
                LIMIT ? FOR UPDATE SKIP LOCKED
                """, (rs, row) -> new OutboxRow(rs.getObject("id", java.util.UUID.class),
                rs.getString("event_type"), rs.getString("payload"), rs.getTimestamp("created_at").toInstant(),
                rs.getInt("retry_count")), batchSize);
        int published = 0;
        for (OutboxRow row : rows) {
            try {
                publish(row);
                jdbc.update("UPDATE outbox_events SET published_at=?, last_error=NULL WHERE id=?",
                        java.sql.Timestamp.from(Instant.now()), row.id());
                published++;
            } catch (RuntimeException exception) {
                jdbc.update("""
                        UPDATE outbox_events
                        SET retry_count=retry_count+1,
                            next_attempt_at=now() + make_interval(secs => LEAST(3600, power(2, LEAST(retry_count, 11))::integer)),
                            last_error=?
                        WHERE id=?
                        """, exception.getClass().getSimpleName(), row.id());
                log.warn("Community event publish failed eventId={} type={} errorType={}",
                        row.id(), row.eventType(), exception.getClass().getSimpleName());
            }
        }
        return published;
    }

    private void publish(OutboxRow row) {
        String route = ROUTING_KEYS.get(row.eventType());
        if (route == null) throw new IllegalStateException("Unsupported community event type");
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

    private record OutboxRow(java.util.UUID id, String eventType, String payload, Instant createdAt, int retryCount) { }
}
