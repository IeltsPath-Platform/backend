package com.ieltspath.events.messaging;

/** Headers used by the points grant consumers in Access and Learning; see docs/contracts/access-points-v1.md. */
public final class MessageHeaders {
    /** How many times a message has been parked on a delayed retry queue; set by the consumer that retries it. */
    public static final String RETRY_COUNT = "x-retry-count";
    /**
     * The routing key a message arrived with, recorded when a consumer parks it on a dead-letter exchange. The dead-letter
     * routing key replaces it, so a replay needs this header to send the message back on its original route.
     */
    public static final String ORIGINAL_ROUTING_KEY = "x-original-routing-key";

    private MessageHeaders() { }
}
