package com.ieltspath.assessment.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.PlacementSpeakingGradeStore;
import com.ieltspath.assessment.application.usecase.PlacementGradingService;
import com.ieltspath.events.assessment.SpeakingGradingCompletedV1;
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
import java.util.List;
import java.util.Map;

/** Applies one Speaking result idempotently and acks only after the result transaction commits. */
@Component
public class PlacementSpeakingCompletedListener {
    private static final Logger log = LoggerFactory.getLogger(PlacementSpeakingCompletedListener.class);
    private static final String FAILURE_HEADER = "x-assessment-failure";
    private final ObjectMapper json;
    private final PlacementSpeakingGradeStore grades;
    private final PlacementGradingService grading;
    private final int maxAttempts;
    private com.ieltspath.assessment.application.port.DeletedUserStore deletedUsers;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setDeletedUsers(com.ieltspath.assessment.application.port.DeletedUserStore deletedUsers) {
        this.deletedUsers = deletedUsers;
    }

    public PlacementSpeakingCompletedListener(ObjectMapper json, PlacementSpeakingGradeStore grades,
                                              PlacementGradingService grading,
                                              @Value("${assessment.placement.speaking-grading.max-delivery-attempts:5}") int maxAttempts) {
        this.json = json; this.grades = grades; this.grading = grading; this.maxAttempts = maxAttempts;
    }

    @RabbitListener(queues = PlacementSpeakingCompletedMessagingConfig.QUEUE, ackMode = "MANUAL",
            autoStartup = "${assessment.placement.speaking-grading.consumer-enabled:true}")
    public void onMessage(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        SpeakingGradingCompletedV1 event;
        try {
            event = json.readValue(message.getBody(), SpeakingGradingCompletedV1.class);
            if (event.eventId() == null || !SpeakingGradingCompletedV1.EVENT_TYPE.equals(event.eventType())
                    || event.data() == null || event.data().attemptId() == null || event.data().userId() == null
                    || !List.of("GRADED", "FAILED").contains(event.data().status())
                    || ("GRADED".equals(event.data().status()) && event.data().band() == null)) {
                throw new IllegalArgumentException("invalid Speaking grading contract");
            }
        } catch (Exception invalid) {
            park(message, channel, "contract violation");
            return;
        }
        try {
            if (deletedUsers != null && deletedUsers.isDeleted(event.data().userId())) {
                channel.basicAck(tag, false);
                return;
            }
            grades.complete(event);
            grading.completeIfGraded(event.data().attemptId());
            channel.basicAck(tag, false);
        } catch (RuntimeException failure) {
            int attempt = previousFailures(message.getMessageProperties()) + 1;
            log.warn("Placement Speaking result failed: eventId={}, attempt={}, errorType={}", event.eventId(), attempt,
                    failure.getClass().getSimpleName());
            if (attempt >= maxAttempts) park(message, channel, "delivery attempts exhausted");
            else channel.basicNack(tag, false, false);
        }
    }

    private static int previousFailures(MessageProperties properties) {
        List<Map<String, ?>> deaths = properties.getXDeathHeader();
        if (deaths == null) return 0;
        return deaths.stream().filter(death -> PlacementSpeakingCompletedMessagingConfig.QUEUE.equals(death.get("queue"))
                && "rejected".equals(death.get("reason")))
                .mapToInt(death -> death.get("count") instanceof Number n ? n.intValue() : 0).sum();
    }

    private static void park(Message message, Channel channel, String reason) throws IOException {
        MessageProperties source = message.getMessageProperties();
        Map<String, Object> headers = new HashMap<>(source.getHeaders()); headers.put(FAILURE_HEADER, reason);
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder().messageId(source.getMessageId())
                .type(source.getType()).contentType(source.getContentType()).deliveryMode(2).headers(headers).build();
        try {
            channel.confirmSelect();
            channel.basicPublish(PlacementSpeakingCompletedMessagingConfig.DLX, PlacementSpeakingCompletedMessagingConfig.DLQ,
                    true, props, message.getBody());
            channel.waitForConfirmsOrDie(5_000);
            channel.basicAck(source.getDeliveryTag(), false);
        } catch (IOException | java.util.concurrent.TimeoutException | InterruptedException failure) {
            if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
            channel.basicNack(source.getDeliveryTag(), false, false);
        }
    }
}
