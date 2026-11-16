package com.ieltspath.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.events.library.VocabularyReviewDueTopology;
import com.ieltspath.events.library.VocabularyReviewDueV1;
import com.ieltspath.events.messaging.MessageRetry;
import com.ieltspath.notification.application.RecordVocabularyReviewDueUseCase;
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
public class VocabularyReviewDueListener {
    private static final Logger log = LoggerFactory.getLogger(VocabularyReviewDueListener.class);
    private final ObjectMapper mapper;
    private final RecordVocabularyReviewDueUseCase record;
    private final NoticePushBridge noticePush;
    private final MessageRetry retry;

    public VocabularyReviewDueListener(ObjectMapper mapper, RecordVocabularyReviewDueUseCase record,
                                       NoticePushBridge noticePush,
                                       @Qualifier(VocabularyReviewDueMessagingConfig.RETRY) MessageRetry retry) {
        this.mapper = mapper;
        this.record = record;
        this.noticePush = noticePush;
        this.retry = retry;
    }

    @RabbitListener(queues = VocabularyReviewDueTopology.QUEUE,
            containerFactory = VocabularyReviewDueMessagingConfig.CONTAINER_FACTORY)
    public void onVocabularyReviewDue(Message message) {
        VocabularyReviewDueV1 event;
        try {
            event = mapper.readValue(message.getBody(), VocabularyReviewDueV1.class);
        } catch (IOException | RuntimeException malformed) {
            throw new AmqpRejectAndDontRequeueException("Unreadable vocabulary review due event", malformed);
        }
        try {
            record.execute(event, message.getMessageProperties().getReceivedRoutingKey(),
                    message.getMessageProperties().getMessageId()).ifPresent(this::push);
        } catch (IllegalArgumentException invalid) {
            throw new AmqpRejectAndDontRequeueException("Vocabulary review due event breaks its contract", invalid);
        } catch (RuntimeException temporary) {
            log.warn("Vocabulary review notice will be retried: errorType={}", temporary.getClass().getSimpleName());
            retry.retryLater(message, temporary);
        }
    }

    private void push(Notification notice) {
        noticePush.pushToUser(notice.recipientId(), Map.of("id", notice.id(), "actorId", notice.actorId(),
                "eventType", notice.eventType(), "createdAt", notice.createdAt().toString()));
    }
}
