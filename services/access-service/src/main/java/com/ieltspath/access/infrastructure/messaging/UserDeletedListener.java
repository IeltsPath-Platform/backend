package com.ieltspath.access.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.access.application.usecase.RecordUserDeletionUseCase;
import com.ieltspath.events.user.UserDeletedV1;
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

@Component
public class UserDeletedListener {
    private static final Logger log = LoggerFactory.getLogger(UserDeletedListener.class);
    private static final String FAILURE_HEADER = "x-access-failure";

    private final ObjectMapper json;
    private final RecordUserDeletionUseCase recordDeletion;
    private final int maxAttempts;

    public UserDeletedListener(ObjectMapper json, RecordUserDeletionUseCase recordDeletion,
                               @Value("${access.messaging.max-delivery-attempts:5}") int maxAttempts) {
        this.json = json;
        this.recordDeletion = recordDeletion;
        this.maxAttempts = maxAttempts;
    }

    @RabbitListener(queues = UserDeletionMessagingConfig.QUEUE, ackMode = "MANUAL",
            autoStartup = "${access.messaging.consumer-enabled:true}")
    public void onMessage(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        UserDeletedV1 event;
        try {
            event = json.readValue(message.getBody(), UserDeletedV1.class);
            if (event.eventId() == null || !UserDeletedV1.EVENT_TYPE.equals(event.eventType())
                    || event.userId() == null || event.occurredAt() == null) {
                throw new IllegalArgumentException("invalid user.deleted.v1 event");
            }
        } catch (Exception invalid) {
            park(message, channel, "contract violation");
            return;
        }
        try {
            recordDeletion.execute(event.eventId(), event.userId(), event.occurredAt());
            channel.basicAck(tag, false);
        } catch (RuntimeException failure) {
            int attempt = previousFailures(message.getMessageProperties()) + 1;
            log.warn("User deletion event failed: eventId={}, attempt={}, errorType={}",
                    event.eventId(), attempt, failure.getClass().getSimpleName());
            if (attempt >= maxAttempts) {
                park(message, channel, "exhausted after " + attempt + " attempts: " + failure.getClass().getSimpleName());
            } else {
                channel.basicNack(tag, false, false);
            }
        }
    }

    private static int previousFailures(MessageProperties properties) {
        List<Map<String, ?>> deaths = properties.getXDeathHeader();
        if (deaths == null) return 0;
        return deaths.stream()
                .filter(death -> UserDeletionMessagingConfig.QUEUE.equals(death.get("queue"))
                        && "rejected".equals(death.get("reason")))
                .mapToInt(death -> death.get("count") instanceof Number count ? count.intValue() : 0)
                .sum();
    }

    private static void park(Message message, Channel channel, String reason) throws IOException {
        MessageProperties source = message.getMessageProperties();
        Map<String, Object> headers = new HashMap<>(source.getHeaders());
        headers.put(FAILURE_HEADER, reason);
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .messageId(source.getMessageId())
                .type(source.getType())
                .contentType(source.getContentType())
                .contentEncoding(source.getContentEncoding())
                .deliveryMode(2)
                .headers(headers)
                .build();
        try {
            channel.confirmSelect();
            channel.basicPublish(UserDeletionMessagingConfig.DEAD_LETTER_EXCHANGE,
                    UserDeletionMessagingConfig.DEAD_LETTER_QUEUE, true, properties, message.getBody());
            channel.waitForConfirmsOrDie(5_000);
            channel.basicAck(source.getDeliveryTag(), false);
        } catch (IOException | java.util.concurrent.TimeoutException | InterruptedException failure) {
            if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
            channel.basicNack(source.getDeliveryTag(), false, false);
        }
    }
}
