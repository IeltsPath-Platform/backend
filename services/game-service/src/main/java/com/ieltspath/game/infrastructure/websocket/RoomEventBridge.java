package com.ieltspath.game.infrastructure.websocket;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Carries room events between instances over one Redis channel. Each instance relays the events it commits, and
 * delivers the ones it receives from other instances to its own sessions. An instance drops its own messages, since
 * its local listeners already delivered them. A missed message is not retried: the REST state stays authoritative.
 */
@Component
public class RoomEventBridge implements MessageListener {
    public static final String CHANNEL = "game.room-events";
    private static final Logger log = LoggerFactory.getLogger(RoomEventBridge.class);

    private final String instanceId = UUID.randomUUID().toString();
    private final RoomEventCodec codec;
    private final StringRedisTemplate redis;
    private final RoomEventDelivery delivery;

    public RoomEventBridge(RoomEventCodec codec, StringRedisTemplate redis, RoomEventDelivery delivery) {
        this.codec = codec;
        this.redis = redis;
        this.delivery = delivery;
    }

    /** Runs after the transaction that produced the event commits, so no instance sees an event that was rolled back. */
    @TransactionalEventListener
    public void relay(Object event) {
        if (!codec.isRoomEvent(event)) return;
        try {
            redis.convertAndSend(CHANNEL, instanceId + "\n" + codec.encode(event));
        } catch (RuntimeException failure) {
            log.warn("Room event not relayed: errorType={}", failure.getClass().getSimpleName());
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String raw = new String(message.getBody(), StandardCharsets.UTF_8);
        int split = raw.indexOf('\n');
        if (split < 0 || raw.substring(0, split).equals(instanceId)) return;
        try {
            delivery.deliver(codec.decode(raw.substring(split + 1)));
        } catch (RuntimeException failure) {
            log.warn("Room event not delivered: errorType={}", failure.getClass().getSimpleName());
        }
    }
}
