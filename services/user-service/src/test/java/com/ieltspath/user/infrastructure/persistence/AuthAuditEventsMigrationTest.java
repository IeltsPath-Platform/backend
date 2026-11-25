package com.ieltspath.user.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class AuthAuditEventsMigrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
    }

    @Test
    void createsIndexedAuditTableWithoutUserForeignKeyAndStoresJsonb() throws Exception {
        UUID deletedUserId = UUID.randomUUID();
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            for (String name : new String[]{"auth_audit_events", "idx_auth_audit_user_created", "idx_auth_audit_type_created"}) {
                try (var rows = statement.executeQuery("SELECT to_regclass('public." + name + "')")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString(1)).isNotNull();
                }
            }
            statement.executeUpdate("INSERT INTO auth_audit_events (user_id, event_type, detail) VALUES ('" + deletedUserId
                    + "', 'SESSION_REVOKED', '{\"reason\":\"USER_REQUEST\"}'::jsonb)");
            try (var rows = statement.executeQuery("SELECT detail->>'reason', pg_typeof(detail)::text FROM auth_audit_events WHERE user_id='"
                    + deletedUserId + "'")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("USER_REQUEST");
                assertThat(rows.getString(2)).isEqualTo("jsonb");
            }
            statement.executeUpdate("DELETE FROM users WHERE id='" + deletedUserId + "'");
            try (var rows = statement.executeQuery("SELECT count(*) FROM auth_audit_events WHERE user_id='" + deletedUserId + "'")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getInt(1)).isEqualTo(1);
            }
        }
    }

    @Test
    void purgesOnlyExpiredRowsInTheRequestedBatch() throws Exception {
        UUID userId = UUID.randomUUID();
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO auth_audit_events (user_id, event_type, created_at) VALUES ('" + userId
                    + "', 'SESSION_REVOKED', CURRENT_TIMESTAMP - interval '400 days'), ('" + userId
                    + "', 'SESSION_REVOKED', CURRENT_TIMESTAMP - interval '1 day')");
            int removed = statement.executeUpdate("DELETE FROM auth_audit_events WHERE id IN (SELECT id FROM auth_audit_events "
                    + "WHERE created_at < CURRENT_TIMESTAMP - interval '365 days' ORDER BY created_at, id LIMIT 1 FOR UPDATE SKIP LOCKED)");
            assertThat(removed).isEqualTo(1);
            try (var rows = statement.executeQuery("SELECT count(*) FROM auth_audit_events WHERE user_id='" + userId + "'")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getInt(1)).isEqualTo(1);
            }
        }
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
