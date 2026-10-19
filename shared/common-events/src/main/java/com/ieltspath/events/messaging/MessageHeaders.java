package com.ieltspath.events.messaging;

/** Headers used by the points grant consumers in Access and Learning; see docs/contracts/access-points-v1.md. */
public final class MessageHeaders {
    /** How many times a message has been parked on a delayed retry queue; set by the consumer that retries it. */
    public static final String RETRY_COUNT = "x-retry-count";

    private MessageHeaders() { }
}
