package com.ieltspath.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.events.assessment.GradingOverdueV1;
import com.ieltspath.events.messaging.MessageRetry;
import com.ieltspath.notification.application.RecordGradingOverdueUseCase;
import com.ieltspath.notification.domain.vo.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Stores the notice and pushes it to the learner. A payload that breaks the contract is refused and dead-lettered. Any
 * other failure is treated as temporary: the message goes to the retry queue and, after the allowed retries, to the
 * dead-letter queue. Storing is idempotent per event id, so handling a message twice never notifies twice.
 */
@Component
public class GradingOverdueListener {
    private static final Logger log = LoggerFactory.getLogger(GradingOverdueListener.class);

    private final ObjectMapper mapper;
    private final RecordGradingOverdueUseCase record;
    private final NoticePushBridge noticePush;
    private final MessageRetry retry;

    public GradingOverdueListener(ObjectMapper mapper, RecordGradingOverdueUseCase record, NoticePushBridge noticePush,
                                  @Qualifier("gradingOverdueRetry") MessageRetry retry) {
        this.mapper = mapper;
        this.record = record;
        this.noticePush = noticePush;
        this.retry = retry;
    }

    @RabbitListener(queues = GradingOverdueMessagingConfig.QUEUE,
            containerFactory = GradingOverdueMessagingConfig.CONTAINER_FACTORY)
    public void onGradingOverdue(Message message) {
        GradingOverdueV1 event;
        try {
            event = mapper.readValue(message.getBody(), GradingOverdueV1.class);
        } catch (IOException | RuntimeException unreadable) {
            throw new AmqpRejectAndDontRequeueException("Unreadable grading overdue event", unreadable);
        }
        try {
            record.execute(event, message.getMessageProperties().getReceivedRoutingKey(),
                    message.getMessageProperties().getMessageId()).ifPresent(this::push);
        } catch (IllegalArgumentException breaksContract) {
            throw new AmqpRejectAndDontRequeueException("Grading overdue event breaks the contract", breaksContract);
        } catch (RuntimeException temporary) {
            log.warn("Grading overdue notice will be retried: errorType={}", temporary.getClass().getSimpleName());
            retry.retryLater(message, temporary);
        }
    }

    private void push(Notification notice) {
        noticePush.pushToUser(notice.recipientId(), Map.of("id", notice.id(),
                    "actorId", notice.actorId(), "eventType", notice.eventType(),
                    "gradingJobId", notice.gradingJobId(), "createdAt", notice.createdAt().toString()));
    }
}
