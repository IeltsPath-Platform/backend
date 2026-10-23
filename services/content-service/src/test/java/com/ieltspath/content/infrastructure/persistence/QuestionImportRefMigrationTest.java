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

    private static void insert(String importRef) throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO questions (id, question_type, purpose, import_ref) VALUES (?, 'MULTIPLE_CHOICE', 'LEARNING', ?)")) {
            statement.setObject(1, UUID.randomUUID());
            statement.setString(2, importRef);
            statement.executeUpdate();
        }
    }
}
