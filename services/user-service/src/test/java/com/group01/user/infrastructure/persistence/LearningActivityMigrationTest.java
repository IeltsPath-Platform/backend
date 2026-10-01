package com.group01.user.infrastructure.persistence;

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

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
class LearningActivityMigrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
    }

    @Test
    void createsActivityAndStreakTablesWithoutUserForeignKeys() throws Exception {
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            for (String table : new String[]{"learning_activities", "streaks"}) {
                try (var rows = statement.executeQuery("SELECT to_regclass('public." + table + "')")) {
                    assertTrue(rows.next());
                    assertNotNull(rows.getString(1), table);
                }
                try (var rows = statement.executeQuery("SELECT count(*) FROM pg_constraint WHERE conrelid = '"
                        + table + "'::regclass AND contype = 'f'")) {
                    assertTrue(rows.next());
                    assertEquals(0, rows.getInt(1), table);
                }
            }
            try (var rows = statement.executeQuery(
                    "SELECT to_regclass('public.idx_learning_activities_user_occurred')")) {
                assertTrue(rows.next());
                assertNotNull(rows.getString(1));
            }
            UUID unknownUser = UUID.randomUUID();
            statement.executeUpdate("INSERT INTO learning_activities "
                    + "(id, user_id, activity_type, source_type, source_id, occurred_at, duration_seconds) "
                    + "VALUES ('" + UUID.randomUUID() + "', '" + unknownUser
                    + "', 'WATCH', 'VIDEO', '" + UUID.randomUUID() + "', now(), 12)");
            statement.executeUpdate("INSERT INTO streaks (user_id, timezone) VALUES ('"
                    + unknownUser + "', 'Asia/Ho_Chi_Minh')");
        }
    }

    @Test
    void negativeActivityDurationIsRejected() throws Exception {
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            assertThrows(SQLException.class, () -> statement.executeUpdate("INSERT INTO learning_activities "
                    + "(id, user_id, activity_type, source_type, source_id, occurred_at, duration_seconds) "
                    + "VALUES ('" + UUID.randomUUID() + "', '" + UUID.randomUUID()
                    + "', 'WATCH', 'VIDEO', '" + UUID.randomUUID() + "', now(), -1)"));
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
