package com.ieltspath.assessment.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.dao.DataAccessException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers(disabledWithoutDocker = true)
class PracticeAttemptScopeMigrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void addsPracticeIdentityAndDefaultsLegacyAttemptsWithoutBlockingTheirUpdates() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Flyway.configure().dataSource(dataSource).target(MigrationVersion.fromVersion("7")).load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        UUID legacyOfficial = insertAttempt(jdbc, "OFFICIAL_PRACTICE");

        Flyway.configure().dataSource(dataSource).load().migrate();

        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version = '8'", Integer.class));
        assertEquals("FULL", jdbc.queryForObject("SELECT scope FROM assessment_attempts WHERE id = ?", String.class,
                legacyOfficial));
        jdbc.update("UPDATE assessment_attempts SET status = 'SUBMITTED' WHERE id = ?", legacyOfficial);
        assertEquals("SUBMITTED", jdbc.queryForObject("SELECT status FROM assessment_attempts WHERE id = ?",
                String.class, legacyOfficial));
        assertThrows(DataAccessException.class,
                () -> jdbc.update("UPDATE assessment_attempts SET package_id = ? WHERE id = ?",
                        UUID.randomUUID(), legacyOfficial));
        assertThrows(DataAccessException.class, () -> insertAttempt(jdbc, "OFFICIAL_PRACTICE"));
        assertThrows(DataAccessException.class, () -> insertOfficialPractice(jdbc, "INVALID", "READING"));
        assertThrows(DataAccessException.class, () -> insertOfficialPractice(jdbc, "FULL", "INVALID"));
    }

    private static UUID insertAttempt(JdbcTemplate jdbc, String type) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO assessment_attempts (id, user_id, package_version_id, attempt_type, mode, channel, status, started_at)
                VALUES (?, ?, ?, ?, 'STANDARD', 'WEB', 'IN_PROGRESS', CURRENT_TIMESTAMP)
                """, id, UUID.randomUUID(), UUID.randomUUID(), type);
        return id;
    }

    private static void insertOfficialPractice(JdbcTemplate jdbc, String scope, String skill) {
        jdbc.update("""
                INSERT INTO assessment_attempts (id, user_id, package_version_id, package_id, attempt_type,
                    mode, channel, status, started_at, scope, skill)
                VALUES (?, ?, ?, ?, 'OFFICIAL_PRACTICE', 'STANDARD', 'WEB', 'IN_PROGRESS', CURRENT_TIMESTAMP, ?, ?)
                """, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), scope, skill);
    }
}
