package com.ieltspath.user.infrastructure.messaging;

import com.ieltspath.commonobservability.OutboxTraceContext;
import com.ieltspath.user.domain.aggregate.OutboxEvent;
import com.ieltspath.user.domain.repository.OutboxEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class UserOutboxRelay {
    private static final Logger log = LoggerFactory.getLogger(UserOutboxRelay.class);
    private final OutboxEventRepository outbox;
    private final UserEventPublisher publisher;
    private final MeterRegistry meters;
    private final OutboxTraceContext traceContext;
    private final int batchSize;
    private final int maxAttempts;

    public UserOutboxRelay(OutboxEventRepository outbox, UserEventPublisher publisher, MeterRegistry meters,
                          OutboxTraceContext traceContext,
                          @Value("${user.outbox.relay.batch-size:50}") int batchSize,
                          @Value("${user.outbox.relay.max-attempts:5}") int maxAttempts) {
        this.outbox = outbox; this.publisher = publisher; this.meters = meters; this.traceContext = traceContext;
        this.batchSize = batchSize; this.maxAttempts = maxAttempts;
    }

    @Transactional
    public int publishPendingBatch() {
        int published = 0;
        for (OutboxEvent event : outbox.claimPending(batchSize, maxAttempts)) {
            OutboxTraceContext.Scope scope = traceContext.openRelay(event.traceContext());
            try {
                publisher.publish(event);
                outbox.markPublished(event.id(), Instant.now());
                published++;
            } catch (RuntimeException failure) {
                outbox.recordFailure(event.id(), failure.getClass().getSimpleName(), maxAttempts);
                meters.counter("outbox.relay.failures", "service", "user", "eventType", event.eventType()).increment();
                log.warn("User outbox publish failed eventId={} errorType={}", event.id(), failure.getClass().getSimpleName());
            } finally {
                scope.close();
            }
        }
        outbox.clearExpiredFailedPayloads(Instant.now().minusSeconds(15 * 60L));
        return published;
    }
}
