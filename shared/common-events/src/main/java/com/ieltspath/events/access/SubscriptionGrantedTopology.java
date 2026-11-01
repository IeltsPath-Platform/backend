package com.ieltspath.events.access;

import com.ieltspath.events.messaging.RetryTopology;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.TopicExchange;

/**
 * The queue that carries {@link SubscriptionGrantedV1} from Access to Notification. Both services declare it from here, so
 * the two declarations are identical, which the broker requires, and a notice is not lost when Notification starts later.
 */
public final class SubscriptionGrantedTopology {
    public static final String QUEUE = "notification.subscription-granted.v1";
    public static final String RETRY_QUEUE = "notification.subscription-granted.v1.retry";
    public static final String DLQ = "notification.subscription-granted.v1.dlq";
    public static final String DEAD_LETTER_EXCHANGE = "notification.subscription-granted.dlx";
    public static final long RETRY_DELAY_MS = 30_000L;
    public static final int MAX_RETRIES = 5;

    private SubscriptionGrantedTopology() { }

    /** Declared on the exchange Access publishes to; durable and not auto-deleted. */
    public static Declarables declarables(String exchangeName) {
        TopicExchange exchange = new TopicExchange(exchangeName, true, false);
        return RetryTopology.of(new RetryTopology.Names(QUEUE, RETRY_QUEUE, DLQ, DEAD_LETTER_EXCHANGE),
                RETRY_DELAY_MS, exchange, AccessMessaging.SUBSCRIPTION_GRANTED_ROUTING_KEY);
    }
}
