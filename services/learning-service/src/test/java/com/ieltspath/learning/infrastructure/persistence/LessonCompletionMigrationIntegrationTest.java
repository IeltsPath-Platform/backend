package com.ieltspath.learning.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A lesson confirmed before its Practice passed is no longer completed, and neither is one completed by a Practice pass
 * stored under the earlier one-set rule; lessons without required Practice keep their completion.
 */
@Testcontainers(disabledWithoutDocker = true)
class LessonCompletionMigrationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static final UUID USER = UUID.randomUUID();
    private static final UUID TOPIC = UUID.randomUUID();
    private static final UUID UNPASSED = UUID.randomUUID();
    private static final UUID NO_PRACTICE = UUID.randomUUID();
    private static final UUID PASSED = UUID.randomUUID();
    private static final UUID NOT_COMPLETED = UUID.randomUUID();

    private static JdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").target("10").load().migrate();
        jdbc = new JdbcTemplate(source);
        int order = 1;
        for (UUID lesson : new UUID[]{UNPASSED, NO_PRACTICE, PASSED}) {
            jdbc.update("INSERT INTO lesson_progress (user_id, lesson_id, topic_id, lesson_sort_order, "
                    + "knowledge_point_ids, completed_at) VALUES (?, ?, ?, ?, '{}', clock_timestamp())",
                    USER, lesson, TOPIC, order++);
        }
        jdbc.update("INSERT INTO lesson_progress (user_id, lesson_id, topic_id, lesson_sort_order, "
                + "knowledge_point_ids) VALUES (?, ?, ?, 4, '{}')", USER, NOT_COMPLETED, TOPIC);
        jdbc.update("INSERT INTO lesson_practice_passes (user_id, lesson_id, reason) VALUES (?, ?, 'NO_PRACTICE')",
                USER, NO_PRACTICE);
        jdbc.update("INSERT INTO lesson_practice_passes (user_id, lesson_id, reason) VALUES (?, ?, 'FIRST_SUBMISSION')",
                USER, PASSED);
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
    }

    private static int passRows(UUID lesson) {
        return jdbc.queryForObject("SELECT count(*) FROM lesson_practice_passes WHERE user_id = ? AND lesson_id = ?",
                Integer.class, USER, lesson);
    }

    private static Object completedAt(UUID lesson) {
        return jdbc.queryForObject("SELECT completed_at FROM lesson_progress WHERE user_id = ? AND lesson_id = ?",
                Object.class, USER, lesson);
    }

    @Test
    void aLessonConfirmedWithoutAPassedPracticeIsNoLongerCompleted() {
        assertNull(completedAt(UNPASSED));
    }

    @Test
    void aLessonWithoutRequiredPracticeKeepsItsCompletionAndPass() {
        assertNotNull(completedAt(NO_PRACTICE));
        assertEquals(1, passRows(NO_PRACTICE));
    }

    @Test
    void aLessonCompletedByAnEarlierRulePracticePassIsReopenedAndItsPassDropped() {
        assertNull(completedAt(PASSED));
        assertEquals(0, passRows(PASSED));
    }

    @Test
    void aLessonThatWasNeverCompletedStaysThatWay() {
        assertNull(completedAt(NOT_COMPLETED));
    }

    @Test
    void everyLessonRowIsKept() {
        assertEquals(4, jdbc.queryForObject("SELECT count(*) FROM lesson_progress WHERE user_id = ?",
                Integer.class, USER));
    }
}
