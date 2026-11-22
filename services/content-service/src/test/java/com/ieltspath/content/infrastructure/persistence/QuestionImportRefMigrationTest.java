package com.ieltspath.content.infrastructure.persistence;

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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
class QuestionImportRefMigrationTest {
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
    void sameReferenceCannotBeStoredTwice() throws Exception {
        insert("dup-ref");

        assertThrows(SQLException.class, () -> insert("dup-ref"));
    }

    @Test
    void questionsWithoutAReferenceDoNotConflict() throws Exception {
        insert(null);
        insert(null);
    }

    @Test
    void existingQuestionsAreNumberedInCreationOrderAndCounterContinues() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            try (var statement = connection.createStatement(); var result = statement.executeQuery("""
                    SELECT COUNT(*) FROM (
                        SELECT code, 'Q-' || LPAD(ROW_NUMBER() OVER (ORDER BY created_at, id)::text, 6, '0') expected
                        FROM questions WHERE code ~ '^Q-[0-9]{6}$'
                    ) numbered WHERE code <> expected
                    """)) {
                result.next();
                assertEquals(0, result.getInt(1));
            }
            try (var statement = connection.createStatement(); var result = statement.executeQuery("""
                    SELECT c.next_number,
                           (SELECT MAX(SUBSTRING(code FROM 3)::bigint) + 1 FROM questions WHERE code ~ '^Q-[0-9]{6}$')
                    FROM code_counters c WHERE c.code_kind = 'QUESTION' AND c.scope_key = ''
                    """)) {
                result.next();
                assertEquals(result.getLong(2), result.getLong(1));
            }
        }
    }

    private static void insert(String importRef) throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO questions (id, code, question_type, purpose, import_ref) VALUES (?, ?, 'MULTIPLE_CHOICE', 'LEARNING', ?)")) {
            statement.setObject(1, UUID.randomUUID());
            statement.setString(2, "Q-" + UUID.randomUUID().toString().replace("-", ""));
            statement.setString(3, importRef);
            statement.executeUpdate();
        }
    }
}
