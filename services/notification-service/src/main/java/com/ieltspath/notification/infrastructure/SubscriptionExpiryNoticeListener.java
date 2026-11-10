package com.ieltspath.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.events.access.SubscriptionExpiryNoticeTopology;
import com.ieltspath.events.access.SubscriptionExpiryNoticeV1;
import com.ieltspath.events.messaging.MessageRetry;
import com.ieltspath.notification.application.SendAccountNoticeMailUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@ConditionalOnExpression("${notification.mail.enabled:false} and '${spring.mail.host:}' != '' and '${notification.mail.from:}' != '' and '${notification.mail.frontend-base-url:}' != ''")
public class SubscriptionExpiryNoticeListener {
    private static final Logger log = LoggerFactory.getLogger(SubscriptionExpiryNoticeListener.class);

    private final ObjectMapper mapper;
    private final SendAccountNoticeMailUseCase send;
    private final MessageRetry retry;

    public SubscriptionExpiryNoticeListener(ObjectMapper mapper, SendAccountNoticeMailUseCase send,
                                            @Qualifier(SubscriptionExpiryNoticeMessagingConfig.RETRY) MessageRetry retry) {
        this.mapper = mapper;
        this.send = send;
        this.retry = retry;
    }

    @RabbitListener(queues = SubscriptionExpiryNoticeTopology.QUEUE,
            containerFactory = SubscriptionExpiryNoticeMessagingConfig.CONTAINER_FACTORY)
    public void onSubscriptionExpiryNotice(Message message) {
        SubscriptionExpiryNoticeV1 event;
        try {
            event = mapper.readValue(message.getBody(), SubscriptionExpiryNoticeV1.class);
        } catch (IOException | RuntimeException malformed) {
            throw new AmqpRejectAndDontRequeueException("Unreadable subscription expiry notice", malformed);
        }
        try {
            send.subscriptionExpiry(event);
        } catch (IllegalArgumentException invalidContract) {
            throw new AmqpRejectAndDontRequeueException("Subscription expiry notice breaks its contract", invalidContract);
        } catch (RuntimeException temporary) {
            log.warn("Subscription expiry mail will be retried eventId={} errorType={}", event.eventId(),
                    temporary.getClass().getSimpleName());
            retry.retryLater(message, temporary);
        }
    }
}
