package com.ieltspath.events.assessment;

import com.ieltspath.events.messaging.RetryTopology;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.TopicExchange;

/**
 * The queue that carries {@link HumanReviewCompletedV1} from Assessment to Learning. Assessment and Learning both declare it
 * from here, with identical attributes, so the broker accepts either start order and no completed review is lost.
 */
public final class HumanReviewCompletedTopology {
    public static final String QUEUE = "learning.human-review-completed.v1";
    public static final String RETRY_QUEUE = "learning.human-review-completed.v1.retry";
    public static final String DLQ = "learning.human-review-completed.v1.dlq";
    public static final String DEAD_LETTER_EXCHANGE = "learning.human-review-completed.dlx";
    public static final long RETRY_DELAY_MS = 30_000L;
    public static final int MAX_RETRIES = 5;

    private HumanReviewCompletedTopology() { }

    /** Declared on the exchange Assessment publishes to; durable and not auto-deleted, like the other assessment queues. */
    public static Declarables declarables(String exchangeName) {
        TopicExchange exchange = new TopicExchange(exchangeName, true, false);
        return RetryTopology.of(new RetryTopology.Names(QUEUE, RETRY_QUEUE, DLQ, DEAD_LETTER_EXCHANGE),
                RETRY_DELAY_MS, exchange, AssessmentMessaging.HUMAN_REVIEW_COMPLETED_ROUTING_KEY);
    }
}
