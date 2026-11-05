package com.ieltspath.library.infrastructure.persistence;

import com.ieltspath.library.infrastructure.persistence.entity.FlashcardJpaEntity;
import com.ieltspath.library.infrastructure.persistence.repository.FlashcardJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
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
class FlashcardConcurrentUpdateTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired FlashcardJpaRepository cards;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void parallelReviewsDoNotLoseReviewCountUpdates() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        jdbc.update("INSERT INTO flashcards(id,user_id,source_type,front,back,status) VALUES (?,?,?,?,?,?)",
                cardId, userId, "MANUAL", "front", "back", "ACTIVE");
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> review(userId, cardId, transaction, ready, start));
            var second = pool.submit(() -> review(userId, cardId, transaction, ready, start));
            ready.await();
            start.countDown();
            first.get();
            second.get();
        }

        assertEquals(2, jdbc.queryForObject("SELECT review_count FROM flashcards WHERE id=?", Integer.class, cardId));
    }

    private Void review(UUID userId, UUID cardId, TransactionTemplate transaction,
                        CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        transaction.executeWithoutResult(status -> {
            FlashcardJpaEntity card = cards.findForReviewByIdAndUserId(cardId, userId).orElseThrow();
            card.setReviewCount(card.getReviewCount() + 1);
            cards.saveAndFlush(card);
        });
        return null;
    }
}
