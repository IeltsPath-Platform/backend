package com.ieltspath.events.messaging;

/**
 * A dead-letter queue a service declares, and where its messages go when an operator replays them. A replay publishes
 * through {@code exchange} with {@code routingKey}, so the consumer sees the routing key of the original delivery.
 * Use {@link #toQueue} for queues a message reaches through the default exchange, where the routing key is the queue name.
 * Use {@link #byOriginalRoutingKey} when one dead-letter queue holds messages for several routing keys; the consumer must
 * then record the key in {@link MessageHeaders#ORIGINAL_ROUTING_KEY} when it parks a message.
 */
public record DeadLetterRoute(String deadLetterQueue, String queue, String exchange, String routingKey) {

    public static DeadLetterRoute toQueue(String deadLetterQueue, String queue) {
        return new DeadLetterRoute(deadLetterQueue, queue, "", queue);
    }

    /** A route whose routing key is read from each message's {@link MessageHeaders#ORIGINAL_ROUTING_KEY} header. */
    public static DeadLetterRoute byOriginalRoutingKey(String deadLetterQueue, String queue, String exchange) {
        return new DeadLetterRoute(deadLetterQueue, queue, exchange, null);
    }
}