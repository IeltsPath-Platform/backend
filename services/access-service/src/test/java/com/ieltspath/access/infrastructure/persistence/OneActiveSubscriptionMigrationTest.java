package com.ieltspath.access.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers(disabledWithoutDocker = true)
class OneActiveSubscriptionMigrationTest {
    private static final String PLAN_ID = "22222222-2222-2222-2222-222222222222";

    @Test
    void expiresLapsedRowsKeepsTheLatestActiveAndAddsTheUniqueIndex() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")) {
            postgres.start();
            flyway(postgres, "14").migrate();

            UUID userId = UUID.randomUUID();
            UUID expiredId = UUID.randomUUID();
            UUID olderId = UUID.randomUUID();
            UUID latestId = UUID.randomUUID();
            try (Connection connection = connection(postgres); Statement statement = connection.createStatement()) {
                insert(statement, expiredId, userId, "2020-01-01T00:00:00Z");
                insert(statement, olderId, userId, "2030-01-01T00:00:00Z");
                insert(statement, latestId, userId, "2031-01-01T00:00:00Z");
            }

            flyway(postgres, null).migrate();

            try (Connection connection = connection(postgres); Statement statement = connection.createStatement()) {
                try (var rows = statement.executeQuery("SELECT id, status FROM subscriptions WHERE user_id = '" + userId + "'")) {
                    var statuses = new java.util.HashMap<UUID, String>();
                    while (rows.next()) statuses.put(UUID.fromString(rows.getString("id")), rows.getString("status"));
                    assertThat(statuses).containsEntry(expiredId, "EXPIRED")
                            .containsEntry(olderId, "CANCELLED")
                            .containsEntry(latestId, "ACTIVE");
                }
                assertThrows(SQLException.class, () -> insert(statement, UUID.randomUUID(), userId, "2032-01-01T00:00:00Z"));
            }
        }
    }

    private static Flyway flyway(PostgreSQLContainer<?> postgres, String target) {
        return Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .target(target == null ? MigrationVersion.LATEST : MigrationVersion.fromVersion(target))
                .load();
    }

    private static Connection connection(PostgreSQLContainer<?> postgres) throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    private static void insert(Statement statement, UUID id, UUID userId, String endsAt) throws SQLException {
        statement.executeUpdate("INSERT INTO subscriptions (id, user_id, plan_id, status, starts_at, ends_at, source_type) "
                + "VALUES ('" + id + "', '" + userId + "', '" + PLAN_ID + "', 'ACTIVE', now(), '" + endsAt
                + "', 'ACTIVATION_KEY')");
    }
}
