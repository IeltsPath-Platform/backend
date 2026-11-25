package com.ieltspath.learning.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.commonobservability.Correlation;
import com.ieltspath.commonobservability.OutboxTraceContext;
import com.ieltspath.learning.application.port.PlacementSpeakingJobStore;
import com.ieltspath.events.assessment.SpeakingGradingRequestedV1;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Validates and persists each request before acknowledging it; duplicate events are harmless. */
@Component
public class PlacementSpeakingRequestedListener {
    private static final Logger log = LoggerFactory.getLogger(PlacementSpeakingRequestedListener.class);
    private static final String FAILURE_HEADER = "x-learning-failure";
    private final ObjectMapper json;
    private final PlacementSpeakingJobStore jobs;
    private final OutboxTraceContext traceContext;
    private final int maxAttempts;

    public PlacementSpeakingRequestedListener(ObjectMapper json, PlacementSpeakingJobStore jobs,
                                              OutboxTraceContext traceContext,
                                              @Value("${learning.speaking-grading.max-delivery-attempts:5}") int maxAttempts) {
        this.json=json; this.jobs=jobs; this.traceContext=traceContext; this.maxAttempts=maxAttempts;
    }

    @RabbitListener(queues=PlacementSpeakingRequestedMessagingConfig.QUEUE,ackMode="MANUAL",
            autoStartup="${learning.speaking-grading.consumer-enabled:true}")
    public void onMessage(Message message, Channel channel) throws IOException {
        long tag=message.getMessageProperties().getDeliveryTag();
        SpeakingGradingRequestedV1 event;
        try {
            event=json.readValue(message.getBody(),SpeakingGradingRequestedV1.class);
            validate(event);
        } catch(Exception invalid) { park(message,channel,"contract violation"); return; }
        try {
            String payload=json.writeValueAsString(event);
            Object correlation=message.getMessageProperties().getHeaders().get(Correlation.HEADER);
            String correlationId=correlation instanceof String value ? value : null;
            String trace=traceContext.capture();
            jobs.enqueue(event.eventId(),event.data().attemptId(),event.data().userId(),payload,correlationId,trace);
            channel.basicAck(tag,false);
        } catch(Exception failure) {
            int attempt=previousFailures(message.getMessageProperties())+1;
            log.warn("Placement Speaking request could not be stored: eventId={}, attempt={}, errorType={}",
                    event.eventId(),attempt,failure.getClass().getSimpleName());
            if(attempt>=maxAttempts) park(message,channel,"delivery attempts exhausted");
            else channel.basicNack(tag,false,false);
        }
    }

    private static void validate(SpeakingGradingRequestedV1 event) {
        if(event.eventId()==null || !SpeakingGradingRequestedV1.EVENT_TYPE.equals(event.eventType()) || event.data()==null
                || event.data().attemptId()==null || event.data().userId()==null || event.data().items()==null || event.data().items().isEmpty())
            throw new IllegalArgumentException("invalid event");
        var ids=new HashSet<UUID>();
        for(var item:event.data().items()) {
            if(item==null || item.attemptItemId()==null || !ids.add(item.attemptItemId()) || item.partNo()<1
                    || item.questionNo()<1 || item.question()==null || item.objectKey()==null
                    || !item.objectKey().startsWith("speaking/"+event.data().userId()+"/"))
                throw new IllegalArgumentException("invalid item");
        }
    }

    private static int previousFailures(MessageProperties properties) {
        List<Map<String,?>> deaths=properties.getXDeathHeader();
        if(deaths==null)return 0;
        return deaths.stream().filter(death->PlacementSpeakingRequestedMessagingConfig.QUEUE.equals(death.get("queue"))
                && "rejected".equals(death.get("reason"))).mapToInt(death->death.get("count") instanceof Number n?n.intValue():0).sum();
    }

    private static void park(Message message,Channel channel,String reason)throws IOException {
        MessageProperties source=message.getMessageProperties(); Map<String,Object> headers=new HashMap<>(source.getHeaders());
        headers.put(FAILURE_HEADER,reason);
        AMQP.BasicProperties props=new AMQP.BasicProperties.Builder().messageId(source.getMessageId()).type(source.getType())
                .contentType(source.getContentType()).deliveryMode(2).headers(headers).build();
        try { channel.confirmSelect(); channel.basicPublish(PlacementSpeakingRequestedMessagingConfig.DLX,
                PlacementSpeakingRequestedMessagingConfig.DLQ,true,props,message.getBody()); channel.waitForConfirmsOrDie(5000);
            channel.basicAck(source.getDeliveryTag(),false);
        } catch(IOException|java.util.concurrent.TimeoutException|InterruptedException failure) {
            if(failure instanceof InterruptedException)Thread.currentThread().interrupt(); channel.basicNack(source.getDeliveryTag(),false,false);
        }
    }
}
