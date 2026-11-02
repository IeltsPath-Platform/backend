package com.ieltspath.access.infrastructure.messaging;

import com.ieltspath.access.domain.entity.OutboxEvent;

/** Sends one outbox event to the broker. Returns only when the broker has confirmed the event; otherwise throws. */
public interface AccessEventPublisher {
    void publish(OutboxEvent event);
}
