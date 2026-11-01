package com.ieltspath.access.infrastructure.messaging;

import com.ieltspath.access.domain.entity.OutboxEvent;
import com.ieltspath.access.domain.repository.OutboxEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Moves committed outbox rows to the broker. Only rows visible to this transaction are claimed, so an event whose
 * business transaction rolled back is never published. Delivery is at-least-once: a crash after the broker confirm but
 * before this commit publishes the same event id again, which consumers deduplicate.
 */
@Component
public class AccessOutboxRelay {
    private static final Logger log = LoggerFactory.getLogger(AccessOutboxRelay.class);

    private final OutboxEventRepository outbox;
    private final AccessEventPublisher publisher;
    private final MeterRegistry meters;
    private final int batchSize;
    private final int maxAttempts;

    public AccessOutboxRelay(OutboxEventRepository outbox, AccessEventPublisher publisher, MeterRegistry meters,
                             @Value("${access.outbox.relay.batch-size:50}") int batchSize,
                             @Value("${access.outbox.relay.max-attempts:20}") int maxAttempts) {
        this.outbox = outbox;
        this.publisher = publisher;
        this.meters = meters;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
    }

    /** @return the number of events the broker confirmed in this batch. */
    @Transactional
    public int publishPendingBatch() {
        int published = 0;
        for (OutboxEvent event : outbox.claimPending(batchSize, maxAttempts)) {
            try {
                publisher.publish(event);
                outbox.markPublished(event.getId(), Instant.now());
                published++;
            } catch (RuntimeException exception) {
                boolean giveUp = event.getRetryCount() + 1 >= maxAttempts;
                log.warn("Outbox event {} ({}) was not published: attempt={} giveUp={} errorType={}", event.getId(),
                        event.getEventType(), event.getRetryCount() + 1, giveUp, exception.getClass().getSimpleName());
                meters.counter("outbox.relay.failures", "service", "access", "eventType", event.getEventType()).increment();
                outbox.recordFailure(event.getId(), exception.getMessage(), maxAttempts);
            }
        }
        return published;
    }
}
