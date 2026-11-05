package com.ieltspath.events.assessment;

import com.ieltspath.events.messaging.RetryTopology;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.TopicExchange;

/**
 * The queue that carries {@link GradingOverdueV1} from Assessment to Notification. Notification consumes it, but Assessment
 * declares it too: the broker only accepts an event that reaches a queue, and an event that was refused enough times is
 * parked for good, so a queue that does not exist yet (Assessment started before Notification ever did) would lose notices.
 * Both services build the topology here, so the two declarations are identical, which the broker requires.
 */
public final class GradingOverdueTopology {
    public static final String QUEUE = "notification.grading-overdue.v1";
    public static final String RETRY_QUEUE = "notification.grading-overdue.v1.retry";
    public static final String DLQ = "notification.grading-overdue.v1.dlq";
    public static final String DEAD_LETTER_EXCHANGE = "notification.grading-overdue.dlx";
    public static final long RETRY_DELAY_MS = 30_000L;
    public static final int MAX_RETRIES = 5;

    private GradingOverdueTopology() { }

    /** Declared on the same exchange Assessment publishes to; durable and not auto-deleted, like Assessment's own. */
    public static Declarables declarables(String exchangeName) {
        TopicExchange exchange = new TopicExchange(exchangeName, true, false);
        return RetryTopology.of(new RetryTopology.Names(QUEUE, RETRY_QUEUE, DLQ, DEAD_LETTER_EXCHANGE),
                RETRY_DELAY_MS, exchange, AssessmentMessaging.GRADING_OVERDUE_ROUTING_KEY);
    }
}
