package com.ieltspath.events.messaging;

/** A dead-letter queue a service declares and the queue its messages go back to when replayed. */
public record DeadLetterRoute(String deadLetterQueue, String queue) { }
