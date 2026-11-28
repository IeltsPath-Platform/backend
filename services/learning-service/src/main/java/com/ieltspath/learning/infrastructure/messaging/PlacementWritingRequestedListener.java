package com.ieltspath.learning.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.commonobservability.Correlation;
import com.ieltspath.commonobservability.OutboxTraceContext;
import com.ieltspath.learning.application.port.PlacementWritingJobStore;
import com.ieltspath.events.assessment.PlacementWritingGradingRequestedV1;
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
public class PlacementWritingRequestedListener {
    private static final Logger log = LoggerFactory.getLogger(PlacementWritingRequestedListener.class);
    private static final String FAILURE_HEADER = "x-learning-failure";
    private final ObjectMapper json;
    private final PlacementWritingJobStore jobs;
    private final OutboxTraceContext traceContext;
    private final int maxAttempts;
    private com.ieltspath.learning.domain.repository.DeletedUserRepository deletedUsers;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setDeletedUsers(com.ieltspath.learning.domain.repository.DeletedUserRepository deletedUsers) {
        this.deletedUsers = deletedUsers;
    }

    public PlacementWritingRequestedListener(ObjectMapper json, PlacementWritingJobStore jobs,
                                              OutboxTraceContext traceContext,
                                              @Value("${learning.placement-writing.max-delivery-attempts:5}") int maxAttempts) {
        this.json=json; this.jobs=jobs; this.traceContext=traceContext; this.maxAttempts=maxAttempts;
    }

    @RabbitListener(queues=PlacementWritingRequestedMessagingConfig.QUEUE,ackMode="MANUAL",
            autoStartup="${learning.placement-writing.consumer-enabled:true}")
    public void onMessage(Message message, Channel channel) throws IOException {
        long tag=message.getMessageProperties().getDeliveryTag();
        PlacementWritingGradingRequestedV1 event;
        try {
            event=json.readValue(message.getBody(),PlacementWritingGradingRequestedV1.class);
            validate(event);
        } catch(Exception invalid) { park(message,channel,"contract violation"); return; }
        try {
            if (deletedUsers != null && deletedUsers.isDeleted(event.data().userId())) {
                channel.basicAck(tag, false);
                return;
            }
            String payload=json.writeValueAsString(event);
            Object correlation=message.getMessageProperties().getHeaders().get(Correlation.HEADER);
            String correlationId=correlation instanceof String value ? value : null;
            String trace=traceContext.capture();
            jobs.enqueue(event.eventId(),event.data().attemptId(),event.data().userId(),payload,correlationId,trace);
            channel.basicAck(tag,false);
        } catch(Exception failure) {
            int attempt=previousFailures(message.getMessageProperties())+1;
            log.warn("Placement Writing request could not be stored: eventId={}, attempt={}, errorType={}",
                    event.eventId(),attempt,failure.getClass().getSimpleName());
            if(attempt>=maxAttempts) park(message,channel,"delivery attempts exhausted");
            else channel.basicNack(tag,false,false);
        }
    }

    private static void validate(PlacementWritingGradingRequestedV1 event) {
        if(event.eventId()==null || !PlacementWritingGradingRequestedV1.EVENT_TYPE.equals(event.eventType()) || event.data()==null
                || event.data().attemptId()==null || event.data().userId()==null || event.data().items()==null || event.data().items().isEmpty())
            throw new IllegalArgumentException("invalid event");
        var ids=new HashSet<UUID>();
        for(var item:event.data().items()) {
            if(item==null || item.attemptItemId()==null || !ids.add(item.attemptItemId()) || item.stem()==null
                    || item.task()==null || item.essayText()==null || item.essayText().isBlank())
                throw new IllegalArgumentException("invalid item");
        }
    }

    private static int previousFailures(MessageProperties properties) {
        List<Map<String,?>> deaths=properties.getXDeathHeader();
        if(deaths==null)return 0;
        return deaths.stream().filter(death->PlacementWritingRequestedMessagingConfig.QUEUE.equals(death.get("queue"))
                && "rejected".equals(death.get("reason"))).mapToInt(death->death.get("count") instanceof Number n?n.intValue():0).sum();
    }

    private static void park(Message message,Channel channel,String reason)throws IOException {
        MessageProperties source=message.getMessageProperties(); Map<String,Object> headers=new HashMap<>(source.getHeaders());
        headers.put(FAILURE_HEADER,reason);
        AMQP.BasicProperties props=new AMQP.BasicProperties.Builder().messageId(source.getMessageId()).type(source.getType())
                .contentType(source.getContentType()).deliveryMode(2).headers(headers).build();
        try { channel.confirmSelect(); channel.basicPublish(PlacementWritingRequestedMessagingConfig.DLX,
                PlacementWritingRequestedMessagingConfig.DLQ,true,props,message.getBody()); channel.waitForConfirmsOrDie(5000);
            channel.basicAck(source.getDeliveryTag(),false);
        } catch(IOException|java.util.concurrent.TimeoutException|InterruptedException failure) {
            if(failure instanceof InterruptedException)Thread.currentThread().interrupt(); channel.basicNack(source.getDeliveryTag(),false,false);
        }
    }
}
