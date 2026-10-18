package com.ieltspath.community.infrastructure.messaging;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CommunityOutboxRelayScheduler {
    private final CommunityOutboxRelay relay;

    public CommunityOutboxRelayScheduler(CommunityOutboxRelay relay) {
        this.relay = relay;
    }

    @Scheduled(fixedDelayString = "${community.outbox.relay.poll-interval:PT2S}")
    public void publishPending() {
        relay.publishPendingBatch();
    }
}
