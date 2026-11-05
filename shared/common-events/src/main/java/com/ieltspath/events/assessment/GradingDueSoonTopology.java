package com.ieltspath.events.assessment;

import com.ieltspath.events.messaging.RetryTopology;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.TopicExchange;

/**
 * The queue that carries {@link GradingDueSoonV1} from Assessment to Notification. Like {@link GradingOverdueTopology},
 * both services declare it from here, so a reminder is never lost because Notification had not started yet and the two
 * declarations are identical, which the broker requires.
 */
public final class GradingDueSoonTopology {
    public static final String QUEUE = "notification.grading-due-soon.v1";
    public static final String RETRY_QUEUE = "notification.grading-due-soon.v1.retry";
    public static final String DLQ = "notification.grading-due-soon.v1.dlq";
    public static final String DEAD_LETTER_EXCHANGE = "notification.grading-due-soon.dlx";
    public static final long RETRY_DELAY_MS = 30_000L;
    public static final int MAX_RETRIES = 5;

    private GradingDueSoonTopology() { }

    /** Declared on the same exchange Assessment publishes to; durable and not auto-deleted, like Assessment's own. */
    public static Declarables declarables(String exchangeName) {
        TopicExchange exchange = new TopicExchange(exchangeName, true, false);
        return RetryTopology.of(new RetryTopology.Names(QUEUE, RETRY_QUEUE, DLQ, DEAD_LETTER_EXCHANGE),
                RETRY_DELAY_MS, exchange, AssessmentMessaging.GRADING_DUE_SOON_ROUTING_KEY);
    }
}
