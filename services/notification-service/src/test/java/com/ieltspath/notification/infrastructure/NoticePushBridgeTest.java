package com.ieltspath.notification.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class NoticePushBridgeTest {
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

    private NoticePushBridge instance(SimpMessagingTemplate socket) throws Exception {
        var template = template();
        var bridge = new NoticePushBridge(socket, template, new ObjectMapper().findAndRegisterModules());
        var container = new RedisMessageListenerContainer();
        container.setConnectionFactory(template.getConnectionFactory());
        container.addMessageListener(bridge, new ChannelTopic(NoticePushBridge.CHANNEL));
        container.afterPropertiesSet();
        container.start();
        containers.add(container);
        Thread.sleep(300);
        return bridge;
    }

    @Test
    void aNoticePushedOnOneInstanceReachesTheUserOnEveryOtherInstanceOnce() throws Exception {
        var socketA = mock(SimpMessagingTemplate.class);
        var socketB = mock(SimpMessagingTemplate.class);
        var a = instance(socketA);
        instance(socketB);
        UUID user = UUID.randomUUID();
        var payload = Map.<String, Object>of("id", "n-1", "eventType", "assessment.grading-completed.v1", "slot", 2);

        a.pushToUser(user, payload);

        verify(socketA, times(1)).convertAndSendToUser(eq(user.toString()), eq("/queue/notifications"), any());
        verify(socketB, timeout(5000).times(1)).convertAndSendToUser(eq(user.toString()), eq("/queue/notifications"),
                eq(Map.of("id", "n-1", "eventType", "assessment.grading-completed.v1", "slot", 2)));
        Thread.sleep(300);
        verify(socketA, times(1)).convertAndSendToUser(eq(user.toString()), eq("/queue/notifications"), any());
    }

    @Test
    void localDeliveryDoesNotDependOnRedis() {
        var socket = mock(SimpMessagingTemplate.class);
        var template = new StringRedisTemplate(new LettuceConnectionFactory("127.0.0.1", 1));
        var bridge = new NoticePushBridge(socket, template, new ObjectMapper().findAndRegisterModules());
        UUID user = UUID.randomUUID();

        assertDoesNotThrow(() -> bridge.pushToUser(user, Map.of("id", "n-2")));

        verify(socket).convertAndSendToUser(eq(user.toString()), eq("/queue/notifications"), any());
        verify(socket, never()).convertAndSendToUser(eq("other"), any(), any());
    }

    @Test
    void anUnreadableRemoteMessageIsIgnored() throws Exception {
        var socket = mock(SimpMessagingTemplate.class);
        var bridge = instance(socket);

        bridge.onMessage(new org.springframework.data.redis.connection.DefaultMessage(
                NoticePushBridge.CHANNEL.getBytes(), "other-instance\n{not json".getBytes()), null);

        Thread.sleep(100);
        verify(socket, never()).convertAndSendToUser(any(), any(), any());
    }
}
