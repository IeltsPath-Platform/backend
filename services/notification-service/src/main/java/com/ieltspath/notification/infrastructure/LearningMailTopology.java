package com.ieltspath.notification.infrastructure;

import com.ieltspath.events.access.AccessMessaging;
import com.ieltspath.events.assessment.AssessmentMessaging;
import com.ieltspath.events.messaging.RetryTopology;
import com.ieltspath.events.user.UserMessaging;
import com.ieltspath.events.user.UserEmailVerificationStateV1;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.TopicExchange;

import java.util.ArrayList;
import java.util.List;

/**
 * Queues that feed the learning and billing mails. Each queue is bound to the routing key of an event that Notification
 * already receives elsewhere, so the existing consumers are untouched: every event is delivered to each bound queue.
 */
public final class LearningMailTopology {
    public static final String CONTACT_QUEUE = "notification.user-contact.v1";
    public static final String GRADING_COMPLETED_QUEUE = "notification.learning-grading-completed.v1";
    public static final String GRADING_OVERDUE_QUEUE = "notification.learning-grading-overdue.v1";
    public static final String GRADING_DUE_SOON_QUEUE = "notification.learning-grading-due-soon.v1";
    public static final String GRADING_REQUESTED_QUEUE = "notification.learning-grading-requested.v1";
    public static final String ASSESSMENT_COMPLETED_QUEUE = "notification.learning-assessment-completed.v1";
    public static final String SUBSCRIPTION_GRANTED_QUEUE = "notification.learning-subscription-granted.v1";

    public static final List<String> QUEUES = List.of(CONTACT_QUEUE, GRADING_COMPLETED_QUEUE, GRADING_OVERDUE_QUEUE,
            GRADING_DUE_SOON_QUEUE, GRADING_REQUESTED_QUEUE, ASSESSMENT_COMPLETED_QUEUE, SUBSCRIPTION_GRANTED_QUEUE);

    public static final String CONTAINER_FACTORY = "learningMailListenerContainerFactory";
    public static final int MAX_RETRIES = 5;
    public static final long RETRY_DELAY_MS = 30_000L;

    private LearningMailTopology() { }

    public static Declarables declarables() {
        List<Declarable> all = new ArrayList<>();
        all.addAll(bind(CONTACT_QUEUE, UserMessaging.EXCHANGE, UserEmailVerificationStateV1.EVENT_TYPE));
        all.addAll(bind(GRADING_COMPLETED_QUEUE, AssessmentMessaging.EXCHANGE,
                AssessmentMessaging.GRADING_COMPLETED_ROUTING_KEY));
        all.addAll(bind(GRADING_OVERDUE_QUEUE, AssessmentMessaging.EXCHANGE, AssessmentMessaging.GRADING_OVERDUE_ROUTING_KEY));
        all.addAll(bind(GRADING_DUE_SOON_QUEUE, AssessmentMessaging.EXCHANGE, AssessmentMessaging.GRADING_DUE_SOON_ROUTING_KEY));
        all.addAll(bind(GRADING_REQUESTED_QUEUE, AssessmentMessaging.EXCHANGE, AssessmentMessaging.GRADING_REQUESTED_ROUTING_KEY));
        all.addAll(bind(ASSESSMENT_COMPLETED_QUEUE, AssessmentMessaging.EXCHANGE, AssessmentMessaging.COMPLETED_ROUTING_KEY));
        all.addAll(bind(SUBSCRIPTION_GRANTED_QUEUE, AccessMessaging.EXCHANGE, AccessMessaging.SUBSCRIPTION_GRANTED_ROUTING_KEY));
        return new Declarables(all);
    }

    private static List<Declarable> bind(String queue, String exchange, String routingKey) {
        return new ArrayList<>(RetryTopology.of(
                new RetryTopology.Names(queue, queue + ".retry", queue + ".dlq", queue + ".dlx"),
                RETRY_DELAY_MS, new TopicExchange(exchange, true, false), routingKey).getDeclarables());
    }
}
