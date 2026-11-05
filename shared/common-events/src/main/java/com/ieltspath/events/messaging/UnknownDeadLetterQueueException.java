package com.ieltspath.events.messaging;

/** The requested queue is not one of the dead-letter queues the service declares. */
public class UnknownDeadLetterQueueException extends RuntimeException {
    public UnknownDeadLetterQueueException(String deadLetterQueue) {
        super("Unknown dead-letter queue: " + deadLetterQueue);
    }
}