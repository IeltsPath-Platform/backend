package com.ieltspath.user.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class RefreshTokenSessionMigrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void addsIndependentSessionFamiliesToExistingRefreshTokens() throws Exception {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").target(MigrationVersion.fromVersion("16")).load().migrate();
        UUID userId = UUID.randomUUID();
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO users (id, email, full_name, password_hash, status) VALUES ('"
                    + userId + "', 'refresh-migration@example.com', 'Test User', 'hash', 'ACTIVE')");
            statement.executeUpdate("INSERT INTO refresh_tokens (user_id, token_hash, expires_at) VALUES ('"
                    + userId + "', 'legacy-refresh-a', CURRENT_TIMESTAMP + interval '1 day'), ('"
                    + userId + "', 'legacy-refresh-b', CURRENT_TIMESTAMP + interval '1 day')");
        }

        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();

        try (Connection connection = connection(); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT count(DISTINCT family_id), count(*), count(*) FILTER (WHERE session_started_at IS NOT NULL AND revoked_reason IS NULL) FROM refresh_tokens WHERE user_id = '" + userId + "'")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getInt(1)).isEqualTo(2);
            assertThat(rows.getInt(2)).isEqualTo(2);
            assertThat(rows.getInt(3)).isEqualTo(2);
        }
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
