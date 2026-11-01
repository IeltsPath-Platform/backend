package com.ieltspath.game.infrastructure.persistence;

import com.ieltspath.game.infrastructure.persistence.entity.GameRoomJpaEntity;
import com.ieltspath.game.infrastructure.persistence.entity.GameRoomMemberJpaEntity;
import com.ieltspath.game.infrastructure.persistence.repository.GameRoomJpaRepository;
import com.ieltspath.game.infrastructure.persistence.repository.GameRoomMemberJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
        "spring.cloud.config.enabled=false", "eureka.client.enabled=false"
})
class GameRoomConcurrentUpdateTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired GameRoomJpaRepository rooms;
    @Autowired GameRoomMemberJpaRepository members;
    @Autowired PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentRoomJoinsRespectCapacity() throws Exception {
        UUID roomId = UUID.randomUUID();
        UUID host = UUID.randomUUID();
        Instant now = Instant.now();
        rooms.saveAndFlush(new GameRoomJpaEntity(roomId, "R" + UUID.randomUUID().toString().substring(0, 5)
                .toUpperCase(), host, "WORD_MEANING_MATCH", "VOCABULARY", "PRACTICE", 2, Map.of(), now, null));
        members.saveAndFlush(new GameRoomMemberJpaEntity(UUID.randomUUID(), roomId, host, "HOST", now));
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> join(roomId, UUID.randomUUID(), transaction, ready, start));
            var second = pool.submit(() -> join(roomId, UUID.randomUUID(), transaction, ready, start));
            ready.await();
            start.countDown();
            first.get();
            second.get();
        }

        assertEquals(2, members.findAllByRoomIdOrderByJoinedAt(roomId).size());
    }

    private Void join(UUID roomId, UUID userId, TransactionTemplate transaction,
                      CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        transaction.executeWithoutResult(status -> {
            var room = rooms.findByIdForUpdate(roomId).orElseThrow();
            int joined = members.findAllByRoomIdOrderByJoinedAt(roomId).size();
            if (joined < room.getMaxPlayers()) {
                members.saveAndFlush(new GameRoomMemberJpaEntity(UUID.randomUUID(), roomId, userId, "PLAYER", Instant.now()));
            }
        });
        return null;
    }
}
