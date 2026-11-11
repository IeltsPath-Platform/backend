package com.ieltspath.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.events.access.SubscriptionGrantedV1;
import com.ieltspath.events.assessment.AssessmentCompletedV2;
import com.ieltspath.events.assessment.GradingCompletedV1;
import com.ieltspath.events.assessment.GradingDueSoonV1;
import com.ieltspath.events.assessment.GradingOverdueV1;
import com.ieltspath.events.assessment.GradingRequestedV1;
import com.ieltspath.events.messaging.MessageRetry;
import com.ieltspath.events.user.UserEmailVerificationStateV1;
import com.ieltspath.notification.application.LearningMailUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Feeds the learning and billing mails. A payload that cannot be read or breaks its contract is refused and dead-lettered;
 * any other failure goes to the queue's retry route. Results are sent once per source event id.
 */
@Component
@ConditionalOnExpression(LearningMailGate.EXPRESSION)
public class LearningMailListener {
    private static final Logger log = LoggerFactory.getLogger(LearningMailListener.class);
    private static final String PLACEMENT = "PLACEMENT";
    private static final String MOCK = "MOCK";

    private final ObjectMapper mapper;
    private final LearningMailUseCase mail;
    private final Map<String, MessageRetry> retries = new HashMap<>();

    public LearningMailListener(ObjectMapper mapper, LearningMailUseCase mail, RabbitTemplate rabbitTemplate) {
        this.mapper = mapper;
        this.mail = mail;
        for (String queue : LearningMailTopology.QUEUES) {
            retries.put(queue, new MessageRetry(rabbitTemplate, queue + ".retry", LearningMailTopology.MAX_RETRIES));
        }
    }

    @RabbitListener(queues = LearningMailTopology.CONTACT_QUEUE, containerFactory = LearningMailTopology.CONTAINER_FACTORY)
    public void onUserContact(Message message) {
        var event = parse(message, UserEmailVerificationStateV1.class);
        settle(message, LearningMailTopology.CONTACT_QUEUE, () -> mail.recordContact(event));
    }

    @RabbitListener(queues = LearningMailTopology.GRADING_COMPLETED_QUEUE, containerFactory = LearningMailTopology.CONTAINER_FACTORY)
    public void onGradingCompleted(Message message) {
        var event = parse(message, GradingCompletedV1.class);
        settle(message, LearningMailTopology.GRADING_COMPLETED_QUEUE,
                () -> mail.gradingCompleted(event.eventId(), event.learnerId()));
    }

    @RabbitListener(queues = LearningMailTopology.GRADING_OVERDUE_QUEUE, containerFactory = LearningMailTopology.CONTAINER_FACTORY)
    public void onGradingOverdue(Message message) {
        var event = parse(message, GradingOverdueV1.class);
        settle(message, LearningMailTopology.GRADING_OVERDUE_QUEUE,
                () -> mail.gradingOverdue(event.eventId(), event.learnerId(), event.gradingJobId()));
    }

    @RabbitListener(queues = LearningMailTopology.GRADING_DUE_SOON_QUEUE, containerFactory = LearningMailTopology.CONTAINER_FACTORY)
    public void onGradingDueSoon(Message message) {
        var event = parse(message, GradingDueSoonV1.class);
        settle(message, LearningMailTopology.GRADING_DUE_SOON_QUEUE,
                () -> mail.examinerDueSoon(event.eventId(), event.examinerId()));
    }

    @RabbitListener(queues = LearningMailTopology.GRADING_REQUESTED_QUEUE, containerFactory = LearningMailTopology.CONTAINER_FACTORY)
    public void onGradingRequested(Message message) {
        var event = parse(message, GradingRequestedV1.class);
        settle(message, LearningMailTopology.GRADING_REQUESTED_QUEUE,
                () -> mail.examinerNewJob(event.gradingJobId(), event.examinerId(), event.occurredAt()));
    }

    /** Only the first graded version of a placement or mock result: a re-grade does not mail the learner again. */
    @RabbitListener(queues = LearningMailTopology.ASSESSMENT_COMPLETED_QUEUE, containerFactory = LearningMailTopology.CONTAINER_FACTORY)
    public void onAssessmentCompleted(Message message) {
        var event = parse(message, AssessmentCompletedV2.class);
        var data = event.data();
        if (data == null || data.userId() == null) return;
        boolean relevant = PLACEMENT.equals(data.assessmentType()) || MOCK.equals(data.assessmentType());
        if (!relevant || data.resultVersion() != 1) return;
        settle(message, LearningMailTopology.ASSESSMENT_COMPLETED_QUEUE,
                () -> mail.resultReady(event.eventId(), data.userId()));
    }

    @RabbitListener(queues = LearningMailTopology.SUBSCRIPTION_GRANTED_QUEUE, containerFactory = LearningMailTopology.CONTAINER_FACTORY)
    public void onSubscriptionGranted(Message message) {
        var event = parse(message, SubscriptionGrantedV1.class);
        settle(message, LearningMailTopology.SUBSCRIPTION_GRANTED_QUEUE,
                () -> mail.subscriptionGranted(event.eventId(), event.userId(), event.endsAt()));
    }

    private <T> T parse(Message message, Class<T> type) {
        try {
            return mapper.readValue(message.getBody(), type);
        } catch (IOException | RuntimeException malformed) {
            throw new AmqpRejectAndDontRequeueException("Unreadable " + type.getSimpleName(), malformed);
        }
    }

    private void settle(Message message, String queue, Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException invalidContract) {
            throw new AmqpRejectAndDontRequeueException("Event breaks its contract on " + queue, invalidContract);
        } catch (RuntimeException temporary) {
            log.warn("Learning mail will be retried queue={} errorType={}", queue, temporary.getClass().getSimpleName());
            retries.get(queue).retryLater(message, temporary);
        }
    }
}
