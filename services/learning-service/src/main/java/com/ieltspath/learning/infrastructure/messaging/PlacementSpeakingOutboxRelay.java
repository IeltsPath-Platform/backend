package com.ieltspath.learning.infrastructure.messaging;

import com.ieltspath.commonobservability.Correlation;
import com.ieltspath.commonobservability.OutboxTraceContext;
import com.ieltspath.events.assessment.AssessmentMessaging;
import com.ieltspath.events.assessment.SpeakingGradingCompletedV1;
import com.ieltspath.learning.application.port.PlacementSpeakingJobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Relays committed grade events with broker confirmation; retries retain the same event id. */
@Component
public class PlacementSpeakingOutboxRelay {
    private static final Logger log=LoggerFactory.getLogger(PlacementSpeakingOutboxRelay.class);
    private final PlacementSpeakingJobStore jobs;
    private final RabbitTemplate rabbit;
    private final OutboxTraceContext traceContext;
    private final Duration timeout;

    public PlacementSpeakingOutboxRelay(PlacementSpeakingJobStore jobs,RabbitTemplate rabbit,
                                        OutboxTraceContext traceContext,
                                        @Value("${learning.speaking-grading.confirm-timeout:PT5S}") Duration timeout) {
        this.jobs=jobs;this.rabbit=rabbit;this.traceContext=traceContext;this.timeout=timeout;
    }

    @Scheduled(fixedDelayString="${learning.speaking-grading.outbox-poll-ms:1000}")
    @Transactional
    public void publishPending() {
        for(var event:jobs.claimOutbox(50)) {
            OutboxTraceContext.Scope scope=traceContext.openRelay(event.traceContext());
            try {
                // Placement Writing results leave through the same outbox.
                String routingKey=SpeakingGradingCompletedV1.EVENT_TYPE.equals(event.eventType())
                        ? AssessmentMessaging.SPEAKING_GRADING_COMPLETED_ROUTING_KEY
                        : com.ieltspath.events.assessment.PlacementWritingGradingCompletedV1.EVENT_TYPE.equals(event.eventType())
                        ? AssessmentMessaging.PLACEMENT_WRITING_COMPLETED_ROUTING_KEY : null;
                if(routingKey==null)throw new IllegalStateException("Unknown placement Speaking event type");
                var builder=MessageBuilder.withBody(event.payload().getBytes(StandardCharsets.UTF_8))
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON).setContentEncoding("UTF-8")
                        .setDeliveryMode(MessageDeliveryMode.PERSISTENT).setMessageId(event.id().toString())
                        .setType(event.eventType()).setTimestamp(Date.from(event.createdAt()));
                if(event.correlationId()!=null)builder.setHeader(Correlation.HEADER,event.correlationId());
                Message message=builder.build();
                CorrelationData confirmation=new CorrelationData(event.id().toString());
                rabbit.send(AssessmentMessaging.EXCHANGE,routingKey,message,confirmation);
                CorrelationData.Confirm confirm=confirmation.getFuture().get(timeout.toMillis(),TimeUnit.MILLISECONDS);
                if(!confirm.isAck()||confirmation.getReturned()!=null)throw new IllegalStateException("Broker did not route placement Speaking event");
                jobs.markPublished(event.id());
            } catch(InterruptedException interrupted) {
                Thread.currentThread().interrupt(); jobs.recordPublishFailure(event.id(),"interrupted");
            } catch(ExecutionException|TimeoutException|RuntimeException failure) {
                jobs.recordPublishFailure(event.id(),failure.getClass().getSimpleName());
                log.warn("Placement Speaking outbox event was not published: eventId={}, errorType={}",event.id(),failure.getClass().getSimpleName());
            } finally { scope.close(); }
        }
    }
}
