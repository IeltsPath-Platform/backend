package com.ieltspath.events.access;

import com.ieltspath.events.messaging.RetryTopology;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.TopicExchange;

/** Notification queue for {@link SubscriptionExpiryNoticeV1}, with its retry and dead-letter queues. */
public final class SubscriptionExpiryNoticeTopology {
    public static final String QUEUE = "notification.subscription-expiry-notice.v1";
    public static final String RETRY_QUEUE = QUEUE + ".retry";
    public static final String DLQ = QUEUE + ".dlq";
    public static final String DEAD_LETTER_EXCHANGE = "notification.subscription-expiry-notice.dlx";
    public static final long RETRY_DELAY_MS = 30_000L;
    public static final int MAX_RETRIES = 5;

    private SubscriptionExpiryNoticeTopology() { }

    public static Declarables declarables() {
        TopicExchange exchange = new TopicExchange(AccessMessaging.EXCHANGE, true, false);
        return RetryTopology.of(new RetryTopology.Names(QUEUE, RETRY_QUEUE, DLQ, DEAD_LETTER_EXCHANGE),
                RETRY_DELAY_MS, exchange, AccessMessaging.SUBSCRIPTION_EXPIRY_NOTICE_ROUTING_KEY);
    }
}
