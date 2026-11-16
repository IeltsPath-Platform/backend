package com.ieltspath.library.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Polls the outbox; kept separate from {@link LibraryOutboxRelay} so each batch runs through its transactional proxy. */
@Component
@ConditionalOnProperty(name = "library.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class LibraryOutboxRelayScheduler {
    private static final Logger log = LoggerFactory.getLogger(LibraryOutboxRelayScheduler.class);

    private final LibraryOutboxRelay relay;

    public LibraryOutboxRelayScheduler(LibraryOutboxRelay relay) {
        this.relay = relay;
    }

    @Scheduled(fixedDelayString = "${library.outbox.relay.poll-interval:PT2S}")
    public void relayPendingEvents() {
        try {
            relay.publishPendingBatch();
        } catch (RuntimeException failure) {
            // The next tick retries; a broken database or broker must not stop the scheduler thread.
            log.warn("Outbox relay batch failed: errorType={}", failure.getClass().getSimpleName());
        }
    }
}
