package com.ieltspath.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.events.access.SubscriptionExpiryNoticeV1;
import com.ieltspath.events.messaging.MessageRetry;
import com.ieltspath.notification.application.RecordSubscriptionExpiryNoticeUseCase;
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

@Component
public class SubscriptionExpiryInboxListener {
    private static final Logger log = LoggerFactory.getLogger(SubscriptionExpiryInboxListener.class);
    private final ObjectMapper mapper;
    private final RecordSubscriptionExpiryNoticeUseCase record;
    private final NoticePushBridge noticePush;
    private final MessageRetry retry;

    public SubscriptionExpiryInboxListener(ObjectMapper mapper, RecordSubscriptionExpiryNoticeUseCase record,
                                           NoticePushBridge noticePush,
                                           @Qualifier(SubscriptionExpiryInboxMessagingConfig.RETRY) MessageRetry retry) {
        this.mapper = mapper;
        this.record = record;
        this.noticePush = noticePush;
        this.retry = retry;
    }

    @RabbitListener(queues = SubscriptionExpiryInboxTopology.QUEUE,
            containerFactory = SubscriptionExpiryInboxMessagingConfig.CONTAINER_FACTORY)
    public void onSubscriptionExpiryNotice(Message message) {
        SubscriptionExpiryNoticeV1 event;
        try {
            event = mapper.readValue(message.getBody(), SubscriptionExpiryNoticeV1.class);
        } catch (IOException | RuntimeException malformed) {
            throw new AmqpRejectAndDontRequeueException("Unreadable subscription expiry notice", malformed);
        }
        try {
            record.execute(event, message.getMessageProperties().getReceivedRoutingKey(),
                    message.getMessageProperties().getMessageId()).ifPresent(this::push);
        } catch (IllegalArgumentException invalid) {
            throw new AmqpRejectAndDontRequeueException("Subscription expiry notice breaks its contract", invalid);
        } catch (RuntimeException temporary) {
            log.warn("Subscription expiry inbox notice will be retried: errorType={}", temporary.getClass().getSimpleName());
            retry.retryLater(message, temporary);
        }
    }

    private void push(Notification notice) {
        noticePush.pushToUser(notice.recipientId(), Map.of("id", notice.id(), "actorId", notice.actorId(),
                "eventType", notice.eventType(), "createdAt", notice.createdAt().toString()));
    }
}
