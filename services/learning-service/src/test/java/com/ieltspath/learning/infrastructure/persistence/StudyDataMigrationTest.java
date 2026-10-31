package com.ieltspath.learning.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Goals, learning activity and study time live in Learning's database, keyed by an account id owned by the User Service. */
@Testcontainers(disabledWithoutDocker = true)
class StudyDataMigrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    static JdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
    }

    private static void goal(UUID user, String status) {
        jdbc.update("INSERT INTO learning_goals (user_id, target_band, status) VALUES (?, 7.0, ?)", user, status);
    }

    private static void session(UUID user, String status) {
        jdbc.update("""
                INSERT INTO study_sessions (id, user_id, client_session_id, source_type, source_id, timezone, epoch,
                    started_at, lease_expires_at, status)
                VALUES (?, ?, ?, 'LESSON', ?, 'UTC', 1, now(), now(), ?)
                """, UUID.randomUUID(), user, UUID.randomUUID(), UUID.randomUUID(), status);
    }

    @Test
    void aLearnerHasOneActiveGoalAndKeepsTheirOthers() {
        UUID user = UUID.randomUUID();
        goal(user, "ACTIVE");
        goal(user, "PAUSED");
        goal(user, "ACHIEVED");

        assertThrows(DataIntegrityViolationException.class, () -> goal(user, "ACTIVE"));
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM learning_goals WHERE user_id = ?", Integer.class, user));
    }

    @Test
    void aGoalStatusOutsideTheKnownSetIsRefused() {
        assertThrows(DataIntegrityViolationException.class, () -> goal(UUID.randomUUID(), "DELETED"));
    }

    @Test
    void aLearnerHasOneOpenStudySessionAtATime() {
        UUID user = UUID.randomUUID();
        session(user, "OPEN");
        session(user, "CLOSED");

        assertThrows(DataIntegrityViolationException.class, () -> session(user, "OPEN"));
    }

    @Test
    void anActivityMustNotHaveANegativeDuration() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO learning_activities (id, user_id, activity_type, source_type, source_id, occurred_at, duration_seconds)
                VALUES (?, ?, 'LESSON', 'LESSON', ?, now(), -1)
                """, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void trackingStartsFromOneRecordedMoment() {
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM study_tracking_policy", Integer.class));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO study_tracking_policy VALUES (2, now())"));
    }

    @Test
    void theDatabaseHoldsNoAccountTable() {
        assertEquals(0, jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'users'", Integer.class));
    }
}
