package com.ieltspath.game.infrastructure.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.game.application.result.MatchStartedEvent;
import com.ieltspath.game.application.result.RoomClosedEvent;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class RoomEventBridgeTest {
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    private final List<RedisMessageListenerContainer> containers = new CopyOnWriteArrayList<>();

    @AfterEach
    void stopListeners() {
        containers.forEach(RedisMessageListenerContainer::stop);
    }

    private StringRedisTemplate template() {
        var factory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(redis.getHost(), redis.getMappedPort(6379)));
        factory.afterPropertiesSet();
        var template = new StringRedisTemplate(factory);
        template.afterPropertiesSet();
        return template;
    }

    /** Creates an instance-like bridge whose local deliveries are recorded, and starts its subscription. */
    private RoomEventBridge instance(List<Object> delivered) throws Exception {
        var codec = new RoomEventCodec(new ObjectMapper().findAndRegisterModules());
        var template = template();
        var bridge = new RoomEventBridge(codec, template, delivered::add);
        var container = new RedisMessageListenerContainer();
        container.setConnectionFactory(template.getConnectionFactory());
        container.addMessageListener(bridge, new ChannelTopic(RoomEventBridge.CHANNEL));
        container.afterPropertiesSet();
        container.start();
        containers.add(container);
        Thread.sleep(300);
        return bridge;
    }

    @Test
    void anEventRelayedByOneInstanceIsDeliveredOnceToEveryOtherInstance() throws Exception {
        List<Object> onA = new CopyOnWriteArrayList<>();
        List<Object> onB = new CopyOnWriteArrayList<>();
        List<Object> onC = new CopyOnWriteArrayList<>();
        var a = instance(onA);
        instance(onB);
        instance(onC);
        var event = new RoomClosedEvent(UUID.randomUUID(), "Phòng đã đóng");

        a.relay(event);

        awaitSize(onB, 1);
        awaitSize(onC, 1);
        assertEquals(event, onB.getFirst());
        assertEquals(event, onC.getFirst());
        Thread.sleep(300);
        assertTrue(onA.isEmpty(), "the originating instance delivers locally, not through Redis");
        assertEquals(1, onB.size());
    }

    @Test
    void eventsOutsideTheRoomEventSetAreNotRelayed() throws Exception {
        List<Object> onB = new CopyOnWriteArrayList<>();
        var a = instance(new CopyOnWriteArrayList<>());
        instance(onB);

        a.relay("not a room event");
        a.relay(7);
        Thread.sleep(300);
        assertTrue(onB.isEmpty());

        a.relay(new MatchStartedEvent(UUID.randomUUID(), UUID.randomUUID()));
        awaitSize(onB, 1);
        assertTrue(onB.getFirst() instanceof MatchStartedEvent);
    }

    private static void awaitSize(List<?> list, int size) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (list.size() < size && System.nanoTime() < deadline) Thread.sleep(50);
        assertEquals(size, list.size());
    }
}
