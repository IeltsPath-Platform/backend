package com.ieltspath.learning.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
class LearningOverviewPersistenceTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    static JdbcTemplate jdbc;
    static JdbcLearningOverviewReader reader;

    @BeforeAll
    static void setup() {
        var ds = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(ds);
        reader = new JdbcLearningOverviewReader(jdbc);
    }

    private void daily(UUID user, String date, String timezone, String scope, long milliseconds) {
        jdbc.update("INSERT INTO study_daily_totals(user_id,local_date,timezone,scope_key,active_ms) VALUES(?,?,?,?,?)",
                user, Date.valueOf(date), timezone, scope, milliseconds);
    }

    private void lesson(UUID user, UUID lessonId, UUID topic, boolean completed) {
        jdbc.update("""
                INSERT INTO lesson_progress(user_id,lesson_id,topic_id,lesson_sort_order,knowledge_point_ids,completed_at)
                VALUES(?,?,?,0,ARRAY[?]::uuid[],?)
                """, user, lessonId, topic, UUID.randomUUID(),
                completed ? Timestamp.from(Instant.parse("2026-01-01T00:00:00Z")) : null);
    }

    @Test
    void sumsOnlyAllScopesForLifetime() {
        UUID user = UUID.randomUUID();
        daily(user, "2026-01-01", "UTC", "ALL", 40_000);
        daily(user, "2026-01-01", "Asia/Ho_Chi_Minh", "ALL", 30_000);
        daily(user, "2026-01-02", "UTC", "ALL", 90_000);
        daily(user, "2026-01-03", "UTC", "ALL", 0);
        daily(user, "2026-01-04", "UTC", "ALL", 59_999);
        daily(user, "2026-01-01", "UTC", UUID.randomUUID().toString(), 1500);
        daily(UUID.randomUUID(), "2026-01-01", "UTC", "ALL", 999999);

        var totals = reader.totals(user);

        assertEquals(219_999, totals.studyMilliseconds());
        assertEquals(jdbc.queryForObject("SELECT started_at FROM study_tracking_policy WHERE id=1", Timestamp.class)
                .toInstant(), totals.dataFrom());
    }

    @Test
    void completedLessonsAreUniqueAndIndependentOfStartedCoursePagination() {
        UUID user = UUID.randomUUID();
        for (int i = 0; i < 7; i++) {
            UUID topic = UUID.randomUUID();
            jdbc.update("INSERT INTO topic_progress(user_id,topic_id,course_id) VALUES(?,?,?)",
                    user, topic, UUID.randomUUID());
            UUID lessonId = UUID.randomUUID();
            lesson(user, lessonId, topic, true);
            // Multiple knowledge points on a lesson must not become multiple completions.
            jdbc.update("UPDATE lesson_progress SET knowledge_point_ids=ARRAY[?,?]::uuid[] WHERE user_id=? AND lesson_id=?",
                    UUID.randomUUID(), UUID.randomUUID(), user, lessonId);
        }
        lesson(user, UUID.randomUUID(), UUID.randomUUID(), false);
        lesson(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), true);

        assertEquals(7, reader.totals(user).completedLessons());
    }

    @Test
    void emptyLearnerHasZeroCountsAndTrackingPolicyAvailability() {
        var totals = reader.totals(UUID.randomUUID());
        assertEquals(0, totals.studyMilliseconds());
        assertEquals(0, totals.completedLessons());
        assertNotNull(totals.dataFrom());
    }
}
