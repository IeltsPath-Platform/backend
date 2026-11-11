package com.ieltspath.notification.infrastructure;

import com.ieltspath.events.assessment.AssessmentCompletedV2;
import com.ieltspath.events.assessment.AssessmentMessaging;
import com.ieltspath.events.messaging.RetryTopology;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.TopicExchange;

public final class AssessmentResultReadyTopology {
    public static final String QUEUE = "notification.assessment-result-ready.v2";
    public static final String RETRY_QUEUE = QUEUE + ".retry";
    public static final String DLQ = QUEUE + ".dlq";
    public static final String DLX = QUEUE + ".dlx";
    public static final int MAX_RETRIES = 5;
    public static final long RETRY_DELAY_MS = 30_000L;

    private AssessmentResultReadyTopology() { }

    public static Declarables declarables() {
        return new Declarables(RetryTopology.of(
                new RetryTopology.Names(QUEUE, RETRY_QUEUE, DLQ, DLX), RETRY_DELAY_MS,
                new TopicExchange(AssessmentMessaging.EXCHANGE, true, false),
                AssessmentMessaging.COMPLETED_ROUTING_KEY).getDeclarables());
    }
}
