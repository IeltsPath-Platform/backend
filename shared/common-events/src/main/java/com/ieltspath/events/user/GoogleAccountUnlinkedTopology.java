package com.ieltspath.events.user;

import com.ieltspath.events.messaging.RetryTopology;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.TopicExchange;

public final class GoogleAccountUnlinkedTopology {
    public static final String QUEUE = "notification.google-account-unlinked.v1";
    public static final String RETRY_QUEUE = QUEUE + ".retry";
    public static final String DLQ = QUEUE + ".dlq";
    public static final String DEAD_LETTER_EXCHANGE = "notification.google-account-unlinked.dlx";
    public static final long RETRY_DELAY_MS = 30_000L;
    public static final int MAX_RETRIES = 5;

    private GoogleAccountUnlinkedTopology() { }

    public static Declarables declarables() {
        TopicExchange exchange = new TopicExchange(UserMessaging.EXCHANGE, true, false);
        return RetryTopology.of(new RetryTopology.Names(QUEUE, RETRY_QUEUE, DLQ, DEAD_LETTER_EXCHANGE),
                RETRY_DELAY_MS, exchange, UserMessaging.GOOGLE_ACCOUNT_UNLINKED_ROUTING_KEY);
    }
}
