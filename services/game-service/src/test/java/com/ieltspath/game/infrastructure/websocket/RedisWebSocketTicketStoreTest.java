package com.ieltspath.game.infrastructure.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class RedisWebSocketTicketStoreTest {
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    static StringRedisTemplate template() {
        var factory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(redis.getHost(), redis.getMappedPort(6379)));
        factory.afterPropertiesSet();
        var template = new StringRedisTemplate(factory);
        template.afterPropertiesSet();
        return template;
    }

    @Test
    void ticketIsConsumedOnlyOnce() {
        var store = new RedisWebSocketTicketStore(template(), Duration.ofSeconds(30));
        UUID room = UUID.randomUUID();
        UUID user = UUID.randomUUID();

        var ticket = store.issue(room, user);

        assertEquals(room, store.consume(ticket.token()).orElseThrow().roomId());
        assertFalse(store.consume(ticket.token()).isPresent());
    }

    @Test
    void ticketIssuedByOneInstanceIsConsumableByAnother() {
        var template = template();
        var issuingInstance = new RedisWebSocketTicketStore(template, Duration.ofSeconds(30));
        var acceptingInstance = new RedisWebSocketTicketStore(template, Duration.ofSeconds(30));
        UUID user = UUID.randomUUID();

        var ticket = issuingInstance.issue(UUID.randomUUID(), user);

        assertEquals(user, acceptingInstance.consume(ticket.token()).orElseThrow().userId());
    }

    @Test
    void unknownOrBlankTokenIsRejected() {
        var store = new RedisWebSocketTicketStore(template(), Duration.ofSeconds(30));

        assertFalse(store.consume("missing-token").isPresent());
        assertFalse(store.consume(null).isPresent());
        assertFalse(store.consume("  ").isPresent());
    }

    @Test
    void ticketExpiresAfterTheConfiguredTtl() throws InterruptedException {
        var store = new RedisWebSocketTicketStore(template(), Duration.ofSeconds(1));
        var ticket = store.issue(UUID.randomUUID(), UUID.randomUUID());

        Thread.sleep(1500);

        assertFalse(store.consume(ticket.token()).isPresent());
    }
}
