package com.ieltspath.access.infrastructure.persistence;

import com.ieltspath.access.application.command.DebitPointsCommand;
import com.ieltspath.access.application.result.PointLedgerResult;
import com.ieltspath.access.application.usecase.DebitPointsUseCase;
import com.ieltspath.access.application.usecase.PointDebitWriter;
import com.ieltspath.access.infrastructure.persistence.adapter.OutboxEventRepositoryAdapter;
import com.ieltspath.access.infrastructure.persistence.adapter.PointLedgerRepositoryAdapter;
import com.ieltspath.access.infrastructure.persistence.adapter.PointWalletRepositoryAdapter;
import com.ieltspath.access.infrastructure.persistence.mapper.AccessPersistenceMapper;
import com.ieltspath.commonobservability.CorrelationRabbitAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doAnswer;

@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({DebitPointsUseCase.class, PointDebitWriter.class, PointWalletRepositoryAdapter.class,
        PointLedgerRepositoryAdapter.class, OutboxEventRepositoryAdapter.class,
        AccessPersistenceMapper.class, CorrelationRabbitAutoConfiguration.class})
class PointDebitConcurrencyTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired DebitPointsUseCase debit;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean PointLedgerRepositoryAdapter ledger;

    @Test
    void duplicateThatReadsTheWalletAfterTheWinnerCommitsReturnsTheSamePaidLedger() throws Exception {
        UUID user = UUID.randomUUID();
        String key = "vocab-study:" + UUID.randomUUID();
        var command = new DebitPointsCommand(user, user, 5, "HEART_REFILL", UUID.randomUUID(), key, "Heart refill");
        jdbc.update("INSERT INTO point_wallets (user_id, balance, total_credited) VALUES (?, 5, 5)", user);

        var missed = new CountDownLatch(1);
        var committed = new CountDownLatch(1);
        var firstRead = new AtomicBoolean(true);
        doAnswer(invocation -> {
            Object captured = invocation.callRealMethod();
            if (Thread.currentThread().getName().equals("duplicate-debit") && firstRead.compareAndSet(true, false)) {
                assertTrue(((java.util.Optional<?>) captured).isEmpty());
                missed.countDown();
                assertTrue(committed.await(10, TimeUnit.SECONDS));
            }
            return captured;
        }).when(ledger).findByIdempotencyKey(key);

        ExecutorService executor = Executors.newSingleThreadExecutor(task -> new Thread(task, "duplicate-debit"));
        try {
            Future<PointLedgerResult> duplicate = executor.submit(() -> debit.execute(command));
            assertTrue(missed.await(10, TimeUnit.SECONDS));
            PointLedgerResult winner = debit.execute(command);
            assertEquals(0L, winner.balanceAfter());
            committed.countDown();
            PointLedgerResult replayed = duplicate.get(10, TimeUnit.SECONDS);
            assertAll(
                    () -> assertEquals(winner.id(), replayed.id()),
                    () -> assertEquals(winner.userId(), replayed.userId()),
                    () -> assertEquals(winner.delta(), replayed.delta()),
                    () -> assertEquals(winner.balanceAfter(), replayed.balanceAfter()),
                    () -> assertEquals(winner.transactionType(), replayed.transactionType()),
                    () -> assertEquals(winner.referenceType(), replayed.referenceType()),
                    () -> assertEquals(winner.referenceId(), replayed.referenceId()),
                    () -> assertEquals(winner.description(), replayed.description()),
                    // PostgreSQL stores microseconds; compare the replay with the committed timestamp.
                    () -> assertEquals(jdbc.queryForObject(
                            "SELECT created_at FROM point_ledger_entries WHERE idempotency_key = ?",
                            java.sql.Timestamp.class, key).toInstant(), replayed.createdAt()));
            assertEquals(0L, jdbc.queryForObject("SELECT balance FROM point_wallets WHERE user_id = ?", Long.class, user));
            assertEquals(5L, jdbc.queryForObject("SELECT total_debited FROM point_wallets WHERE user_id = ?", Long.class, user));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM point_ledger_entries WHERE user_id = ?", Integer.class, user));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM outbox_events WHERE aggregate_id = ? AND event_type = 'PointDebited'", Integer.class, user));
        } finally {
            committed.countDown();
            executor.shutdownNow();
        }
    }
}
