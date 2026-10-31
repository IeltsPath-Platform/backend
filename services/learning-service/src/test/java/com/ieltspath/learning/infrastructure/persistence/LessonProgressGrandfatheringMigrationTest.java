package com.ieltspath.learning.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class LessonProgressGrandfatheringMigrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("learning_db").withPassword(UUID.randomUUID().toString());

    @Test
    void upgradeKeepsExistingProgressAndNewRowsDistinct() {
        Flyway before = flyway(MigrationVersion.fromVersion("38"));
        before.migrate();
        JdbcTemplate jdbc = jdbc();
        UUID existingUser = UUID.randomUUID();
        UUID existingLesson = UUID.randomUUID();
        insert(jdbc, existingUser, existingLesson);

        flyway(null).migrate();

        assertTrue(grandfathered(jdbc, existingUser, existingLesson));
        UUID newUser = UUID.randomUUID();
        UUID newLesson = UUID.randomUUID();
        insert(jdbc, newUser, newLesson);
        assertFalse(grandfathered(jdbc, newUser, newLesson));
    }

    private static Flyway flyway(MigrationVersion target) {
        var configuration = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration");
        if (target != null) configuration.target(target);
        return configuration.load();
    }

    private static JdbcTemplate jdbc() {
        return new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(),
                postgres.getPassword()));
    }

    private static void insert(JdbcTemplate jdbc, UUID userId, UUID lessonId) {
        jdbc.update("INSERT INTO lesson_progress (user_id, lesson_id, topic_id, lesson_sort_order) VALUES (?, ?, ?, 1)",
                userId, lessonId, UUID.randomUUID());
    }

    private static boolean grandfathered(JdbcTemplate jdbc, UUID userId, UUID lessonId) {
        return jdbc.queryForObject("SELECT grandfathered FROM lesson_progress WHERE user_id = ? AND lesson_id = ?",
                Boolean.class, userId, lessonId);
    }
}
