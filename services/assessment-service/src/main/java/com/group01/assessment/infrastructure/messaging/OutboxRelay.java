package com.group01.assessment.infrastructure.messaging;

import com.group01.assessment.domain.entity.OutboxEvent;
import com.group01.assessment.domain.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Moves committed outbox rows to the broker. Only rows visible to this transaction are claimed, so an event
 * whose business transaction rolled back is never published. Delivery is at-least-once: a crash after the
 * broker confirm but before this commit republishes the same event id, which consumers deduplicate.
 */
@Component
public class OutboxRelay {
    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository outbox;
    private final OutboxEventPublisher publisher;
    private final int batchSize;
    private final int maxAttempts;

    public OutboxRelay(OutboxEventRepository outbox, OutboxEventPublisher publisher,
                       @Value("${assessment.outbox.relay.batch-size:50}") int batchSize,
                       @Value("${assessment.outbox.relay.max-attempts:20}") int maxAttempts) {
        this.outbox = outbox;
        this.publisher = publisher;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
    }

    /** @return the number of events the broker confirmed in this batch. */
    @Transactional
    public int publishPendingBatch() {
        int published = 0;
        for (OutboxEvent event : outbox.claimUnpublished(batchSize, maxAttempts)) {
            try {
                publisher.publish(event);
                outbox.markPublished(event.id(), Instant.now());
                published++;
            } catch (RuntimeException exception) {
                log.warn("Outbox event {} ({}) was not published: {}", event.id(), event.eventType(),
                        exception.getMessage());
                outbox.recordFailure(event.id(), exception.getMessage());
            }
        }
        return published;
    }
}
