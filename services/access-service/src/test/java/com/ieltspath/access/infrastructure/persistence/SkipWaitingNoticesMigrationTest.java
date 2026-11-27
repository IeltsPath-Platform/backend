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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class SkipWaitingNoticesMigrationTest {
    private static final String PLAN_ID = "22222222-2222-2222-2222-222222222222";

    @Test
    void nothingThatWasWaitingBeforeSchedulingIsSent() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")) {
            postgres.start();
            flyway(postgres, "18").migrate();

            UUID waiting = UUID.randomUUID();
            UUID published = UUID.randomUUID();
            UUID ended = UUID.randomUUID();
            UUID alreadyMarked = UUID.randomUUID();
            UUID running = UUID.randomUUID();
            try (Connection connection = connection(postgres); Statement statement = connection.createStatement()) {
                outbox(statement, waiting, "PENDING");
                outbox(statement, published, "PUBLISHED");
                subscription(statement, ended, "ACTIVE", "2020-01-01T00:00:00Z");
                subscription(statement, alreadyMarked, "EXPIRED", "2020-02-01T00:00:00Z");
                subscription(statement, running, "ACTIVE", "2099-01-01T00:00:00Z");
                statement.executeUpdate("INSERT INTO subscription_expiry_notices (subscription_id, stage, sent_at) VALUES ('"
                        + running + "', 'SEVEN_DAYS', now())");
            }

            flyway(postgres, null).migrate();

            try (Connection connection = connection(postgres); Statement statement = connection.createStatement()) {
                assertThat(values(statement, "SELECT status FROM outbox_events WHERE id = '" + waiting + "'"))
                        .containsExactly("NOT_RELAYED");
                assertThat(values(statement, "SELECT status FROM outbox_events WHERE id = '" + published + "'"))
                        .containsExactly("PUBLISHED");
                assertThat(values(statement, "SELECT subscription_id::text FROM subscription_expiry_notices "
                        + "WHERE stage = 'EXPIRED'")).containsExactlyInAnyOrder(ended.toString(), alreadyMarked.toString());
                assertThat(values(statement, "SELECT count(*)::text FROM subscription_expiry_notices n JOIN subscriptions s "
                        + "ON s.id = n.subscription_id AND s.ends_at = n.ends_at")).containsExactly("3");
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

    private static void outbox(Statement statement, UUID id, String status) throws SQLException {
        statement.executeUpdate("INSERT INTO outbox_events (id, aggregate_type, aggregate_id, event_type, payload, status) "
                + "VALUES ('" + id + "', 'Subscription', '" + UUID.randomUUID() + "', 'SubscriptionGranted', '{}', '"
                + status + "')");
    }

    private static void subscription(Statement statement, UUID id, String status, String endsAt) throws SQLException {
        statement.executeUpdate("INSERT INTO subscriptions (id, user_id, plan_id, status, starts_at, ends_at, source_type) "
                + "VALUES ('" + id + "', '" + UUID.randomUUID() + "', '" + PLAN_ID + "', '" + status
                + "', '2019-01-01T00:00:00Z', '" + endsAt + "', 'ACTIVATION_KEY')");
    }

    private static List<String> values(Statement statement, String query) throws SQLException {
        List<String> values = new ArrayList<>();
        try (ResultSet rows = statement.executeQuery(query)) {
            while (rows.next()) values.add(rows.getString(1));
        }
        return values;
    }
}
