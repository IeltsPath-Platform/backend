package com.ieltspath.access.infrastructure.persistence;

import com.ieltspath.access.application.command.DebitPointsCommand;
import com.ieltspath.access.application.usecase.PointDebitWriter;
import com.ieltspath.access.infrastructure.persistence.adapter.OutboxEventRepositoryAdapter;
import com.ieltspath.access.infrastructure.persistence.adapter.PointLedgerRepositoryAdapter;
import com.ieltspath.access.infrastructure.persistence.adapter.PointWalletRepositoryAdapter;
import com.ieltspath.access.infrastructure.persistence.mapper.AccessPersistenceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A debit writes wallet, ledger and a JSONB outbox row through the real JPA mapping. */
@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PointDebitWriter.class, PointWalletRepositoryAdapter.class, PointLedgerRepositoryAdapter.class,
        OutboxEventRepositoryAdapter.class, AccessPersistenceMapper.class})
class PointDebitPersistenceTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired PointDebitWriter writer;
    @Autowired JdbcTemplate jdbc;
    @Autowired TestEntityManager entityManager;

    @Test
    void debitStoresTheOutboxPayloadAsJsonb() {
        UUID userId = UUID.randomUUID();
        jdbc.update("INSERT INTO point_wallets (user_id, balance, total_credited) VALUES (?, 10, 10)", userId);

        writer.debit(new DebitPointsCommand(userId, userId, 3, "LESSON_WRITING", UUID.randomUUID(),
                "lesson-writing:" + userId + ":" + UUID.randomUUID(), "Writing grading"));
        entityManager.flush();

        assertEquals(7L, jdbc.queryForObject("SELECT balance FROM point_wallets WHERE user_id = ?", Long.class, userId));
        assertEquals(-3L, jdbc.queryForObject("SELECT delta FROM point_ledger_entries WHERE user_id = ?", Long.class, userId));
        assertEquals("3", jdbc.queryForObject(
                "SELECT payload ->> 'amount' FROM outbox_events WHERE aggregate_id = ? AND event_type = 'PointDebited'",
                String.class, userId));
    }
}
