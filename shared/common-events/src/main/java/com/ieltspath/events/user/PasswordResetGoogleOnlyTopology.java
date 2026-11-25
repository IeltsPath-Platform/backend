package com.ieltspath.events.user;

import com.ieltspath.events.messaging.RetryTopology;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.TopicExchange;

/** Notification queue for {@link PasswordResetGoogleOnlyV1}, with its retry and dead-letter queues. */
public final class PasswordResetGoogleOnlyTopology {
    public static final String QUEUE = "notification.password-reset-google-only.v1";
    public static final String RETRY_QUEUE = QUEUE + ".retry";
    public static final String DLQ = QUEUE + ".dlq";
    public static final String DEAD_LETTER_EXCHANGE = "notification.password-reset-google-only.dlx";
    public static final long RETRY_DELAY_MS = 30_000L;
    public static final int MAX_RETRIES = 5;

    private PasswordResetGoogleOnlyTopology() { }

    public static Declarables declarables() {
        TopicExchange exchange = new TopicExchange(UserMessaging.EXCHANGE, true, false);
        return RetryTopology.of(new RetryTopology.Names(QUEUE, RETRY_QUEUE, DLQ, DEAD_LETTER_EXCHANGE),
                RETRY_DELAY_MS, exchange, UserMessaging.PASSWORD_RESET_GOOGLE_ONLY_ROUTING_KEY);
    }
}
