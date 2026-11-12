package com.ieltspath.events.user;

import com.ieltspath.events.messaging.RetryTopology;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.TopicExchange;

/** Notification queue for {@link EmailVerificationRequestedV1}, with its retry and dead-letter queues. */
public final class EmailVerificationRequestedTopology {
    public static final String QUEUE = "notification.email-verification-requested.v1";
    public static final String RETRY_QUEUE = QUEUE + ".retry";
    public static final String DLQ = QUEUE + ".dlq";
    public static final String DEAD_LETTER_EXCHANGE = "notification.email-verification-requested.dlx";
    public static final long RETRY_DELAY_MS = 30_000L;
    public static final int MAX_RETRIES = 5;

    private EmailVerificationRequestedTopology() { }

    public static Declarables declarables() {
        TopicExchange exchange = new TopicExchange(UserMessaging.EXCHANGE, true, false);
        return RetryTopology.of(new RetryTopology.Names(QUEUE, RETRY_QUEUE, DLQ, DEAD_LETTER_EXCHANGE),
                RETRY_DELAY_MS, exchange, UserMessaging.EMAIL_VERIFICATION_REQUESTED_ROUTING_KEY);
    }
}
