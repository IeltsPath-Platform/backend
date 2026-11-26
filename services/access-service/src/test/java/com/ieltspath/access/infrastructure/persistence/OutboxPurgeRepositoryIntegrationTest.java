package com.ieltspath.access.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
class OutboxPurgeRepositoryIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void deletesOnlyOldPublishedRowsAndKeepsFailedAndNotRelayedRows() {
        JdbcTemplate jdbc = jdbc();
        jdbc.execute("CREATE TABLE outbox_events (id UUID PRIMARY KEY, status VARCHAR(20), published_at TIMESTAMPTZ)");
        OutboxPurgeRepository repository = new OutboxPurgeRepository(jdbc);
        Instant now = Instant.now();
        Instant cutoff = now.minusSeconds(30 * 86_400L);
        insert(jdbc, "PUBLISHED", now.minusSeconds(31 * 86_400L));
        insert(jdbc, "PUBLISHED", now.minusSeconds(32 * 86_400L));
        insert(jdbc, "PUBLISHED", now.minusSeconds(5 * 86_400L));
        insert(jdbc, "PENDING", null);
        insert(jdbc, "FAILED", null);
        insert(jdbc, "NOT_RELAYED", null);
        applyIndexMigration(jdbc, "V15__outbox_published_index.sql", "idx_access_outbox_published");

        assertEquals(1, repository.deletePublishedBefore(cutoff, 1));
        assertEquals(1, repository.deletePublishedBefore(cutoff, 1));
        assertEquals(0, repository.deletePublishedBefore(cutoff, 1));
        assertEquals(4, count(jdbc));
    }

    @Test
    void concurrentRunsDeleteEligibleRowsOnlyOnce() throws Exception {
        JdbcTemplate jdbc = jdbc();
        jdbc.execute("CREATE TABLE outbox_events (id UUID PRIMARY KEY, status VARCHAR(20), published_at TIMESTAMPTZ)");
        OutboxPurgeRepository repository = new OutboxPurgeRepository(jdbc);
        Instant now = Instant.now();
        Instant cutoff = now.minusSeconds(30 * 86_400L);
        for (int index = 0; index < 100; index++) insert(jdbc, "PUBLISHED", now.minusSeconds(31 * 86_400L));

        try (var pool = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            var first = pool.submit(() -> { start.await(); return repository.deletePublishedBefore(cutoff, 100); });
            var second = pool.submit(() -> { start.await(); return repository.deletePublishedBefore(cutoff, 100); });
            start.countDown();
            assertEquals(100, first.get(5, TimeUnit.SECONDS) + second.get(5, TimeUnit.SECONDS));
        }
        assertEquals(0, count(jdbc));
    }

    @Test
    void purgeDoesNotWaitForRelayLockedUnpublishedRows() throws Exception {
        JdbcTemplate jdbc = jdbc();
        jdbc.execute("CREATE TABLE outbox_events (id UUID PRIMARY KEY, status VARCHAR(20), published_at TIMESTAMPTZ)");
        OutboxPurgeRepository repository = new OutboxPurgeRepository(jdbc);
        Instant cutoff = Instant.now().minusSeconds(30 * 86_400L);
        UUID pendingId = UUID.randomUUID();
        insert(jdbc, "PUBLISHED", Instant.now().minusSeconds(31 * 86_400L));
        jdbc.update("INSERT INTO outbox_events (id, status) VALUES (?, 'PENDING')", pendingId);

        try (Connection relayConnection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            relayConnection.setAutoCommit(false);
            try (var statement = relayConnection.prepareStatement(
                    "UPDATE outbox_events SET status = 'PENDING' WHERE id = ?")) {
                statement.setObject(1, pendingId);
                statement.executeUpdate();
            }
            var pool = Executors.newSingleThreadExecutor();
            try {
                var purge = pool.submit(() -> repository.deletePublishedBefore(cutoff, 10));
                assertEquals(1, purge.get(5, TimeUnit.SECONDS));
            } finally {
                relayConnection.rollback();
                pool.shutdownNow();
            }
        }
        assertEquals(1, count(jdbc));
    }

    private JdbcTemplate jdbc() {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        jdbc.execute("DROP TABLE IF EXISTS outbox_events");
        return jdbc;
    }

    private void insert(JdbcTemplate jdbc, String status, Instant publishedAt) {
        jdbc.update("INSERT INTO outbox_events (id, status, published_at) VALUES (?, ?, ?)", UUID.randomUUID(), status,
                publishedAt == null ? null : java.sql.Timestamp.from(publishedAt));
    }

    private int count(JdbcTemplate jdbc) {
        return jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class);
    }

    private void applyIndexMigration(JdbcTemplate jdbc, String migration, String indexName) {
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/" + migration)).execute(jdbc.getDataSource());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM pg_indexes WHERE indexname = ?", Integer.class, indexName));
    }
}
