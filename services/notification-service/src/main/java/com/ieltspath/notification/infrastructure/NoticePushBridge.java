package com.ieltspath.notification.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Pushes a notice to a learner's open connections on every instance. The instance that stored the notice delivers it to
 * its own connections at once and also publishes it on a Redis channel; other instances deliver what they receive. The
 * stored notice stays authoritative: a missed push is recovered when the learner reads the inbox.
 */
@Component
public class NoticePushBridge implements MessageListener {
    public static final String CHANNEL = "notification.push";
    private static final String DESTINATION = "/queue/notifications";
    private static final Logger log = LoggerFactory.getLogger(NoticePushBridge.class);
    private static final TypeReference<Map<String, Object>> PAYLOAD = new TypeReference<>() { };

    private final String instanceId = UUID.randomUUID().toString();
    private final SimpMessagingTemplate socket;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public NoticePushBridge(SimpMessagingTemplate socket, StringRedisTemplate redis, ObjectMapper mapper) {
        this.socket = socket;
        this.redis = redis;
        this.mapper = mapper;
    }

    public void pushToUser(UUID userId, Map<String, Object> payload) {
        deliverHere(userId, payload);
        try {
            String json = mapper.writeValueAsString(payload);
            redis.convertAndSend(CHANNEL, instanceId + "\n" + userId + "\n" + json);
        } catch (Exception failure) {
            log.warn("Notice not relayed to other instances: errorType={}", failure.getClass().getSimpleName());
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String[] parts = new String(message.getBody(), StandardCharsets.UTF_8).split("\n", 3);
        if (parts.length != 3 || parts[0].equals(instanceId)) return;
        try {
            UUID userId = UUID.fromString(parts[1]);
            deliverHere(userId, mapper.readValue(parts[2], PAYLOAD));
        } catch (Exception malformed) {
            log.warn("Relayed notice dropped: errorType={}", malformed.getClass().getSimpleName());
        }
    }

    private void deliverHere(UUID userId, Map<String, Object> payload) {
        try {
            socket.convertAndSendToUser(userId.toString(), DESTINATION, payload);
        } catch (RuntimeException ignored) {
            // The inbox over REST stays authoritative after a push failure.
        }
    }
}
