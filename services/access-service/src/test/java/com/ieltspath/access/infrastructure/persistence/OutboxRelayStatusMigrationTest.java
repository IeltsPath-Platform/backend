package com.ieltspath.access.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Runs the outbox migration on a database that was migrated only up to the version before it. */
@Testcontainers(disabledWithoutDocker = true)
class OutboxRelayStatusMigrationTest {

    private static Flyway flyway(PostgreSQLContainer<?> postgres, MigrationVersion target) {
        return Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration").target(target).load();
    }

    @Test
    void rowsPendingBeforeTheRelayExistsAreKeptAsNotRelayed() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")) {
            postgres.start();
            flyway(postgres, MigrationVersion.fromVersion("5")).migrate();
            UUID existing = UUID.randomUUID();
            try (Connection connection = connection(postgres); Statement statement = connection.createStatement()) {
                statement.executeUpdate("INSERT INTO outbox_events (id, aggregate_type, aggregate_id, event_type, payload, status) "
                        + "VALUES ('" + existing + "', 'Subscription', '" + UUID.randomUUID()
                        + "', 'SubscriptionGranted', '{}', 'PENDING')");
            }

            flyway(postgres, MigrationVersion.LATEST).migrate();

            try (Connection connection = connection(postgres);
                 Statement statement = connection.createStatement();
                 ResultSet row = statement.executeQuery("SELECT status FROM outbox_events WHERE id = '" + existing + "'")) {
                row.next();
                assertEquals("NOT_RELAYED", row.getString("status"));
            }
        }
    }

    @Test
    void theStatusCheckAcceptsNotRelayedAndRejectsUnknownValues() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")) {
            postgres.start();
            flyway(postgres, MigrationVersion.LATEST).migrate();
            try (Connection connection = connection(postgres); Statement statement = connection.createStatement()) {
                statement.executeUpdate("INSERT INTO outbox_events (id, aggregate_type, aggregate_id, event_type, payload, status, last_error) "
                        + "VALUES ('" + UUID.randomUUID() + "', 'X', '" + UUID.randomUUID()
                        + "', 'Y', '{}', 'NOT_RELAYED', 'boom')");
                assertThrows(SQLException.class, () -> statement.executeUpdate(
                        "INSERT INTO outbox_events (id, aggregate_type, aggregate_id, event_type, payload, status) "
                                + "VALUES ('" + UUID.randomUUID() + "', 'X', '" + UUID.randomUUID()
                                + "', 'Y', '{}', 'BOGUS')"));
            }
        }
    }

    private static Connection connection(PostgreSQLContainer<?> postgres) throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}
