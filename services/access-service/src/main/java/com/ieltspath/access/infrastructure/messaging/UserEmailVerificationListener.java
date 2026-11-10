package com.ieltspath.access.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.access.application.usecase.RecordUserEmailVerificationUseCase;
import com.ieltspath.events.messaging.MessageRetry;
import com.ieltspath.events.user.UserEmailVerificationStateTopology;
import com.ieltspath.events.user.UserEmailVerificationStateV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Keeps the per-account email-verification state that the activation guard reads. */
@Component
public class UserEmailVerificationListener {
    private static final Logger log = LoggerFactory.getLogger(UserEmailVerificationListener.class);

    private final ObjectMapper objectMapper;
    private final RecordUserEmailVerificationUseCase record;
    private final MessageRetry retry;

    public UserEmailVerificationListener(ObjectMapper objectMapper, RecordUserEmailVerificationUseCase record,
                                         @Qualifier(UserEmailVerificationMessagingConfig.RETRY) MessageRetry retry) {
        this.objectMapper = objectMapper;
        this.record = record;
        this.retry = retry;
    }

    @RabbitListener(queues = UserEmailVerificationStateTopology.QUEUE,
            containerFactory = UserEmailVerificationMessagingConfig.CONTAINER_FACTORY)
    public void onEmailVerificationState(Message message) {
        UserEmailVerificationStateV1 event;
        try {
            event = objectMapper.readValue(message.getBody(), UserEmailVerificationStateV1.class);
        } catch (IOException | RuntimeException malformed) {
            throw new AmqpRejectAndDontRequeueException("Unreadable email verification state event", malformed);
        }
        try {
            record.execute(event);
        } catch (IllegalArgumentException invalidContract) {
            throw new AmqpRejectAndDontRequeueException("Email verification state breaks its contract", invalidContract);
        } catch (RuntimeException temporary) {
            log.warn("Email verification state will be retried errorType={}", temporary.getClass().getSimpleName());
            retry.retryLater(message, temporary);
        }
    }
}
