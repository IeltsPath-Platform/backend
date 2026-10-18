package com.ieltspath.user.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Testcontainers(disabledWithoutDocker = true)
class StudyTimeMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void activeLegacyGoalsWithNonPositiveMinutesDoNotBlockStudyTimeMigration() {
        String schema = "study_time_migration_" + UUID.randomUUID().toString().replace("-", "");
        String jdbcUrl = POSTGRES.getJdbcUrl();
        String url = jdbcUrl + (jdbcUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        var dataSource = new DriverManagerDataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword());

        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .schemas(schema).defaultSchema(schema).target(MigrationVersion.fromVersion("5"))
                .load().migrate();

        var jdbc = new JdbcTemplate(dataSource);
        UUID zeroMinutesUser = insertLegacyGoal(jdbc, 0);
        UUID negativeMinutesUser = insertLegacyGoal(jdbc, -15);

        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .schemas(schema).defaultSchema(schema).load().migrate();

        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM study_goal_history", Integer.class));
        assertNull(jdbc.queryForObject("SELECT minutes FROM study_goal_history WHERE user_id = ?",
                Integer.class, zeroMinutesUser));
        assertNull(jdbc.queryForObject("SELECT minutes FROM study_goal_history WHERE user_id = ?",
                Integer.class, negativeMinutesUser));
    }

    private UUID insertLegacyGoal(JdbcTemplate jdbc, int minutes) {
        UUID userId = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id,email,full_name,password_hash,status) VALUES (?,?,'Legacy learner','test-only','ACTIVE')",
                userId, userId + "@example.test");
        jdbc.update("INSERT INTO learning_goals(user_id,target_band,available_minutes_per_day,status) VALUES (?,7.0,?,'ACTIVE')",
                userId, minutes);
        return userId;
    }
}
