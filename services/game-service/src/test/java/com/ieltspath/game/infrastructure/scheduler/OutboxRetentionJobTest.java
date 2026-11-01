package com.ieltspath.game.infrastructure.scheduler;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The retention delete against the real outbox table, migrated by the service's own Flyway scripts. */
@Testcontainers(disabledWithoutDocker = true)
class OutboxRetentionJobTest {

    @Test
    void deletesRowsOlderThanRetentionAndKeepsRecentRows() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")) {
            postgres.start();
            Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration").load().migrate();
            JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
            Instant now = Instant.now();
            insert(jdbc, now.minus(31, ChronoUnit.DAYS));
            insert(jdbc, now.minus(29, ChronoUnit.DAYS));

            int deleted = new OutboxRetentionJob(jdbc, 30).runOnce(now);

            assertEquals(1, deleted);
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class));
        }
    }

    private static void insert(JdbcTemplate jdbc, Instant createdAt) {
        jdbc.update("INSERT INTO outbox_events (id, aggregate_type, aggregate_id, event_type, payload, created_at) "
                + "VALUES (?, 'GameRoom', ?, 'GameRoomCreated', '{}'::jsonb, ?)",
                UUID.randomUUID(), UUID.randomUUID().toString(), Timestamp.from(createdAt));
    }
}
