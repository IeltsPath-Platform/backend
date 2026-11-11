package com.ieltspath.notification.infrastructure;

import com.ieltspath.events.access.AccessMessaging;
import com.ieltspath.events.access.SubscriptionExpiryNoticeV1;
import com.ieltspath.events.messaging.RetryTopology;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.TopicExchange;

public final class SubscriptionExpiryInboxTopology {
    public static final String QUEUE = "notification.subscription-expiry-inbox.v1";
    public static final String RETRY_QUEUE = QUEUE + ".retry";
    public static final String DLQ = QUEUE + ".dlq";
    public static final String DLX = QUEUE + ".dlx";
    public static final int MAX_RETRIES = 5;
    public static final long RETRY_DELAY_MS = 30_000L;

    private SubscriptionExpiryInboxTopology() { }

    public static Declarables declarables() {
        return new Declarables(RetryTopology.of(
                new RetryTopology.Names(QUEUE, RETRY_QUEUE, DLQ, DLX), RETRY_DELAY_MS,
                new TopicExchange(AccessMessaging.EXCHANGE, true, false),
                SubscriptionExpiryNoticeV1.EVENT_TYPE).getDeclarables());
    }
}
