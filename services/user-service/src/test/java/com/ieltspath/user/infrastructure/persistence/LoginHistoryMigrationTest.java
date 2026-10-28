package com.ieltspath.user.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class LoginHistoryMigrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
    }

    @Test
    void createsTableAndNewestFirstIndex() throws Exception {
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            for (String name : new String[]{"login_history", "idx_login_history_user_created"}) {
                try (var rows = statement.executeQuery("SELECT to_regclass('public." + name + "')")) {
                    assertTrue(rows.next());
                    assertNotNull(rows.getString(1), name);
                }
            }
        }
    }

    @Test
    void failureReasonIsRequiredExactlyWhenTheAttemptFailed() throws Exception {
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            UUID userId = insertUser(statement);
            statement.executeUpdate(insert(userId, "true", "NULL"));
            statement.executeUpdate(insert(userId, "false", "'BAD_PASSWORD'"));
            assertThrows(SQLException.class, () -> statement.executeUpdate(insert(userId, "true", "'BAD_PASSWORD'")));
            assertThrows(SQLException.class, () -> statement.executeUpdate(insert(userId, "false", "NULL")));
            assertThrows(SQLException.class, () -> statement.executeUpdate(insert(userId, "false", "'SOMETHING_ELSE'")));
        }
    }

    @Test
    void historyIsRemovedWithItsUserAndNeedsAnExistingUser() throws Exception {
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            assertThrows(SQLException.class,
                    () -> statement.executeUpdate(insert(UUID.randomUUID(), "true", "NULL")));
            UUID userId = insertUser(statement);
            statement.executeUpdate(insert(userId, "true", "NULL"));
            statement.executeUpdate("DELETE FROM users WHERE id = '" + userId + "'");
            try (var rows = statement.executeQuery("SELECT count(*) FROM login_history WHERE user_id = '" + userId + "'")) {
                assertTrue(rows.next());
                assertEquals(0, rows.getInt(1));
            }
        }
    }

    private static UUID insertUser(java.sql.Statement statement) throws SQLException {
        UUID id = UUID.randomUUID();
        statement.executeUpdate("INSERT INTO users (id, email, full_name, password_hash, status) VALUES ('"
                + id + "', '" + id + "@example.com', 'Test User', 'hash', 'ACTIVE')");
        return id;
    }

    private static String insert(UUID userId, String success, String reason) {
        return "INSERT INTO login_history (user_id, success, failure_reason, ip_address, user_agent) VALUES ('"
                + userId + "', " + success + ", " + reason + ", '10.0.0.1', 'test')";
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
