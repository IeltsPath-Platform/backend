package com.ieltspath.library.infrastructure.messaging;

import com.ieltspath.commonobservability.Correlation;
import com.ieltspath.events.library.LibraryMessaging;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Moves committed outbox rows to the broker. Rows are claimed with SKIP LOCKED, so several instances can run it without
 * publishing the same row twice at once. An event counts as published only when the broker confirmed it and routed it to a
 * queue. Delivery is at-least-once: a crash between the confirm and the commit publishes the same event id again, which
 * consumers deduplicate.
 */
@Component
public class LibraryOutboxRelay {
    private static final Logger log = LoggerFactory.getLogger(LibraryOutboxRelay.class);
    /** Matches the length of the last_error column. */
    private static final int MAX_ERROR_LENGTH = 500;

    private record Row(UUID id, UUID aggregateId, String eventType, String payload, Instant createdAt, String correlationId) { }

    private final NamedParameterJdbcTemplate jdbc;
    private final RabbitTemplate rabbitTemplate;
    private final MeterRegistry meters;
    private final int batchSize;
    private final int maxAttempts;
    private final Duration confirmTimeout;

    public LibraryOutboxRelay(NamedParameterJdbcTemplate jdbc, RabbitTemplate rabbitTemplate, MeterRegistry meters,
                              @Value("${library.outbox.relay.batch-size:50}") int batchSize,
                              @Value("${library.outbox.relay.max-attempts:20}") int maxAttempts,
                              @Value("${library.outbox.relay.confirm-timeout:PT5S}") Duration confirmTimeout) {
        this.jdbc = jdbc;
        this.rabbitTemplate = rabbitTemplate;
        this.meters = meters;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
        this.confirmTimeout = confirmTimeout;
    }

    /** @return the number of events the broker confirmed in this batch. */
    @Transactional
    public int publishPendingBatch() {
        int published = 0;
        for (Row row : claimPending()) {
            try {
                publish(row);
                jdbc.update("update outbox_events set status = 'PUBLISHED', published_at = :at, last_error = null where id = :id",
                        new MapSqlParameterSource("id", row.id()).addValue("at", Timestamp.from(Instant.now())));
                published++;
            } catch (RuntimeException failure) {
                log.warn("Outbox event {} ({}) was not published: errorType={}", row.id(), row.eventType(),
                        failure.getClass().getSimpleName());
                meters.counter("outbox.relay.failures", "service", "library", "eventType", row.eventType()).increment();
                recordFailure(row.id(), failure.getMessage());
            }
        }
        return published;
    }

    private List<Row> claimPending() {
        return jdbc.query("""
                select id, aggregate_id, event_type, payload::text payload, created_at, correlation_id
                from outbox_events
                where status = 'PENDING' and retry_count < :maxAttempts
                order by created_at
                limit :limit
                for update skip locked
                """, new MapSqlParameterSource("maxAttempts", maxAttempts).addValue("limit", batchSize),
                (rs, i) -> new Row(rs.getObject("id", UUID.class), rs.getObject("aggregate_id", UUID.class),
                        rs.getString("event_type"), rs.getString("payload"), rs.getTimestamp("created_at").toInstant(),
                        rs.getString("correlation_id")));
    }

    private void recordFailure(UUID id, String error) {
        String message = error == null ? "" : error;
        jdbc.update("""
                update outbox_events
                set retry_count = retry_count + 1, last_error = :error,
                    status = case when retry_count + 1 >= :maxAttempts then 'FAILED' else status end
                where id = :id
                """, new MapSqlParameterSource("id", id).addValue("maxAttempts", maxAttempts)
                .addValue("error", message.length() > MAX_ERROR_LENGTH ? message.substring(0, MAX_ERROR_LENGTH) : message));
    }

    /** Each outbox type has its own routing key, so consumers bind only to what they handle. */
    private static String routingKeyFor(String outboxEventType) {
        return switch (outboxEventType) {
            case "VocabularyReviewDue" -> LibraryMessaging.VOCABULARY_REVIEW_DUE_ROUTING_KEY;
            default -> throw new IllegalStateException("Event type is not relayed: " + outboxEventType);
        };
    }

    private void publish(Row row) {
        String routingKey = routingKeyFor(row.eventType());
        var builder = MessageBuilder.withBody(row.payload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setMessageId(row.id().toString())
                .setType(routingKey)
                .setTimestamp(Date.from(row.createdAt()))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setHeader("aggregate_id", row.aggregateId().toString());
        if (row.correlationId() != null) builder.setHeader(Correlation.HEADER, row.correlationId());
        Message message = builder.build();
        CorrelationData correlation = new CorrelationData(row.id().toString());
        rabbitTemplate.send(LibraryMessaging.EXCHANGE, routingKey, message, correlation);
        CorrelationData.Confirm confirm;
        try {
            confirm = correlation.getFuture().get(confirmTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for broker confirm", interrupted);
        } catch (ExecutionException | TimeoutException failure) {
            throw new IllegalStateException("Broker did not confirm the event", failure);
        }
        if (!confirm.isAck()) throw new IllegalStateException("Broker rejected the event: " + confirm.getReason());
        if (correlation.getReturned() != null) {
            throw new IllegalStateException("Event was not routed to any queue: " + correlation.getReturned().getReplyText());
        }
    }
}
