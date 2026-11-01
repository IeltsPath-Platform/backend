package com.ieltspath.access.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Polls the outbox; kept separate from {@link AccessOutboxRelay} so each batch runs through its transactional proxy. */
@Component
@ConditionalOnProperty(name = "access.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class AccessOutboxRelayScheduler {
    private static final Logger log = LoggerFactory.getLogger(AccessOutboxRelayScheduler.class);

    private final AccessOutboxRelay relay;

    public AccessOutboxRelayScheduler(AccessOutboxRelay relay) {
        this.relay = relay;
    }

    @Scheduled(fixedDelayString = "${access.outbox.relay.poll-interval:PT2S}")
    public void relayPendingEvents() {
        try {
            relay.publishPendingBatch();
        } catch (RuntimeException exception) {
            // The next tick retries; a broken database or broker must not stop the scheduler thread.
            log.warn("Outbox relay batch failed: errorType={}", exception.getClass().getSimpleName());
        }
    }
}
