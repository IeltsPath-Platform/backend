package com.ieltspath.events.library;

import com.ieltspath.events.messaging.RetryTopology;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.TopicExchange;

/**
 * Notification queue for {@link VocabularyReviewDueV1}, with its retry and dead-letter queues. Library and Notification
 * both declare it, so the queue exists whichever service starts first and the relay's mandatory publish always has a route.
 */
public final class VocabularyReviewDueTopology {
    public static final String QUEUE = "notification.vocabulary-review-due.v1";
    public static final String RETRY_QUEUE = QUEUE + ".retry";
    public static final String DLQ = QUEUE + ".dlq";
    public static final String DEAD_LETTER_EXCHANGE = "notification.vocabulary-review-due.dlx";
    public static final long RETRY_DELAY_MS = 30_000L;
    public static final int MAX_RETRIES = 5;

    private VocabularyReviewDueTopology() { }

    public static Declarables declarables() {
        TopicExchange exchange = new TopicExchange(LibraryMessaging.EXCHANGE, true, false);
        return RetryTopology.of(new RetryTopology.Names(QUEUE, RETRY_QUEUE, DLQ, DEAD_LETTER_EXCHANGE),
                RETRY_DELAY_MS, exchange, LibraryMessaging.VOCABULARY_REVIEW_DUE_ROUTING_KEY);
    }
}
