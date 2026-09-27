package com.group01.content.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
class KnowledgePointLearningTypeMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    @Test
    void activeKnowledgePointRequiresOneOfTheFourLearningTypes() throws Exception {
        UUID topicId = UUID.randomUUID();
        insertTopic(topicId);

        assertConstraintViolation(() -> insertKnowledgePoint(topicId, null));
        for (String learningType : new String[]{"MEMORY", "CONCEPT", "PROCEDURE", "DESIGN"}) {
            insertKnowledgePoint(topicId, learningType);
        }

        assertEquals(4, countActiveKnowledgePoints(topicId));
    }

    private static void insertTopic(UUID topicId) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO topics (id, code, name) VALUES (?, ?, 'Topic')")) {
            statement.setObject(1, topicId);
            statement.setString(2, "TOPIC-" + topicId);
            statement.executeUpdate();
        }
    }

    private static void insertKnowledgePoint(UUID topicId, String learningType) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO knowledge_points (id, topic_id, code, name, kind, learning_type) " +
                             "VALUES (?, ?, ?, 'Example', 'GRAMMAR', ?)")) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, topicId);
            statement.setString(3, "KP-" + UUID.randomUUID());
            statement.setString(4, learningType);
            statement.executeUpdate();
        }
    }

    private static int countActiveKnowledgePoints(UUID topicId) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM knowledge_points WHERE topic_id = ? AND status = 'ACTIVE'")) {
            statement.setObject(1, topicId);
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private static void assertConstraintViolation(SqlRunnable action) throws SQLException {
        try {
            action.run();
            throw new AssertionError("Expected active Knowledge Point with null learning_type to be rejected");
        } catch (SQLException exception) {
            assertEquals("23514", exception.getSQLState());
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
