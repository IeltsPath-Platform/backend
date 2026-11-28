package com.ieltspath.assessment.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.PlacementWritingGradingStore;
import com.ieltspath.assessment.application.usecase.PlacementGradingService;
import com.ieltspath.events.assessment.PlacementWritingGradingCompletedV1;
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

/** Applies one Writing result idempotently and acks only after the result transaction commits. */
@Component
public class PlacementWritingCompletedListener {
    private static final Logger log = LoggerFactory.getLogger(PlacementWritingCompletedListener.class);
    private static final String FAILURE_HEADER = "x-assessment-failure";
    private final ObjectMapper json;
    private final PlacementWritingGradingStore grades;
    private final PlacementGradingService grading;
    private final int maxAttempts;
    private com.ieltspath.assessment.application.port.DeletedUserStore deletedUsers;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setDeletedUsers(com.ieltspath.assessment.application.port.DeletedUserStore deletedUsers) {
        this.deletedUsers = deletedUsers;
    }

    public PlacementWritingCompletedListener(ObjectMapper json, PlacementWritingGradingStore grades,
                                              PlacementGradingService grading,
                                              @Value("${assessment.placement.writing-grading.max-delivery-attempts:5}") int maxAttempts) {
        this.json = json; this.grades = grades; this.grading = grading; this.maxAttempts = maxAttempts;
    }

    @RabbitListener(queues = PlacementWritingCompletedMessagingConfig.QUEUE, ackMode = "MANUAL",
            autoStartup = "${assessment.placement.writing-grading.consumer-enabled:true}")
    public void onMessage(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        PlacementWritingGradingCompletedV1 event;
        try {
            event = json.readValue(message.getBody(), PlacementWritingGradingCompletedV1.class);
            if (event.eventId() == null || !PlacementWritingGradingCompletedV1.EVENT_TYPE.equals(event.eventType())
                    || event.data() == null || event.data().attemptId() == null || event.data().userId() == null
                    || event.data().items().isEmpty()
                    || event.data().items().stream().anyMatch(item -> item.attemptItemId() == null
                            || !List.of("GRADED", "FAILED").contains(item.status())
                            || "GRADED".equals(item.status()) && item.band() == null)) {
                throw new IllegalArgumentException("invalid Writing grading contract");
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
            grades.record(event.eventId(), event.data(), java.time.Instant.now());
            grading.completeIfGraded(event.data().attemptId());
            channel.basicAck(tag, false);
        } catch (RuntimeException failure) {
            int attempt = previousFailures(message.getMessageProperties()) + 1;
            log.warn("Placement Writing result failed: eventId={}, attempt={}, errorType={}", event.eventId(), attempt,
                    failure.getClass().getSimpleName());
            if (attempt >= maxAttempts) park(message, channel, "delivery attempts exhausted");
            else channel.basicNack(tag, false, false);
        }
    }

    private static int previousFailures(MessageProperties properties) {
        List<Map<String, ?>> deaths = properties.getXDeathHeader();
        if (deaths == null) return 0;
        return deaths.stream().filter(death -> PlacementWritingCompletedMessagingConfig.QUEUE.equals(death.get("queue"))
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
            channel.basicPublish(PlacementWritingCompletedMessagingConfig.DLX, PlacementWritingCompletedMessagingConfig.DLQ,
                    true, props, message.getBody());
            channel.waitForConfirmsOrDie(5_000);
            channel.basicAck(source.getDeliveryTag(), false);
        } catch (IOException | java.util.concurrent.TimeoutException | InterruptedException failure) {
            if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
            channel.basicNack(source.getDeliveryTag(), false, false);
        }
    }
}
