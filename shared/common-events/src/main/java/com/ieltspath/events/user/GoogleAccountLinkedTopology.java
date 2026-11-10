package com.ieltspath.events.user;

import com.ieltspath.events.messaging.RetryTopology;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.TopicExchange;

/** Notification queue for {@link GoogleAccountLinkedV1}, with its retry and dead-letter queues. */
public final class GoogleAccountLinkedTopology {
    public static final String QUEUE = "notification.google-account-linked.v1";
    public static final String RETRY_QUEUE = QUEUE + ".retry";
    public static final String DLQ = QUEUE + ".dlq";
    public static final String DEAD_LETTER_EXCHANGE = "notification.google-account-linked.dlx";
    public static final long RETRY_DELAY_MS = 30_000L;
    public static final int MAX_RETRIES = 5;

    private GoogleAccountLinkedTopology() { }

    public static Declarables declarables() {
        TopicExchange exchange = new TopicExchange(UserMessaging.EXCHANGE, true, false);
        return RetryTopology.of(new RetryTopology.Names(QUEUE, RETRY_QUEUE, DLQ, DEAD_LETTER_EXCHANGE),
                RETRY_DELAY_MS, exchange, UserMessaging.GOOGLE_ACCOUNT_LINKED_ROUTING_KEY);
    }
}
