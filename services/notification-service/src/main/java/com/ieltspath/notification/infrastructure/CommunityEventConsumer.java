package com.ieltspath.notification.infrastructure;

import com.ieltspath.events.community.CommunityEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.notification.application.RecordCommunityEventUseCase;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** AMQP adapter: decodes a community event, hands it to the use case, then acks, parks or requeues the message. */
@Component
public class CommunityEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(CommunityEventConsumer.class);
    private static final long REQUEUE_PAUSE_MILLIS = 2000;
    private static final int MAX_RETRIES = 2;
    private final ObjectMapper mapper;
    private final RecordCommunityEventUseCase recordEvent;
    private final SimpMessagingTemplate socket;
    private final RabbitTemplate rabbit;

    public CommunityEventConsumer(ObjectMapper mapper, RecordCommunityEventUseCase recordEvent,
                                  SimpMessagingTemplate socket, RabbitTemplate rabbit) {
        this.mapper = mapper;
        this.recordEvent = recordEvent;
        this.socket = socket;
        this.rabbit = rabbit;
    }

    @RabbitListener(queues = "community.notifications.v1", ackMode = "MANUAL")
    public void receive(Message message, Channel channel) throws Exception {
        long tag = message.getMessageProperties().getDeliveryTag();
        try {
            var stored = recordEvent.execute(decode(message), message.getMessageProperties().getReceivedRoutingKey(),
                    message.getMessageProperties().getMessageId());
            channel.basicAck(tag, false);
            stored.ifPresent(n -> push(n.recipientId(), n.id(), n.actorId(), n.eventType(), n.postId(), n.createdAt()));
        } catch (IllegalArgumentException | com.fasterxml.jackson.core.JsonProcessingException bad) {
            park(channel, tag, "community.events.dlx", "community.notifications.v1.dlq", message);
        } catch (Exception transientFailure) {
            Object attemptsValue = message.getMessageProperties().getHeaders().get("community-retry-count");
            int attempts = attemptsValue instanceof Number n ? n.intValue() : 0;
            if (attempts >= MAX_RETRIES) park(channel, tag, "community.events.dlx", "community.notifications.v1.dlq", message);
            else {
                message.getMessageProperties().setHeader("community-retry-count", attempts + 1);
                park(channel, tag, "community.events.retry", "retry." + message.getMessageProperties().getReceivedRoutingKey(), message);
            }
        }
    }

    private CommunityEvent decode(Message message) throws IOException {
        @SuppressWarnings("unchecked") Map<String, Object> e = mapper.readValue(message.getBody(), Map.class);
        return new CommunityEvent(UUID.fromString(str(e, "eventId")), str(e, "eventType"), Instant.parse(str(e, "occurredAt")),
                UUID.fromString(str(e, "actorId")), UUID.fromString(str(e, "recipientId")), uuid(e.get("postId")), uuid(e.get("commentId")));
    }

    private void push(UUID recipient, UUID id, UUID actor, String type, UUID post, Instant at) {
        try {
            socket.convertAndSendToUser(recipient.toString(), "/queue/notifications", Map.of("id", id, "actorId", actor,
                    "eventType", type, "postId", post == null ? "" : post.toString(), "createdAt", at.toString()));
        } catch (RuntimeException ignored) { /* REST reconciliation remains authoritative after push failure. */ }
    }

    /**
     * Moves the message to the retry or dead-letter exchange and acks the original only after the broker confirms.
     * If parking itself fails, the message is requeued after a pause so a broker problem cannot spin the consumer.
     */
    private void park(Channel channel, long tag, String exchange, String routingKey, Message message) throws IOException {
        try {
            publishConfirmed(exchange, routingKey, message);
            channel.basicAck(tag, false);
        } catch (Exception parkingFailure) {
            if (parkingFailure instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("Could not park notification message messageId={} errorType={}",
                    message.getMessageProperties().getMessageId(), parkingFailure.getClass().getSimpleName());
            pause();
            channel.basicNack(tag, false, true);
        }
    }

    private static void pause() {
        try { Thread.sleep(REQUEUE_PAUSE_MILLIS); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
    }

    private static String str(Map<String, Object> e, String k) {
        Object v = e.get(k);
        if (!(v instanceof String s) || s.isBlank()) throw new IllegalArgumentException("Invalid event");
        return s;
    }

    private static UUID uuid(Object v) { return v == null ? null : UUID.fromString(v.toString()); }

    private void publishConfirmed(String exchange, String routingKey, Message message) throws Exception {
        var correlation = new CorrelationData(UUID.randomUUID().toString());
        rabbit.send(exchange, routingKey, message, correlation);
        var confirmation = correlation.getFuture().get(5, TimeUnit.SECONDS);
        if (!confirmation.isAck() || correlation.getReturned() != null)
            throw new IllegalStateException("RabbitMQ did not confirm event parking");
    }
}
