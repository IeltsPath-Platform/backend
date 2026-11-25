package com.ieltspath.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.events.messaging.MessageRetry;
import com.ieltspath.events.user.GoogleAccountUnlinkedTopology;
import com.ieltspath.events.user.GoogleAccountUnlinkedV1;
import com.ieltspath.notification.application.SendAccountNoticeMailUseCase;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Component
@ConditionalOnExpression("${notification.mail.enabled:false} and '${spring.mail.host:}' != '' and '${notification.mail.from:}' != '' and '${notification.mail.frontend-base-url:}' != ''")
public class GoogleAccountUnlinkedListener {
    private static final Logger log = LoggerFactory.getLogger(GoogleAccountUnlinkedListener.class);
    private final ObjectMapper mapper;
    private final SendAccountNoticeMailUseCase send;
    private final MessageRetry retry;

    public GoogleAccountUnlinkedListener(ObjectMapper mapper, SendAccountNoticeMailUseCase send,
            @Qualifier(GoogleAccountUnlinkedMessagingConfig.RETRY) MessageRetry retry) {
        this.mapper = mapper;
        this.send = send;
        this.retry = retry;
    }

    @RabbitListener(queues = GoogleAccountUnlinkedTopology.QUEUE,
            containerFactory = GoogleAccountUnlinkedMessagingConfig.CONTAINER_FACTORY)
    public void onGoogleAccountUnlinked(Message message) {
        GoogleAccountUnlinkedV1 event;
        try {
            event = mapper.readValue(message.getBody(), GoogleAccountUnlinkedV1.class);
        } catch (IOException | RuntimeException malformed) {
            throw new AmqpRejectAndDontRequeueException("Unreadable Google account unlinked event", malformed);
        }
        try {
            send.googleAccountUnlinked(event);
        } catch (IllegalArgumentException invalidContract) {
            throw new AmqpRejectAndDontRequeueException("Google account unlinked event breaks its contract", invalidContract);
        } catch (RuntimeException temporary) {
            log.warn("Google unlink notice will be retried eventId={} errorType={}", event.eventId(),
                    temporary.getClass().getSimpleName());
            retry.retryLater(message, temporary);
        }
    }
}
