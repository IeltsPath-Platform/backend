package com.ieltspath.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.events.messaging.MessageRetry;
import com.ieltspath.events.user.PasswordResetRequestedV1;
import com.ieltspath.events.user.PasswordResetRequestedTopology;
import com.ieltspath.notification.application.SendPasswordResetMailUseCase;
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
public class PasswordResetRequestedListener {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetRequestedListener.class);
    private final ObjectMapper mapper;
    private final SendPasswordResetMailUseCase send;
    private final MessageRetry retry;
    public PasswordResetRequestedListener(ObjectMapper mapper, SendPasswordResetMailUseCase send,
                                          @Qualifier(PasswordResetRequestedMessagingConfig.RETRY) MessageRetry retry) {
        this.mapper = mapper; this.send = send; this.retry = retry;
    }
    @RabbitListener(queues = PasswordResetRequestedTopology.QUEUE,
            containerFactory = PasswordResetRequestedMessagingConfig.CONTAINER_FACTORY)
    public void onPasswordResetRequested(Message message) {
        PasswordResetRequestedV1 event;
        try { event = mapper.readValue(message.getBody(), PasswordResetRequestedV1.class); }
        catch (IOException | RuntimeException malformed) {
            throw new AmqpRejectAndDontRequeueException("Unreadable password reset event", malformed);
        }
        try { send.execute(event); }
        catch (IllegalArgumentException invalidContract) {
            throw new AmqpRejectAndDontRequeueException("Password reset event breaks its contract", invalidContract);
        } catch (RuntimeException temporary) {
            log.warn("Password reset mail will be retried eventId={} errorType={}", event.eventId(),
                    temporary.getClass().getSimpleName());
            retry.retryLater(message, temporary);
        }
    }
}
