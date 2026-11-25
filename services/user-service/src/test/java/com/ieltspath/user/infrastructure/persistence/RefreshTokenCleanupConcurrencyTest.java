package com.ieltspath.user.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class RefreshTokenCleanupConcurrencyTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
    }

    @Test
    void concurrentCleanupSkipsRowsClaimedByAnotherInstanceAndKeepsActiveTokens() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID lockedId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID deletableId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID liveId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        try (Connection setup = connection(); var statement = setup.createStatement()) {
            statement.executeUpdate("INSERT INTO users (id, email, full_name, password_hash, status) VALUES ('"
                    + userId + "', 'cleanup@example.com', 'Test User', 'hash', 'ACTIVE')");
            statement.executeUpdate("INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at, revoked_at) VALUES "
                    + "('" + lockedId + "', '" + userId + "', 'expired-locked', CURRENT_TIMESTAMP - interval '1 day', NULL), "
                    + "('" + deletableId + "', '" + userId + "', 'expired-free', CURRENT_TIMESTAMP - interval '1 day', NULL), "
                    + "('" + liveId + "', '" + userId + "', 'still-live', CURRENT_TIMESTAMP + interval '1 day', NULL)");
        }

        try (Connection firstInstance = connection(); Connection secondInstance = connection()) {
            firstInstance.setAutoCommit(false);
            try (var lock = firstInstance.prepareStatement("SELECT id FROM refresh_tokens WHERE id = ? FOR UPDATE")) {
                lock.setObject(1, lockedId);
                lock.executeQuery().close();
            }
            try (var cleanup = secondInstance.prepareStatement("DELETE FROM refresh_tokens WHERE id IN ("
                    + "SELECT id FROM refresh_tokens WHERE expires_at <= CURRENT_TIMESTAMP "
                    + "OR (revoked_at IS NOT NULL AND revoked_at < CURRENT_TIMESTAMP - interval '30 days') "
                    + "ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED)")) {
                assertThat(cleanup.executeUpdate()).isEqualTo(1);
            }
            firstInstance.commit();
        }

        try (Connection verify = connection(); var statement = verify.createStatement();
             var rows = statement.executeQuery("SELECT id FROM refresh_tokens WHERE user_id = '" + userId + "' ORDER BY id")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getObject(1, UUID.class)).isEqualTo(lockedId);
            assertThat(rows.next()).isTrue();
            assertThat(rows.getObject(1, UUID.class)).isEqualTo(liveId);
            assertThat(rows.next()).isFalse();
        }
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
