package com.ieltspath.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.events.assessment.GradingRequestedV1;
import com.ieltspath.events.messaging.MessageRetry;
import com.ieltspath.notification.application.RecordGradingRequestedUseCase;
import com.ieltspath.notification.domain.vo.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Stores the notice and pushes it to the examiner. A payload that breaks the contract is refused and dead-lettered. Any
 * other failure is treated as temporary: the message goes to the retry queue and, after the allowed retries, to the
 * dead-letter queue. Storing is idempotent per event id, so handling a message twice never notifies twice.
 */
@Component
public class GradingRequestedListener {
    private static final Logger log = LoggerFactory.getLogger(GradingRequestedListener.class);

    private final ObjectMapper mapper;
    private final RecordGradingRequestedUseCase record;
    private final SimpMessagingTemplate socket;
    private final MessageRetry retry;

    public GradingRequestedListener(ObjectMapper mapper, RecordGradingRequestedUseCase record, SimpMessagingTemplate socket,
                                    @Qualifier(GradingRequestedMessagingConfig.RETRY) MessageRetry retry) {
        this.mapper = mapper;
        this.record = record;
        this.socket = socket;
        this.retry = retry;
    }

    @RabbitListener(queues = GradingRequestedMessagingConfig.QUEUE,
            containerFactory = GradingRequestedMessagingConfig.CONTAINER_FACTORY)
    public void onGradingRequested(Message message) {
        GradingRequestedV1 event;
        try {
            event = mapper.readValue(message.getBody(), GradingRequestedV1.class);
        } catch (IOException | RuntimeException unreadable) {
            throw new AmqpRejectAndDontRequeueException("Unreadable grading requested event", unreadable);
        }
        try {
            record.execute(event, message.getMessageProperties().getReceivedRoutingKey(),
                    message.getMessageProperties().getMessageId()).ifPresent(this::push);
        } catch (IllegalArgumentException breaksContract) {
            throw new AmqpRejectAndDontRequeueException("Grading requested event breaks the contract", breaksContract);
        } catch (RuntimeException temporary) {
            log.warn("Grading requested notice will be retried: errorType={}", temporary.getClass().getSimpleName());
            retry.retryLater(message, temporary);
        }
    }

    private void push(Notification notice) {
        try {
            socket.convertAndSendToUser(notice.recipientId().toString(), "/queue/notifications", Map.of("id", notice.id(),
                    "actorId", notice.actorId(), "eventType", notice.eventType(),
                    "gradingJobId", notice.gradingJobId(), "createdAt", notice.createdAt().toString()));
        } catch (RuntimeException ignored) { /* The inbox over REST stays authoritative after a push failure. */ }
    }
}
