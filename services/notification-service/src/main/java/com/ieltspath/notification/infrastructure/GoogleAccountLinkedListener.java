package com.ieltspath.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.events.messaging.MessageRetry;
import com.ieltspath.events.user.GoogleAccountLinkedTopology;
import com.ieltspath.events.user.GoogleAccountLinkedV1;
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
public class GoogleAccountLinkedListener {
    private static final Logger log = LoggerFactory.getLogger(GoogleAccountLinkedListener.class);

    private final ObjectMapper mapper;
    private final SendAccountNoticeMailUseCase send;
    private final MessageRetry retry;

    public GoogleAccountLinkedListener(ObjectMapper mapper, SendAccountNoticeMailUseCase send,
                                       @Qualifier(GoogleAccountLinkedMessagingConfig.RETRY) MessageRetry retry) {
        this.mapper = mapper;
        this.send = send;
        this.retry = retry;
    }

    @RabbitListener(queues = GoogleAccountLinkedTopology.QUEUE,
            containerFactory = GoogleAccountLinkedMessagingConfig.CONTAINER_FACTORY)
    public void onGoogleAccountLinked(Message message) {
        GoogleAccountLinkedV1 event;
        try {
            event = mapper.readValue(message.getBody(), GoogleAccountLinkedV1.class);
        } catch (IOException | RuntimeException malformed) {
            throw new AmqpRejectAndDontRequeueException("Unreadable Google account linked event", malformed);
        }
        try {
            send.googleAccountLinked(event);
        } catch (IllegalArgumentException invalidContract) {
            throw new AmqpRejectAndDontRequeueException("Google account linked event breaks its contract", invalidContract);
        } catch (RuntimeException temporary) {
            log.warn("Google linked mail will be retried eventId={} errorType={}", event.eventId(),
                    temporary.getClass().getSimpleName());
            retry.retryLater(message, temporary);
        }
    }
}
