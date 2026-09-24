package com.group01.assessment.infrastructure.messaging;

import com.group01.assessment.domain.entity.OutboxEvent;

/** Delivers one committed outbox event to the broker, returning only once the broker has accepted it. */
public interface OutboxEventPublisher {
    void publish(OutboxEvent event);

    class PublishFailedException extends RuntimeException {
        public PublishFailedException(String message) { super(message); }
        public PublishFailedException(String message, Throwable cause) { super(message, cause); }
    }
}
