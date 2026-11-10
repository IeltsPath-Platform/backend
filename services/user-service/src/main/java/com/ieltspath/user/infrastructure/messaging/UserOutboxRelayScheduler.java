package com.ieltspath.user.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "user.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class UserOutboxRelayScheduler {
    private static final Logger log = LoggerFactory.getLogger(UserOutboxRelayScheduler.class);
    private final UserOutboxRelay relay;

    public UserOutboxRelayScheduler(UserOutboxRelay relay) { this.relay = relay; }

    @Scheduled(fixedDelayString = "${user.outbox.relay.poll-interval:PT2S}")
    public void relayPendingEvents() {
        try { relay.publishPendingBatch(); }
        catch (RuntimeException failure) {
            log.warn("User outbox relay batch failed errorType={}", failure.getClass().getSimpleName());
        }
    }
}
