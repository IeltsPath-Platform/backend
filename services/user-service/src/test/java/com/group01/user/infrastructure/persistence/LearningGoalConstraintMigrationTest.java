package com.group01.user.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.flywaydb.core.Flyway;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class LearningGoalConstraintMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static final UUID DUPLICATE_HISTORY_USER = UUID.randomUUID();
    private static final UUID OLDER_GOAL = UUID.randomUUID();
    private static final UUID NEWER_GOAL = UUID.randomUUID();

    @org.junit.jupiter.api.BeforeAll
    static void migrateAndRepairHistoricalDuplicates() throws Exception {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .target("3")
                .load()
                .migrate();

        insertUser(DUPLICATE_HISTORY_USER);
        insertGoal(OLDER_GOAL, DUPLICATE_HISTORY_USER, "ACTIVE", LocalDateTime.parse("2026-01-01T00:00:00"));
        insertGoal(NEWER_GOAL, DUPLICATE_HISTORY_USER, "ACTIVE", LocalDateTime.parse("2026-02-01T00:00:00"));

        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        assertEquals(1, countGoals(DUPLICATE_HISTORY_USER, "ACTIVE"));
        assertEquals(1, countGoals(DUPLICATE_HISTORY_USER, "PAUSED"));
        assertEquals("ACTIVE", goalStatus(NEWER_GOAL));
        assertEquals("PAUSED", goalStatus(OLDER_GOAL));
    }

    @Test
    void partialUniqueIndexAllowsOnlyOneActiveGoalPerUserAndKeepsHistory() throws Exception {
        UUID firstUser = UUID.randomUUID();
        UUID secondUser = UUID.randomUUID();
        insertUser(firstUser);
        insertUser(secondUser);

        insertGoal(firstUser, "ACTIVE");
        assertUniqueViolation(() -> insertGoal(firstUser, "ACTIVE"));
        insertGoal(secondUser, "ACTIVE");
        insertGoal(firstUser, "PAUSED");

        assertEquals(1, countGoals(firstUser, "ACTIVE"));
        assertEquals(1, countGoals(secondUser, "ACTIVE"));
        assertEquals(1, countGoals(firstUser, "PAUSED"));
    }

    @Test
    void concurrentActiveGoalInsertsForOneUserHaveOnlyOneWinner() throws Exception {
        UUID userId = UUID.randomUUID();
        insertUser(userId);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<String> outcomes = java.util.Collections.synchronizedList(new ArrayList<>());
        Runnable insert = () -> {
            ready.countDown();
            try {
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("Timed out waiting to start concurrent inserts");
                }
                insertGoal(userId, "ACTIVE");
                outcomes.add("inserted");
            } catch (SQLException exception) {
                if ("23505".equals(exception.getSQLState())) {
                    outcomes.add("conflict");
                } else {
                    throw new AssertionError(exception);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError(exception);
            }
        };

        Thread first = new Thread(insert);
        Thread second = new Thread(insert);
        first.start();
        second.start();
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        start.countDown();
        first.join(15000);
        second.join(15000);

        assertTrue(!first.isAlive() && !second.isAlive());
        assertEquals(1, outcomes.stream().filter("inserted"::equals).count());
        assertEquals(1, outcomes.stream().filter("conflict"::equals).count());
        assertEquals(1, countGoals(userId, "ACTIVE"));
    }

    private static void insertUser(UUID userId) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO users (id, email, full_name, password_hash, status) VALUES (?, ?, ?, 'test-hash', 'ACTIVE')")) {
            statement.setObject(1, userId);
            statement.setString(2, userId + "@example.test");
            statement.setString(3, "Test learner");
            statement.executeUpdate();
        }
    }

    private static void insertGoal(UUID userId, String status) throws SQLException {
        insertGoal(UUID.randomUUID(), userId, status, LocalDateTime.now());
    }

    private static void insertGoal(UUID goalId, UUID userId, String status, LocalDateTime createdAt) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO learning_goals (id, user_id, target_band, status, created_at) VALUES (?, ?, 7.0, ?, ?)")) {
            statement.setObject(1, goalId);
            statement.setObject(2, userId);
            statement.setString(3, status);
            statement.setTimestamp(4, Timestamp.valueOf(createdAt));
            statement.executeUpdate();
        }
    }

    private static String goalStatus(UUID goalId) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT status FROM learning_goals WHERE id = ?")) {
            statement.setObject(1, goalId);
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getString(1);
            }
        }
    }

    private static int countGoals(UUID userId, String status) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM learning_goals WHERE user_id = ? AND status = ?")) {
            statement.setObject(1, userId);
            statement.setString(2, status);
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private static void assertUniqueViolation(SqlRunnable action) throws SQLException {
        try {
            action.run();
            throw new AssertionError("Expected PostgreSQL partial unique index to reject duplicate active goal");
        } catch (SQLException exception) {
            assertEquals("23505", exception.getSQLState());
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    @FunctionalInterface
    private interface SqlRunnable {
        void run() throws SQLException;
    }
}
