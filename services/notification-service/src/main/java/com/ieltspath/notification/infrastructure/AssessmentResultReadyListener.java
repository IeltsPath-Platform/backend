package com.ieltspath.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.events.assessment.AssessmentCompletedV2;
import com.ieltspath.events.messaging.MessageRetry;
import com.ieltspath.notification.application.RecordAssessmentResultReadyUseCase;
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
public class AssessmentResultReadyListener {
    private static final Logger log = LoggerFactory.getLogger(AssessmentResultReadyListener.class);
    private final ObjectMapper mapper;
    private final RecordAssessmentResultReadyUseCase record;
    private final NoticePushBridge noticePush;
    private final MessageRetry retry;

    public AssessmentResultReadyListener(ObjectMapper mapper, RecordAssessmentResultReadyUseCase record,
                                         NoticePushBridge noticePush,
                                         @Qualifier(AssessmentResultReadyMessagingConfig.RETRY) MessageRetry retry) {
        this.mapper = mapper;
        this.record = record;
        this.noticePush = noticePush;
        this.retry = retry;
    }

    @RabbitListener(queues = AssessmentResultReadyTopology.QUEUE,
            containerFactory = AssessmentResultReadyMessagingConfig.CONTAINER_FACTORY)
    public void onAssessmentCompleted(Message message) {
        AssessmentCompletedV2 event;
        try {
            event = mapper.readValue(message.getBody(), AssessmentCompletedV2.class);
        } catch (IOException | RuntimeException malformed) {
            throw new AmqpRejectAndDontRequeueException("Unreadable assessment completed event", malformed);
        }
        try {
            record.execute(event, message.getMessageProperties().getReceivedRoutingKey(),
                    message.getMessageProperties().getMessageId()).ifPresent(this::push);
        } catch (IllegalArgumentException invalid) {
            throw new AmqpRejectAndDontRequeueException("Assessment completed event breaks its contract", invalid);
        } catch (RuntimeException temporary) {
            log.warn("Assessment result notice will be retried: errorType={}", temporary.getClass().getSimpleName());
            retry.retryLater(message, temporary);
        }
    }

    private void push(Notification notice) {
        noticePush.pushToUser(notice.recipientId(), Map.of("id", notice.id(), "actorId", notice.actorId(),
                "eventType", notice.eventType(), "createdAt", notice.createdAt().toString()));
    }
}
