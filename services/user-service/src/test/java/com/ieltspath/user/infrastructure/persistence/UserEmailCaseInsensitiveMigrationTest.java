package com.ieltspath.user.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class UserEmailCaseInsensitiveMigrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void lowercasesExistingEmailsAndCreatesTheCaseInsensitiveIndex() throws Exception {
        migrateToVersion19();
        UUID userId = insertUser("Mixed.Case@example.test");

        flyway().migrate();

        try (Connection connection = connection(); var statement = connection.createStatement()) {
            try (var rows = statement.executeQuery("SELECT email FROM users WHERE id = '" + userId + "'")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("mixed.case@example.test");
            }
            try (var rows = statement.executeQuery("SELECT to_regclass('public.uq_users_email_lower')")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("uq_users_email_lower");
            }
        }
        assertThatThrownBy(() -> insertUser("MIXED.CASE@example.test"))
                .isInstanceOf(java.sql.SQLException.class)
                .hasMessageContaining("uq_users_email_lower");
    }

    @Test
    void refusesCaseOnlyDuplicatesWithoutChangingEitherAccount() throws Exception {
        migrateToVersion19();
        UUID first = insertUser("same@example.test");
        UUID second = insertUser("Same@example.test");

        assertThatThrownBy(() -> flyway().migrate())
                .hasStackTraceContaining("Cannot normalize users.email: case-insensitive duplicates exist; resolve accounts before applying this migration");

        try (Connection connection = connection(); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT email FROM users WHERE id IN ('" + first + "', '" + second + "') ORDER BY email")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getString(1)).isEqualTo("Same@example.test");
            assertThat(rows.next()).isTrue();
            assertThat(rows.getString(1)).isEqualTo("same@example.test");
        }
    }

    private static Flyway flyway() {
        return Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").cleanDisabled(false).load();
    }

    private static void migrateToVersion19() {
        flyway().clean();
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").target("19").load().migrate();
    }

    private static UUID insertUser(String email) throws Exception {
        UUID id = UUID.randomUUID();
        try (Connection connection = connection(); var statement = connection.prepareStatement(
                "INSERT INTO users (id, email, full_name, password_hash, status) VALUES (?, ?, 'Test User', 'hash', 'ACTIVE')")) {
            statement.setObject(1, id);
            statement.setString(2, email);
            statement.executeUpdate();
        }
        return id;
    }

    private static Connection connection() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
