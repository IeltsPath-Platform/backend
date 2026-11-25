package com.ieltspath.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.events.messaging.MessageRetry;
import com.ieltspath.events.user.PasswordResetGoogleOnlyTopology;
import com.ieltspath.events.user.PasswordResetGoogleOnlyV1;
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
public class PasswordResetGoogleOnlyListener {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetGoogleOnlyListener.class);

    private final ObjectMapper mapper;
    private final SendAccountNoticeMailUseCase send;
    private final MessageRetry retry;

    public PasswordResetGoogleOnlyListener(ObjectMapper mapper, SendAccountNoticeMailUseCase send,
                                       @Qualifier(PasswordResetGoogleOnlyMessagingConfig.RETRY) MessageRetry retry) {
        this.mapper = mapper;
        this.send = send;
        this.retry = retry;
    }

    @RabbitListener(queues = PasswordResetGoogleOnlyTopology.QUEUE,
            containerFactory = PasswordResetGoogleOnlyMessagingConfig.CONTAINER_FACTORY)
    public void onPasswordResetGoogleOnly(Message message) {
        PasswordResetGoogleOnlyV1 event;
        try {
            event = mapper.readValue(message.getBody(), PasswordResetGoogleOnlyV1.class);
        } catch (IOException | RuntimeException malformed) {
            throw new AmqpRejectAndDontRequeueException("Unreadable Password reset Google-only event", malformed);
        }
        try {
            send.passwordResetGoogleOnly(event);
        } catch (IllegalArgumentException invalidContract) {
            throw new AmqpRejectAndDontRequeueException("Password reset Google-only event breaks its contract", invalidContract);
        } catch (RuntimeException temporary) {
            log.warn("Password reset Google-only mail will be retried eventId={} errorType={}", event.eventId(),
                    temporary.getClass().getSimpleName());
            retry.retryLater(message, temporary);
        }
    }
}
