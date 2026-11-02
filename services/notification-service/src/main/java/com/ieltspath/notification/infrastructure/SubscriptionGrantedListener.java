package com.ieltspath.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.events.access.SubscriptionGrantedV1;
import com.ieltspath.events.messaging.MessageRetry;
import com.ieltspath.notification.application.RecordSubscriptionGrantedUseCase;
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
public class SubscriptionGrantedListener {
    private static final Logger log = LoggerFactory.getLogger(SubscriptionGrantedListener.class);

    private final ObjectMapper mapper;
    private final RecordSubscriptionGrantedUseCase record;
    private final NoticePushBridge noticePush;
    private final MessageRetry retry;

    public SubscriptionGrantedListener(ObjectMapper mapper, RecordSubscriptionGrantedUseCase record,
                                       NoticePushBridge noticePush,
                                       @Qualifier(SubscriptionGrantedMessagingConfig.RETRY) MessageRetry retry) {
        this.mapper = mapper;
        this.record = record;
        this.noticePush = noticePush;
        this.retry = retry;
    }

    @RabbitListener(queues = SubscriptionGrantedMessagingConfig.QUEUE,
            containerFactory = SubscriptionGrantedMessagingConfig.CONTAINER_FACTORY)
    public void onSubscriptionGranted(Message message) {
        SubscriptionGrantedV1 event;
        try {
            event = mapper.readValue(message.getBody(), SubscriptionGrantedV1.class);
        } catch (IOException | RuntimeException unreadable) {
            throw new AmqpRejectAndDontRequeueException("Unreadable subscription granted event", unreadable);
        }
        try {
            record.execute(event, message.getMessageProperties().getReceivedRoutingKey(),
                    message.getMessageProperties().getMessageId()).ifPresent(this::push);
        } catch (IllegalArgumentException breaksContract) {
            throw new AmqpRejectAndDontRequeueException("Subscription granted event breaks the contract", breaksContract);
        } catch (RuntimeException temporary) {
            log.warn("Subscription granted notice will be retried: errorType={}", temporary.getClass().getSimpleName());
            retry.retryLater(message, temporary);
        }
    }

    private void push(Notification notice) {
        noticePush.pushToUser(notice.recipientId(), Map.of("id", notice.id(),
                "actorId", notice.actorId(), "eventType", notice.eventType(),
                "createdAt", notice.createdAt().toString()));
    }
}
