package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.aggregate.LessonProgress;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
class LessonProgressConcurrentUpdateTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void parallelBlockPassesKeepBothProgressUpdates() throws Exception {
        var dataSource = new PGSimpleDataSource();
        dataSource.setURL(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();

        var jdbc = new JdbcTemplate(dataSource);
        var lock = new JdbcLearnerLock(jdbc);
        var lessons = new JdbcLessonProgressRepository(jdbc);
        var transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        UUID userId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID firstBlock = UUID.randomUUID();
        UUID secondBlock = UUID.randomUUID();
        transaction.executeWithoutResult(status -> lessons.save(
                LessonProgress.start(userId, lessonId, topicId, 1, List.of())));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> passBlock(transaction, lock, lessons, userId, lessonId, firstBlock, ready, start));
            var second = pool.submit(() -> passBlock(transaction, lock, lessons, userId, lessonId, secondBlock, ready, start));
            ready.await();
            start.countDown();
            first.get();
            second.get();
        }

        assertEquals(Set.of(firstBlock, secondBlock), lessons.find(userId, lessonId).orElseThrow().passedBlockIds());
    }

    private static Void passBlock(TransactionTemplate transaction, JdbcLearnerLock lock,
                                  JdbcLessonProgressRepository lessons, UUID userId, UUID lessonId, UUID blockId,
                                  CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        transaction.executeWithoutResult(status -> {
            lock.lock(userId);
            LessonProgress progress = lessons.find(userId, lessonId).orElseThrow();
            progress.passBlock(blockId);
            lessons.save(progress);
        });
        return null;
    }
}
